package com.twitter.auth.authorization

import com.twitter.auth.authenforcement.thriftscala._
import com.twitter.auth.authentication.utils.AuthenticationUtils._
import com.twitter.auth.authorization.AuthorizationUtils._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.servo.util.MemoizingStatsReceiver
import com.twitter.util.Future
import com.twitter.util.Return
import com.twitter.util.Throw
import scala.collection.Set
import com.twitter.auth.passporttype.thriftscala.PassportType
object Authorizer {
  val GRANT_MISSING_ROUTE_DPS_TO_APP_IDS = "grant_missing_route_dps_to_app_ids"
  val EMAIL_LITE_LOGIN_SCOPE = "email.lite.login"
}
class Authorizer(
  authorizationScopeLookup: AuthorizationScopeLookup,
  dataPermissionLookup: DataPermissionLookup,
  featurePermissionLookup: FeaturePermissionLookup,
  statsReceiver: StatsReceiver) {

  import Authorizer._

  private[this] val authorizerScope = new MemoizingStatsReceiver(statsReceiver.scope("authorizer"))
  private[this] val authorizerSuccessCounter = authorizerScope.counter("success")
  private[this] val authorizerFailureCounter = authorizerScope.counter("failure")
  private[this] val dataPermissionDecisionOnlyCounter = authorizerScope
    .counter("data_permission_decision_only")
  private[this] val featurePermissionDecisionOnlyCounter = authorizerScope
    .counter("feature_permission_decision_only")

  /**
   * Create a Customer Auth Passport with minimum required policies and access privileges populated.
   * Access privileges of a Customer Auth Passport is determined by the principals, and only minimum
   * required access privileges are granted if requested by the policy.
   */
  def authorize(
    passport: Passport,
    policy: Policy,
    includeRejections: Boolean = false
  ): Future[Passport] = {
    // validate policy
    if (!validateDPsPolicy(policy, dataPermissionLookup)) {
      Future.exception(InvalidPolicyException(policy.toString))
    }
    // validate passport (DPs only)
    else if (!validatePassport(passport, dataPermissionLookup)) {
      Future.exception(InvalidPassportException(passport.toString))
    }
    // authorize passport
    else {
      val principalsSet = passport.principals.toSet
      val sessionPrincipalOpt = sessionPrincipal(principalsSet)
      val clientApplicationPrincipalOpt = clientAppPrincipal(principalsSet)

      // TODO: Considering the size of Passport, we may stop carrying Feature Permissions.
      //  For testing purposes, we still carry all FPs for now.
      // here we execute the get synchronously since we will need it regardless
      val fpdFuture: Future[Option[FeaturePermissionDecisions]] = Future(
        clientApplicationPrincipalOpt.map(appPrincipal =>
          lookupFeaturePermissionDecisions(
            featurePermissionLookup = featurePermissionLookup,
            clientApplicationPrincipal = appPrincipal
          )))

      // keep the set for later in case we need it
      val allowedFPSetFuture: Future[scala.collection.Set[String]] =
        fpdFuture
          .map(fpdOption =>
            if (fpdOption.isDefined) fpdOption.get.allowedFeaturePermissions
            else None)
          .map(allowed => allowed.getOrElse(Set.empty[String]))

      val dpFuture = Future(sessionPrincipalOpt.map { sessionPrincipal =>
        val isEmailLiteLoginRequest = sessionPrincipal.scopes
          .getOrElse(Set())
          .contains(EMAIL_LITE_LOGIN_SCOPE)

        val isAccountDelegationRequest =
          passport.passportType == Some(PassportType.AccountDelegation)

        // we want to grant missing DPs for TOO apps, but as part of email lite login project
        // we could have request from twitter rweb with email lite login scope and in that
        // case we do not want to grant all the missing dps
        val shouldGrantMissingDPsFuture: Future[Boolean] = {
          allowedFPSetFuture.map(allowedSet =>
            allowedSet.contains(GRANT_MISSING_ROUTE_DPS_TO_APP_IDS) && !isEmailLiteLoginRequest
              && !isAccountDelegationRequest)
        }

        lookupDataPermissionDecisions(
          authorizationScopeLookup = authorizationScopeLookup,
          dataPermissionLookup = dataPermissionLookup,
          sessionPrincipal = sessionPrincipal,
          requestedDPIds =
            policy.dataPermissionsAnotated.getOrElse(Set()).flatMap(_.id).toSet, // :rainbowpuke
          shouldGrantMissingDPsFuture = shouldGrantMissingDPsFuture,
          clientApplicationPrincipalOpt = clientApplicationPrincipalOpt,
          includeRejections = includeRejections
        )
      })

      val sanitizedPrincipals = sanitizePrincipals(passport.principals)

      val passportFuture = Future.collectToTry(Seq(dpFuture, fpdFuture)).flatMap {
        case Seq(
              Return(Some(dp: DataPermissionDecisions)),
              Return(Some(fp: FeaturePermissionDecisions))) =>
          authorizerSuccessCounter.incr()
          Future.value(
            passport.copy(
              principals = sanitizedPrincipals,
              dataPermissionDecisions = Some(dp),
              featurePermissionDecisions = Some(fp)
            )
          )
        case Seq(Return(Some(dp: DataPermissionDecisions)), Return(None)) =>
          authorizerSuccessCounter.incr()
          dataPermissionDecisionOnlyCounter.incr()
          Future.value(
            passport.copy(dataPermissionDecisions = Some(dp), principals = sanitizedPrincipals))
        case Seq(Return(None), Return(Some(fp: FeaturePermissionDecisions))) =>
          authorizerSuccessCounter.incr()
          featurePermissionDecisionOnlyCounter.incr()
          Future.value(
            passport.copy(featurePermissionDecisions = Some(fp), principals = sanitizedPrincipals))
        case Seq(_, Throw(t)) =>
          authorizerFailureCounter.incr()
          Future.exception(t)
        case Seq(Throw(t), _) =>
          authorizerFailureCounter.incr()
          Future.exception(t)
        // never happens
        case _ =>
          authorizerFailureCounter.incr()
          Future.value(passport.copy(principals = sanitizedPrincipals))
      }

      tracePassport(passportFuture)
      passportFuture
    }
  }
}
