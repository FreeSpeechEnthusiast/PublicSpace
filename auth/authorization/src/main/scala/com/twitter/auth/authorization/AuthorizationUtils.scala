package com.twitter.auth.authorization

import com.twitter.auth.authenforcement.thriftscala._
import com.twitter.servo.util.MemoizingStatsReceiver
import com.twitter.finagle.stats.DefaultStatsReceiver
import com.twitter.util.Future

object AuthorizationUtils {
  private[this] lazy val statsReceiver = new MemoizingStatsReceiver(DefaultStatsReceiver)
  private[this] lazy val authorizerFP = statsReceiver.scope("authorizer_fp")
  private[this] lazy val authorizerDP = statsReceiver.scope("authorizer_dp")

  private[this] lazy val fpLookupCounter = authorizerFP.counter("lookup_count")
  private[this] lazy val dpLookupCounter = authorizerDP.counter("lookup_count")

  private[this] lazy val fineGrainFpAcceptancesScope = authorizerFP.scope("allowed")
  private[this] lazy val fineGrainDpRejectionsScope = authorizerDP.scope("rejected")
  private[this] lazy val fineGrainDpAcceptancesScope = authorizerDP.scope("allowed")

  private[this] lazy val allRejectedDPsCounter = authorizerDP.counter("all_rejected")
  private[this] lazy val allAcceptedDPsCounter = authorizerDP.counter("all_accepted")
  private[this] lazy val partiallyAcceptedDPsCounter = authorizerDP.counter("partially_accepted")

  private[this] lazy val grantedMissingDPsCounter = authorizerDP.scope("granted_missing")

  def validateDPsPolicy(
    policy: Policy,
    dataPermissionLookup: DataPermissionLookup
  ): Boolean = {
    val annotatedDps: Set[DataPermission] = policy.dataPermissionsAnotated.getOrElse(Set()).toSet
    val dpSet = annotatedDps.flatMap(_.id)
    dataPermissionLookup.areAllValidDPIds(dpSet)
  }

  def validatePassport(
    passport: Passport,
    dataPermissionLookup: DataPermissionLookup
  ): Boolean = {
    passport match {
      case Passport(
            id @ _,
            principals @ _,
            /* scala bug#1503 dpDecisions @*/ None,
            /* scala bug#1503 fpDecisions @*/ None,
            passportType @ _,
            spDecisions @ _,
            metadata @ _) =>
        true
      case Passport(
            id @ _,
            principals @ _,
            dataPermissionDecisions @ Some(dpDecisions),
            featurePermissionDecisions @ _,
            passportType @ _,
            subscriptionPermissionDecisions @ _,
            metadata @ _) =>
        val allowedDps = dpDecisions.allowedDataPermissionIds.map(_.toSet).getOrElse(Set.empty)
        val rejectedDps = dpDecisions.rejectedDataPermissionIds.map(_.toSet).getOrElse(Set.empty)

        dataPermissionLookup.areAllValidDPIds(allowedDps) && dataPermissionLookup.areAllValidDPIds(
          rejectedDps)
      case _ =>
        true
    }
  }

  /**
   * Sanitize principals before inserting them into the authZ final passport
   */
  def sanitizePrincipals(
    original: scala.collection.Set[Principal]
  ): scala.collection.Set[Principal] = {
    // here we will first sanitize the Session Principal before pushing it back into the new passport
    val sanitizedPrincipal = original.foldLeft(Set.empty[Principal]) { (principalSet, p) =>
      p match {
        case sp: Principal.SessionPrincipal =>
          // if the token hash in session principal is empty, remove the entire session principal
          if (sp.sessionPrincipal.sessionHash.isEmpty)
            principalSet.union(Set.empty[Principal])
          else { // otherwise, remove the scopes field from session principal
            principalSet.union(Set[Principal](sp.copy(sp.sessionPrincipal.copy(scopes = None))))
          }
        case a => principalSet.union(Set[Principal](a))
      }
    }

    sanitizedPrincipal
  }

