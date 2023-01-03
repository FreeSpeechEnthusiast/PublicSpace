package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine.ScalaStaticExpression
import com.twitter.auth.policykeeper.api.evaluationservice.EvaluatedActionWithInput
import com.twitter.auth.policykeeper.api.evaluationservice.EvaluationServiceInterface
import com.twitter.auth.policykeeper.api.storage.RouteTag
import com.twitter.auth.policykeeper.api.storage.StorageInterface
import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.auth.policykeeper.thriftscala.Code
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService.VerifyPolicies
import com.twitter.auth.policykeeper.thriftscala.Result
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.auth.policykeeper.thriftscala.VerifyPoliciesRequest
import com.twitter.auth.policykeeper.thriftscala.VerifyPoliciesResponse
import com.twitter.scrooge.Request
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VerifyPoliciesServiceTest extends ServiceTestBase {

  private val storage = mock[StorageInterface[String, RouteTag]]
  private val evaluationService = mock[EvaluationServiceInterface[ScalaStaticExpression]]
  private val service =
    new VerifyPoliciesService(
      storage = storage,
      evaluationService = evaluationService,
      statsReceiver = statsReceiver,
      logger = jsonLogger,
      advancedStatsReceiver = advancedStatsReceiver)

  test("test VerifyPoliciesRequest (1 policy, success with code, no custom data)") {
    when(storage.getPoliciesByIds(policyIds = Seq(PolicyId(testPolicyId))))
      .thenReturn(Future.value(Seq(testPolicy)))
    when(
      evaluationService.execute(
        policy = testPolicy,
        routeInformation = None,
        customInput = None,
        authMetadata = None))
      .thenReturn(
        Future.value(
          EvaluatedActionWithInput(
            Some(RuleAction(actionNeeded = true, apiErrorCode = Some(501))),
            input)))
    Await
      .result(
        service(
          Request(args = VerifyPolicies.Args(request =
            VerifyPoliciesRequest(policyIds = Set(testPolicyId), customInput = None))))
      ).value mustBe VerifyPoliciesResponse(executionResult =
      Result(policyExecutionCode = Code.True, apiErrorCode = Some(501), bouncerRequest = None))
    statsReceiver.counters(List(service.Scope, service.FailClosedPolicyEvaluated)) mustEqual 1L
    statsReceiver.counters(
      List(
        service.Scope,
        service.policyScopeName(testPolicy),
        service.FailClosedPolicyEvaluated)) mustEqual 1L
  }

  test("test VerifyPoliciesRequest (1 policy, success without code, no custom data)") {
    when(storage.getPoliciesByIds(policyIds = Seq(PolicyId(testPolicyId))))
      .thenReturn(Future.value(Seq(testPolicy)))
    when(
      evaluationService.execute(
        policy = testPolicy,
        routeInformation = None,
        customInput = None,
        authMetadata = None))
      .thenReturn(
        Future.value(
          EvaluatedActionWithInput(
            Some(RuleAction(actionNeeded = false, apiErrorCode = None)),
            input)))
    Await
      .result(
        service(
          Request(args = VerifyPolicies.Args(request =
            VerifyPoliciesRequest(policyIds = Set(testPolicyId), customInput = None))))
      ).value mustBe VerifyPoliciesResponse(executionResult =
      Result(policyExecutionCode = Code.True, apiErrorCode = None, bouncerRequest = None))
    statsReceiver.counters(List(service.Scope, service.FailClosedPolicyEvaluated)) mustEqual 1L
    statsReceiver.counters(
      List(
        service.Scope,
        service.policyScopeName(testPolicy),
        service.FailClosedPolicyEvaluated)) mustEqual 1L
  }

  test("test VerifyPoliciesRequest (1 policy, false, no custom data)") {
    when(storage.getPoliciesByIds(policyIds = Seq(PolicyId(testPolicyId))))
      .thenReturn(Future.value(Seq(testPolicy)))
    when(
      evaluationService.execute(
        policy = testPolicy,
        routeInformation = None,
        customInput = None,
        authMetadata = None))
      .thenReturn(Future.value(EvaluatedActionWithInput(None, input)))
    Await
      .result(
        service(
          Request(args = VerifyPolicies.Args(request = VerifyPoliciesRequest(
            policyIds = Set(testPolicyId),
            customInput = None,
            authMetadata = None))))
      ).value mustBe VerifyPoliciesResponse(executionResult =
      Result(policyExecutionCode = Code.False, apiErrorCode = None, bouncerRequest = None))
    statsReceiver.counters(List(service.Scope, service.FailClosedPolicyEvaluated)) mustEqual 1L
    statsReceiver.counters(
      List(
        service.Scope,
        service.policyScopeName(testPolicy),
        service.FailClosedPolicyEvaluated)) mustEqual 1L
  }

  test("test VerifyPoliciesRequest (1 policy, failed, no custom data)") {
    when(storage.getPoliciesByIds(policyIds = Seq(PolicyId(testPolicyId))))
      .thenReturn(Future.value(Seq(testPolicy)))
    when(
      evaluationService.execute(
        policy = testPolicy,
        routeInformation = None,
        customInput = None,
        authMetadata = None))
      .thenReturn(Future.exception(new Exception))
    Await
      .result(
        service(
          Request(args = VerifyPolicies.Args(request = VerifyPoliciesRequest(
            policyIds = Set(testPolicyId),
            customInput = None,
            authMetadata = None))))
      ).value mustBe VerifyPoliciesResponse(executionResult =
      Result(policyExecutionCode = Code.Failed, apiErrorCode = None, bouncerRequest = None))
  }

  test("test VerifyPoliciesRequest (1 policy, success, with custom data)") {
    when(storage.getPoliciesByIds(policyIds = Seq(PolicyId(testPolicyId))))
      .thenReturn(Future.value(Seq(testPolicy)))
    when(
      evaluationService.execute(
        policy = testPolicy,
        routeInformation = None,
        customInput = Some(ExpressionInput(
          Map(ExpressionInputParameterName("key") -> ExpressionInputParameterValue("value")))),
        authMetadata = None
      ))
      .thenReturn(
        Future.value(
          EvaluatedActionWithInput(
            Some(RuleAction(actionNeeded = true, apiErrorCode = Some(501))),
            input)))
    Await
      .result(
        service(
          Request(args = VerifyPolicies.Args(request = VerifyPoliciesRequest(
            policyIds = Set(testPolicyId),
            customInput = Some(Map("key" -> "value"))))))
      ).value mustBe VerifyPoliciesResponse(executionResult =
      Result(policyExecutionCode = Code.True, apiErrorCode = Some(501), bouncerRequest = None))
    statsReceiver.counters(List(service.Scope, service.FailClosedPolicyEvaluated)) mustEqual 1L
    statsReceiver.counters(
      List(
        service.Scope,
        service.policyScopeName(testPolicy),
        service.FailClosedPolicyEvaluated)) mustEqual 1L
  }

  test("test VerifyPoliciesRequest (2 policies, mixed, no custom data)") {
    when(
      storage.getPoliciesByIds(policyIds =
        Seq(PolicyId(testPolicyId), PolicyId(testAnotherPolicyId))))
      .thenReturn(Future.value(Seq(testPolicy, testAnotherPolicy)))
    when(
      evaluationService.execute(
        policy = testPolicy,
        routeInformation = None,
        customInput = None,
        authMetadata = None))
      .thenReturn(Future.value(EvaluatedActionWithInput(None, input)))
    when(
      evaluationService
        .execute(
          policy = testAnotherPolicy,
          routeInformation = None,
          customInput = None,
          authMetadata = None))
      .thenReturn(
        Future.value(
          EvaluatedActionWithInput(
            Some(RuleAction(actionNeeded = true, apiErrorCode = Some(501))),
            input)))
    Await
      .result(
        service(
          Request(args = VerifyPolicies.Args(request = VerifyPoliciesRequest(
            policyIds = Set(testPolicyId, testAnotherPolicyId),
            customInput = None))))
      ).value mustBe VerifyPoliciesResponse(executionResult =
      Result(policyExecutionCode = Code.Mixed, apiErrorCode = Some(501), bouncerRequest = None))
    statsReceiver.counters(List(service.Scope, service.FailClosedPolicyEvaluated)) mustEqual 2L
    statsReceiver.counters(
      List(
        service.Scope,
        service.policyScopeName(testPolicy),
        service.FailClosedPolicyEvaluated)) mustEqual 1L
    statsReceiver.counters(
      List(
        service.Scope,
        service.policyScopeName(testAnotherPolicy),
        service.FailClosedPolicyEvaluated)) mustEqual 1L
  }

}
