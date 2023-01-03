package com.twitter.auth.policy

import com.twitter.auth.authenforcement.thriftscala.{Passport, Policy}
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.util.Future

sealed case class FeaturePermissionEnforcementResult(
  fpAllow: Boolean)

sealed case class SubscriptionPermissionEnforcementResult(
  spAllow: Boolean)

sealed case class DataPermissionEnforcementResult(
  dpAllow: Boolean,
  unsatisfiedTestingDp: Boolean)

sealed case class EnforcementResult(
  dpAllow: Boolean,
  fpAllow: Boolean,
  unsatisfiedTestingDp: Boolean, // Missing one or more test DPs that we failed open on
  spAllow: Boolean)

trait Enforcer {

  /**
   * Determine if a passport contains minimum required permissions for access.
   *
   * @param passport contain request authenticated principals and permissions
   * @param policy   minimum required permissions for access
   * @param authTypeOpt   authType of the request
   */
  def enforce(
    passport: Passport,
    policy: Policy,
    authTypeOpt: Option[AuthenticationType] = None
  ): Future[EnforcementResult]
}