  def lookupDataPermissionDecisions(
    authorizationScopeLookup: AuthorizationScopeLookup,
    dataPermissionLookup: DataPermissionLookup,
    sessionPrincipal: SessionPrincipal,
    requestedDPIds: Set[Long],
    shouldGrantMissingDPsFuture: Future[Boolean],
    clientApplicationPrincipalOpt: Option[ClientApplicationPrincipal],
    includeRejections: Boolean = false
  ): DataPermissionDecisions = {
    dpLookupCounter.incr()
    val tokenPrivileges: Set[String] = sessionPrincipal.scopes.getOrElse(Set[String]().empty).toSet
    // convert set of strings to byte buffer based on name
    val convertedScopes = authorizationScopeLookup.authorizationScopes(tokenPrivileges)

    scopeToDataPermissionDecision(
      dataPermissionLookup = dataPermissionLookup,
      scopes = convertedScopes,
      requestedDPIds = requestedDPIds,
      shouldGrantMissingDPsFuture = shouldGrantMissingDPsFuture,
      clientApplicationPrincipalOpt = clientApplicationPrincipalOpt,
      includeRejections = includeRejections
    )
  }

  def lookupFeaturePermissionDecisions(
    featurePermissionLookup: FeaturePermissionLookup,
    clientApplicationPrincipal: ClientApplicationPrincipal
  ): FeaturePermissionDecisions = {
    fpLookupCounter.incr()
    val appId = clientApplicationPrincipal.clientApplicationId
    // at this point, we are certain that application is active
    // (passport checks would have failed otherwise)
    val allFPsByAppId = featurePermissionLookup.featurePermissions(appId)
    recordFineGrainFPStats(allFPsByAppId)
    FeaturePermissionDecisions(toOption(allFPsByAppId), None)
  }

  def scopeToDataPermissionDecision(
    dataPermissionLookup: DataPermissionLookup,
    scopes: Set[String],
    requestedDPIds: Set[Long],
    shouldGrantMissingDPsFuture: Future[Boolean],
    clientApplicationPrincipalOpt: Option[ClientApplicationPrincipal],
    includeRejections: Boolean,
  ): DataPermissionDecisions = {
    val tokenScopeDPsIds: Set[Long] = dataPermissionLookup.dataPermissionIds(scopes)
    // only grant minimum permissions
    var allowedIds = tokenScopeDPsIds.intersect(requestedDPIds)
    // request:[D], permissions:[A,B,C] => rejected:[D]
    var rejectedIds = requestedDPIds.diff(tokenScopeDPsIds)

    // if there are any rejected DPs, and we should grant them, do so
    // if there's a need, here we can also subtract any premium / special DPs that
    // should not be granted so easily
    if (rejectedIds.nonEmpty) {
      shouldGrantMissingDPsFuture.map(should =>
        if (should) {
          allowedIds = requestedDPIds
          rejectedIds = Set.empty
          clientApplicationPrincipalOpt.map(app =>
            grantedMissingDPsCounter.counter(app.clientApplicationId.toString).incr())
        })
    }

    recordGeneralDPStats(rejectedIds.size, requestedDPIds.size)
    recordFineGrainDPStats(allowedIds, rejectedIds)

    val rejectedDpsSet = if (includeRejections) rejectedIds else Set.empty[Long]
    DataPermissionDecisions(
      allowedDataPermissionIds = toOption(allowedIds),
      rejectedDataPermissionIds = toOption(rejectedDpsSet)
    )
  }

  def sessionPrincipal(principals: Set[Principal]): Option[SessionPrincipal] = {
    principals.collectFirst { case Principal.SessionPrincipal(s) => s }
  }

  def clientAppPrincipal(principals: Set[Principal]): Option[ClientApplicationPrincipal] = {
    principals.collectFirst { case Principal.ClientApplicationPrincipal(c) => c }
  }

  def toOption[T](set: Set[T]): Option[Set[T]] = {
    if (set.isEmpty) None else Some(set)
  }

  private def recordGeneralDPStats(
    rejectedPermissionsSize: Int,
    requestedPermissionsSize: Int
  ): Unit = {
    if (rejectedPermissionsSize == requestedPermissionsSize) {
      allRejectedDPsCounter.incr()
    } else if (rejectedPermissionsSize == 0) {
      allAcceptedDPsCounter.incr()
    } else {
      partiallyAcceptedDPsCounter.incr()
    }
  }

  // record per FP id rejection stats if the decider says so
  private def recordFineGrainFPStats(
    fps: Set[String]
  ): Unit = {
    fps.foreach(fineGrainFpAcceptancesScope.counter(_).incr())
  }

  // record per DP id rejection stats if the decider says so
  private def recordFineGrainDPStats(
    allowed: Set[Long],
    rejected: Set[Long]
  ): Unit = {
    allowed.map(_.toString).foreach(fineGrainDpAcceptancesScope.counter(_).incr())
    rejected.map(_.toString).foreach(fineGrainDpRejectionsScope.counter(_).incr())
  }
}
