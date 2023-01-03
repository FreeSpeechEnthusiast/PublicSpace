package com.twitter.auth.policykeeper.api.storage

import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StaticPolicyToRouteTagMappingTest extends StorageTestBase {

  private val jsonLogger = JsonLogger(logger)

  protected val policyStorage = InMemoryPolicyStorage()
  protected val mappingStorage =
    StaticPolicyToRouteTagMapping(
      policyStorage = policyStorage,
      statsReceiver = statsReceiver,
      logger = jsonLogger)

  before {
    statsReceiver.clear()
    policyStorage.clear()
  }

  test("check getAssociatedPolicies method with single policy") {
    policyStorage.storage = Map(PolicyId(testPolicyId) -> testPolicy)
    Await.result(
      mappingStorage.getAssociatedPolicies(Seq(RouteTag("policy_" + testPolicyId)))
    ) mustBe Seq(testPolicy)
  }

  test("check getAssociatedPolicies method with multiple policy") {
    policyStorage.storage =
      Map(PolicyId(testPolicyId) -> testPolicy, PolicyId(testOtherPolicyId) -> testOtherPolicy)
    Await.result(
      mappingStorage.getAssociatedPolicies(
        Seq(RouteTag("policy_" + testPolicyId), RouteTag("policy_" + testOtherPolicyId)))
    ) mustBe Seq(testPolicy, testOtherPolicy)
  }

  test("check getAssociatedPolicies method with unrecognized tag") {
    policyStorage.storage = Map(PolicyId(testPolicyId) -> testPolicy)
    Await.result(
      mappingStorage.getAssociatedPolicies(Seq(RouteTag("someothertag")))
    ) mustBe Seq()
  }

  test("check getAssociatedPolicies method with mix of recognized and unrecognized tags") {
    policyStorage.storage = Map(PolicyId(testPolicyId) -> testPolicy)
    Await.result(
      mappingStorage.getAssociatedPolicies(
        Seq(RouteTag("policy_" + testPolicyId), RouteTag("someothertag")))
    ) mustBe Seq(testPolicy)
  }

  test("check policy retrieval from RouteInformation") {
    policyStorage.storage =
      Map(PolicyId(testPolicyId) -> testPolicy, PolicyId(testOtherPolicyId) -> testOtherPolicy)
    Await.result(
      mappingStorage.getAssociatedPolicies(
        RouteTags.fromRouteInformation(
          RouteInformation(
            isNgRoute = true,
            routeTags =
              Some(Set("policy_" + testPolicyId, "policy_" + testOtherPolicyId, "othertag")))))
    ) mustBe Seq(testPolicy, testOtherPolicy)
  }
}
