package com.twitter.auth.policykeeper.api.storage

import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class InMemoryPolicyStorageTest extends StorageTestBase {

  protected val policyStorage = InMemoryPolicyStorage()

  before {
    statsReceiver.clear()
    policyStorage.clear()
  }

  test("check clear method") {
    policyStorage.storage = Map(PolicyId(testPolicyId) -> testPolicy)
    policyStorage.storage.size mustBe (1)
    policyStorage.clear()
    policyStorage.storage.size mustBe (0)
  }

  test("check getPoliciesByIds method with single policy") {
    policyStorage.storage = Map(PolicyId(testPolicyId) -> testPolicy)
    Await.result(
      policyStorage.getPoliciesByIds(policyIds = Seq(PolicyId(testPolicyId)))
    ) mustBe Seq(testPolicy)
  }

  test("check getPoliciesByIds method with multiple policies") {
    policyStorage.storage =
      Map(PolicyId(testPolicyId) -> testPolicy, PolicyId(testOtherPolicyId) -> testOtherPolicy)
    Await.result(
      policyStorage.getPoliciesByIds(policyIds =
        Seq(PolicyId(testPolicyId), PolicyId(testOtherPolicyId)))
    ) mustBe Seq(testPolicy, testOtherPolicy)
  }

  test("check getPoliciesByIds method with missing policy") {
    policyStorage.storage = Map(PolicyId(testPolicyId) -> testPolicy)
    Await.result(
      policyStorage.getPoliciesByIds(policyIds = Seq(PolicyId(testMissingPolicyId)))
    ) mustBe Seq()
  }

  test("check getPoliciesByIds method with multiple policies and partial match") {
    policyStorage.storage = Map(PolicyId(testPolicyId) -> testPolicy)
    Await.result(
      policyStorage.getPoliciesByIds(policyIds =
        Seq(PolicyId(testPolicyId), PolicyId(testOtherPolicyId)))
    ) mustBe Seq(testPolicy)
  }

  test("check replaceDataWithNewVersion method") {
    policyStorage.storage = Map(PolicyId(testPolicyId) -> testPolicy)
    policyStorage.replaceDataWithNewVersion(data = Set(testOtherPolicy))
    Await.result(
      policyStorage.getPoliciesByIds(policyIds = Seq(PolicyId(testPolicyId)))
    ) mustBe Seq()
    Await.result(
      policyStorage.getPoliciesByIds(policyIds = Seq(PolicyId(testOtherPolicyId)))
    ) mustBe Seq(testOtherPolicy)
  }

}
