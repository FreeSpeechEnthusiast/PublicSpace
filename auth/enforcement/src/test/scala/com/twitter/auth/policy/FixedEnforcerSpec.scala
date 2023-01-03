package com.twitter.auth.policy

import com.twitter.auth.authenforcement.thriftscala._
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.auth.authorization.FixedFeaturePermissionLookup
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterEach, OneInstancePerTest}
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class FixedEnforcerSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers
    with BeforeAndAfterEach {

  private[this] val statsReceiver = new InMemoryStatsReceiver
  private[this] val transactionId = "12345"
  private[this] val emptyPolicy = Policy()

  private[this] val policyOptional = Policy(
    dataPermissionsAnotated = Some(
      Set(
        DataPermission(
          Some(1L),
          state = Some(DataPermissionState.Enforced),
          testing = Some(false)
        ),
        DataPermission(
          Some(2L),
          state = Some(DataPermissionState.Optional),
          testing = Some(false)
        )
      )),
    featurePermissions = Some(Set("can_access_control_tower"))
  )

  private[this] val allDPsOptionalPolicy = Policy(
    dataPermissionsAnotated = Some(
      Set(
        DataPermission(
          Some(1L),
          state = Some(DataPermissionState.Optional),
          testing = Some(false)
        ),
        DataPermission(
          Some(2L),
          state = Some(DataPermissionState.Optional),
          testing = Some(false)
        )
      )),
    featurePermissions = Some(Set("can_access_control_tower"))
  )

  private[this] val policyTesting = Policy(
    dataPermissionsAnotated = Some(
      Set(
        DataPermission(
          Some(1L),
          state = Some(DataPermissionState.Enforced),
          testing = Some(true)
        ),
        DataPermission(
          Some(2L),
          state = Some(DataPermissionState.Enforced),
          testing = Some(false)
        )
      )),
    featurePermissions = Some(Set("can_access_control_tower"))
  )

  private[this] val allDPsTestingPolicy = Policy(
    dataPermissionsAnotated = Some(
      Set(
        DataPermission(
          Some(1L),
          state = Some(DataPermissionState.Enforced),
          testing = Some(true)
        ),
        DataPermission(
          Some(2L),
          state = Some(DataPermissionState.Enforced),
          testing = Some(true)
        )
      )),
    featurePermissions = Some(Set("can_access_control_tower"))
  )

  private[this] val policyAll = Policy(
    dataPermissionsAnotated = Some(
      Set(
        DataPermission(
          Some(1L),
          state = Some(DataPermissionState.Enforced),
          testing = Some(false)
        ),
        DataPermission(
          Some(2L),
          state = Some(DataPermissionState.Enforced),
          testing = Some(false)
        )
      )),
    featurePermissions = Some(Set("can_access_control_tower"))
  )

  private[this] val passportNoDecision = Passport(
    transactionId,
    Set.empty
  )
  private[this] val passportEmptyDecision = Passport(
    transactionId,
    Set.empty,
    Some(DataPermissionDecisions()),
    Some(FeaturePermissionDecisions())
  )
  private[this] val passport1 = Passport(
    transactionId,
    Set(Principal.ClientApplicationPrincipal(ClientApplicationPrincipal(1L))),
    Some(DataPermissionDecisions(allowedDataPermissionIds = Some(Set(1L))))
  )
  private[this] val passport2 = Passport(
    transactionId,
    Set(Principal.ClientApplicationPrincipal(ClientApplicationPrincipal(1L))),
    Some(DataPermissionDecisions(allowedDataPermissionIds = Some(Set(1L, 2L))))
  )
  private[this] val passport3 = Passport(
    transactionId,
    Set.empty,
    Some(DataPermissionDecisions(allowedDataPermissionIds = Some(Set(1L)))),
    None
  )
  private[this] val passport4 = Passport(
    passportId = transactionId,
    principals = Set(Principal.ClientApplicationPrincipal(ClientApplicationPrincipal(1L))),
    dataPermissionDecisions =
      Some(DataPermissionDecisions(allowedDataPermissionIds = Some(Set(2L))))
  )

  private[this] val passport5 = Passport(
    passportId = transactionId,
    principals = Set(Principal.ClientApplicationPrincipal(ClientApplicationPrincipal(1L))),
    dataPermissionDecisions =
      Some(DataPermissionDecisions(allowedDataPermissionIds = Some(Set(1L, 2L))))
  )

  private[this] val enforcer = new FixedEnforcer(FixedFeaturePermissionLookup, statsReceiver)

  override def beforeEach(): Unit = {
    statsReceiver.clear()
  }

  test("test data permission enforcement") {
    Await.result(enforcer.enforce(passportNoDecision, emptyPolicy)).dpAllow mustBe true
    Await.result(enforcer.enforce(passportNoDecision, emptyPolicy)).fpAllow mustBe true

    Await.result(enforcer.enforce(passportEmptyDecision, emptyPolicy)).dpAllow mustBe true
    Await.result(enforcer.enforce(passportEmptyDecision, emptyPolicy)).fpAllow mustBe true

    Await.result(enforcer.enforce(passportNoDecision, policyAll)).dpAllow mustBe false
    Await.result(enforcer.enforce(passportNoDecision, policyAll)).fpAllow mustBe false

    Await.result(enforcer.enforce(passportEmptyDecision, policyAll)).dpAllow mustBe false
    Await.result(enforcer.enforce(passportEmptyDecision, policyAll)).fpAllow mustBe false

    Await.result(enforcer.enforce(passport1, emptyPolicy)).dpAllow mustBe true
    Await.result(enforcer.enforce(passport1, emptyPolicy)).fpAllow mustBe true

    Await.result(enforcer.enforce(passport2, emptyPolicy)).dpAllow mustBe true
    Await.result(enforcer.enforce(passport2, emptyPolicy)).fpAllow mustBe true

    Await.result(enforcer.enforce(passport1, policyAll)).dpAllow mustBe false
    Await.result(enforcer.enforce(passport1, policyAll)).fpAllow mustBe true

    Await.result(enforcer.enforce(passport2, policyAll)).dpAllow mustBe true
    Await.result(enforcer.enforce(passport2, policyAll)).fpAllow mustBe true

  }

  test("test annotated data permission enforcement") {
    // Passport has the mandatory but not the optional DP
    val result1 =
      Await.result(enforcer.enforce(passport3, policyOptional, Some(AuthenticationType.Session)))
    result1.dpAllow mustBe true
    result1.fpAllow mustBe false
    statsReceiver.counters(Seq("fixed_enforcer", "dp_allowed")) mustEqual 1
    statsReceiver.counters(Seq("fixed_enforcer", "fp_rejected")) mustEqual 1

    // Passport has the mandatory but not the optional DP
    val result2 =
      Await.result(enforcer.enforce(passport4, policyTesting, Some(AuthenticationType.Session)))
    result2.dpAllow mustBe true
    result2.fpAllow mustBe true
    statsReceiver.counters(Seq("fixed_enforcer", "fp_allowed")) mustEqual 1
    statsReceiver.counters(Seq("fixed_enforcer", "unsatisfied_testing_dp")) mustEqual 1

    // Passport does not have all required permissions
    val result3 =
      Await.result(enforcer.enforce(passport4, policyAll, Some(AuthenticationType.Session)))
    result3.dpAllow mustBe false
    result3.fpAllow mustBe true
    statsReceiver.counters(Seq("fixed_enforcer", "fp_allowed")) mustEqual 2
    statsReceiver.counters(Seq("fixed_enforcer", "dp_rejected")) mustEqual 1
  }

  test("test annotated data permission when restricted auth type is passed") {
    // Passport has the mandatory but not the optional DP, with restricted session
    // Passport: DP1, Policy: DP1(optional), DP2(optional) => reject request
    val result1 = Await.result(
      enforcer
        .enforce(passport3, allDPsOptionalPolicy, Some(AuthenticationType.RestrictedOauth2Session)))
    result1.dpAllow mustBe false
    result1.fpAllow mustBe false
    statsReceiver.counters(Seq("fixed_enforcer", "dp_rejected")) mustEqual 1
    statsReceiver.counters(Seq("fixed_enforcer", "fp_rejected")) mustEqual 1
    statsReceiver.clear()

    // Passport has the mandatory testing but not the optional DP, with restricted session
    // Passport: DP2, Policy: DP1(testing), DP2(testing) => reject request
    val result2 = Await.result(
      enforcer.enforce(passport4, policyTesting, Some(AuthenticationType.RestrictedOauth2Session)))
    result2.dpAllow mustBe false
    result2.fpAllow mustBe true
    statsReceiver.counters(Seq("fixed_enforcer", "fp_allowed")) mustEqual 1
    statsReceiver.counters(Seq("fixed_enforcer", "dp_rejected")) mustEqual 1
    statsReceiver.clear()

    // Passport does not have all required permissions
    // Passport: DP2, Policy: DP1, DP2 => reject request
    val result3 = Await.result(
      enforcer.enforce(passport4, policyAll, Some(AuthenticationType.RestrictedSession)))
    result3.dpAllow mustBe false
    result3.fpAllow mustBe true
    statsReceiver.counters(Seq("fixed_enforcer", "fp_allowed")) mustEqual 1
    statsReceiver.counters(Seq("fixed_enforcer", "dp_rejected")) mustEqual 1
    statsReceiver.clear()

    // Passport has required dps
    // Passport: DP1, DP2, Policy: DP1, DP2 => accept request
    val result4 =
      Await.result(
        enforcer.enforce(passport5, policyAll, Some(AuthenticationType.RestrictedSession)))
    result4.dpAllow mustBe true
    result4.fpAllow mustBe true
    statsReceiver.counters(Seq("fixed_enforcer", "dp_allowed")) mustEqual 1
    statsReceiver.counters(Seq("fixed_enforcer", "fp_allowed")) mustEqual 1
  }
}
