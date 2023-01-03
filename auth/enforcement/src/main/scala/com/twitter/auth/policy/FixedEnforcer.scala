package com.twitter.auth.policy

import com.twitter.auth.authorization.FeaturePermissionLookup
import com.twitter.auth.authenforcement.thriftscala.{
  DataPermission,
  DataPermissionDecisions,
  DataPermissionState,
  Passport,
  Policy,
  SubscriptionPermissionDecisions
}
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.auth.authorization.AuthorizationUtils.clientAppPrincipal
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Future

class FixedEnforcer(
  featurePermissionLookup: FeaturePermissionLookup,
  statsReceiver: StatsReceiver)
    extends Enforcer {

  private[this] val scopedReceiver = statsReceiver.scope("fixed_enforcer")
  private[this] val fpAllowedCounter = scopedReceiver.counter("fp_allowed")
  private[this] val fpRejectCounter = scopedReceiver.counter("fp_rejected")
  private[this] val dpAllowedCounter = scopedReceiver.counter("dp_allowed")
  private[this] val dpRejectedCounter = scopedReceiver.counter("dp_rejected")
  private[this] val unsatisfiedTestingDpCounter = scopedReceiver.counter("unsatisfied_testing_dp")
  private[this] val spAllowedCounter = scopedReceiver.counter("sp_allowed")
  private[this] val spRejectCounter = scopedReceiver.counter("sp_rejected")

  override def enforce(
    passport: Passport,
    policy: Policy,
    authTypeOpt: Option[AuthenticationType] = None
  ): Future[EnforcementResult] = {

    // grab the annotated data permissions or use empty set
    val annotatedDps: Set[DataPermission] = policy.dataPermissionsAnotated.getOrElse(Set()).toSet

    // If the request is for restricted session, we do not want to respect optional/testing state on
    // the data permission, so we want to enforce all the annotated data permissions
    val (testingDps, enforcedDps) =
      authTypeOpt match {
        case Some(authType)
            if authType == AuthenticationType.RestrictedSession || authType == AuthenticationType.RestrictedOauth2Session =>
          (Set(), annotatedDps)
        case _ =>
          annotatedDps
            .filter(_.state.contains(DataPermissionState.Enforced)).partition(
              _.testing.contains(true))
      }

    val testingDpIds = testingDps.flatMap(_.id)
    val enforcedDpIds = enforcedDps.flatMap(_.id)

    // We really only care about whether there is a set of DP ids, if not, use empty Set
    val allowedDpIds: Set[Long] = passport.dataPermissionDecisions match {
      case Some(DataPermissionDecisions(_, _, Some(allowedDpIds), _)) =>
        allowedDpIds.toSet
      case _ =>
        Set.empty[Long]
    }

    // Grab FPs or use empty set if not there
    val policyFps: Set[String] = policy.featurePermissions.getOrElse(Set()).toSet

    // Lookup Feature Permissions against Feature Switch directly with client application id
    val allowedFps: Set[String] = clientAppPrincipal(passport.principals.toSet)
      .map(principal =>
        featurePermissionLookup.featurePermissions(principal.clientApplicationId)).getOrElse(Set())

    val policySps: Set[String] = policy.subscriptionPermissions.getOrElse(Set()).toSet
    val allowedSps: Set[String] = passport.subscriptionPermissionDecisions match {
      case Some(SubscriptionPermissionDecisions(Some(allowed), _)) =>
        allowed.toSet
      case _ =>
        Set.empty[String]
    }

    // Do enforcements so we do not gate stats for one on the other succeeding
    val fpResult = doEnforcementFps(policyFps, allowedFps)
    val dpResult = doEnforcementDps(enforcedDpIds, testingDpIds, allowedDpIds)
    val spResult = doEnforcementSps(policySps, allowedSps)

    enforcementResult(fpResult, dpResult, spResult)
  }

  /**
   * Given a Policy and Passport Feature Permission sets, it says YES only if the FPs carried by
   * Passport are greater than or equal to the ones Policy has (e.g. Passport.FPs >= Policy.FPs)
   */
  private[this] def doEnforcementFps(
    policyFeaturePermissions: Set[String],
    passportFeaturePermissions: Set[String]
  ): FeaturePermissionEnforcementResult = {
    if (policyFeaturePermissions.subsetOf(passportFeaturePermissions)) {
      fpAllowedCounter.incr()
      FeaturePermissionEnforcementResult(true)
    } else {
      fpRejectCounter.incr()
      FeaturePermissionEnforcementResult(false)
    }
  }

  /**
   * Given a Policy and Passport Subscription Permission sets, it says YES only if the FPs carried
   * by Passport are greater than or equal to the ones Policy has (e.g. Passport.SPs >= Policy.SPs)
   */
  private[this] def doEnforcementSps(
    policySubscriptionPermissions: Set[String],
    passportSubscriptionPermissions: Set[String]
  ): SubscriptionPermissionEnforcementResult = {
    if (policySubscriptionPermissions.subsetOf(passportSubscriptionPermissions)) {
      spAllowedCounter.incr()
      SubscriptionPermissionEnforcementResult(true)
    } else {
      spRejectCounter.incr()
      SubscriptionPermissionEnforcementResult(false)
    }
  }

  /**
   * Given a Policy and Passport Data Permission sets, it says YES only if the DPs carried by
   * Passport are greater than or equal to the ones Policy has (e.g. Passport.DPs >= Policy.DPs)
   */
  private[this] def doEnforcementDps(
    enforcedDps: Set[Long],
    testingDps: Set[Long],
    passportDataPermissions: Set[Long]
  ): DataPermissionEnforcementResult = {
    if (enforcedDps.subsetOf(passportDataPermissions)) {
      dpAllowedCounter.incr()
      // we are going to let this request pass, but we need to raise an alert
      // if there are testing Dps that would have failed request had they been enforced
      if (testingDps.diff(passportDataPermissions).nonEmpty) {
        // there are dps in the testing set that is not present in the passport
        unsatisfiedTestingDpCounter.incr()
        DataPermissionEnforcementResult(dpAllow = true, unsatisfiedTestingDp = true)
      } else {
        DataPermissionEnforcementResult(dpAllow = true, unsatisfiedTestingDp = false)
      }
    } else {
      dpRejectedCounter.incr()
      DataPermissionEnforcementResult(dpAllow = false, unsatisfiedTestingDp = false)
    }
  }

  private[this] def enforcementResult(
    featurePermissionEnforcementResult: FeaturePermissionEnforcementResult,
    dataPermissionEnforcementResult: DataPermissionEnforcementResult,
    subscriptionPermissionEnforcementResult: SubscriptionPermissionEnforcementResult
  ): Future[EnforcementResult] = {
    Future(
      EnforcementResult(
        dpAllow = dataPermissionEnforcementResult.dpAllow,
        fpAllow = featurePermissionEnforcementResult.fpAllow,
        unsatisfiedTestingDp = dataPermissionEnforcementResult.unsatisfiedTestingDp,
        spAllow = subscriptionPermissionEnforcementResult.spAllow
      )
    )
  }
}
