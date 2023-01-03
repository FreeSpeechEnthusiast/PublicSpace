package com.twitter.auth.policykeeper.api.storage.solutions

import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.FeatureNotSupported
import com.twitter.auth.policykeeper.api.storage.InvalidPolicyIdentifier
import com.twitter.auth.policykeeper.api.storage.RouteTag
import com.twitter.auth.policykeeper.api.storage.StaticPolicyToRouteTagMapping
import com.twitter.auth.policykeeper.api.storage.StorageTestBase
import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.auth.policykeeper.api.storage.configbus.ROConfigBusPolicyStorage
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.when
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar.mock

@RunWith(classOf[JUnitRunner])
class SimpleStorageSolutionTest extends StorageTestBase {

  private val jsonLogger = JsonLogger(logger)

  private val simpleStoragePartialMock = spy(
    SimpleStorageSolution(statsReceiver = statsReceiver, logger = jsonLogger, waitForBoot = false)
  )

  private val roPolicyStorageMock = mock[ROConfigBusPolicyStorage]
  private val roEndpointAssociationStorageMock = mock[StaticPolicyToRouteTagMapping]

  // inject mocked storages
  when(simpleStoragePartialMock.roPolicyStorage).thenReturn(roPolicyStorageMock)
  when(simpleStoragePartialMock.roEndpointAssociationStorage)
    .thenReturn(roEndpointAssociationStorageMock)

  before {
    statsReceiver.clear()
  }

  test("test getPolicyById") {

    when(roPolicyStorageMock.getPoliciesByIds(Seq(PolicyId(testPolicyId))))
      .thenReturn(Future.value(Seq(testPolicy)))

    Await.result(simpleStoragePartialMock.getPolicyById(PolicyId(testPolicyId))) mustBe Some(
      testPolicy)

  }

  test("test getPoliciesByIds") {

    when(
      roPolicyStorageMock.getPoliciesByIds(
        Seq(PolicyId(testPolicyId), PolicyId(testOtherPolicyId))))
      .thenReturn(Future.value(Seq(testPolicy, testOtherPolicy)))

    Await.result(
      simpleStoragePartialMock.getPoliciesByIds(
        Seq(PolicyId(testPolicyId), PolicyId(testOtherPolicyId)))) mustBe Seq(
      testPolicy,
      testOtherPolicy)

  }

  test("test attachAssociatedPolicies") {

    intercept[FeatureNotSupported] {
      Await.result(
        simpleStoragePartialMock.attachAssociatedPolicies(
          Set(RouteTag("policy_" + testPolicyId)),
          Set(PolicyId(testPolicyId))
        )
      )
    }

  }

  test("test detachAssociatedPolicies") {

    intercept[FeatureNotSupported] {
      Await.result(
        simpleStoragePartialMock.detachAssociatedPolicies(
          Set(RouteTag("policy_" + testPolicyId)),
          Some(Set(PolicyId(testPolicyId)))
        )
      )
    }

  }

  test("test syncAssociatedPolicies") {

    intercept[FeatureNotSupported] {
      Await.result(
        simpleStoragePartialMock.syncAssociatedPolicies(
          Set(RouteTag("policy_" + testPolicyId)),
          Set(PolicyId(testPolicyId))
        )
      )
    }

  }

  test("test updatePolicy") {

    intercept[FeatureNotSupported] {
      Await.result(
        simpleStoragePartialMock.updatePolicy(
          PolicyId(testPolicyId),
          testPolicy
        )
      )
    }

  }

  test("test updatePolicy with wrong id") {

    intercept[InvalidPolicyIdentifier] {
      Await.result(
        simpleStoragePartialMock.updatePolicy(
          PolicyId(testPolicyId),
          testPolicy.copy(policyId = invalidPolicyId)
        )
      )
    }

  }

  test("test storePolicy") {

    intercept[FeatureNotSupported] {
      Await.result(
        simpleStoragePartialMock.storePolicy(
          testPolicy
        )
      )
    }

  }

  test("test storePolicy with wrong id") {

    intercept[InvalidPolicyIdentifier] {
      Await.result(
        simpleStoragePartialMock.storePolicy(
          testPolicy.copy(policyId = invalidPolicyId)
        )
      )
    }

  }

  test("test deletePolicy") {

    intercept[FeatureNotSupported] {
      Await.result(
        simpleStoragePartialMock.deletePolicy(
          PolicyId(testPolicyId)
        )
      )
    }

  }

  test("test deletePolicies") {

    intercept[FeatureNotSupported] {
      Await.result(
        simpleStoragePartialMock.deletePolicies(
          Seq(PolicyId(testPolicyId))
        )
      )
    }

  }

  test("test getAssociatedPoliciesIds") {

    val tag = RouteTag("policy_" + testPolicyId)

    when(roEndpointAssociationStorageMock.getAssociatedPoliciesIds(Seq(tag)))
      .thenReturn(Future.value(Seq(PolicyId(testPolicyId))))

    when(roPolicyStorageMock.getPoliciesByIds(Seq(PolicyId(testPolicyId))))
      .thenReturn(Future.value(Seq(testPolicy)))

    Await.result(simpleStoragePartialMock.getAssociatedPolicies(Seq(tag))) mustBe Seq(testPolicy)

  }

}
