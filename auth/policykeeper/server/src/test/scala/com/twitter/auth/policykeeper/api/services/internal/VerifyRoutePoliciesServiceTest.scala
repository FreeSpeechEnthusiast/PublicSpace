package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine.ScalaStaticExpression
import com.twitter.auth.policykeeper.api.evaluationservice.EvaluatedActionWithInput
import com.twitter.auth.policykeeper.api.evaluationservice.EvaluationServiceInterface
import com.twitter.auth.policykeeper.api.storage.RouteTag
import com.twitter.auth.policykeeper.api.storage.RouteTags
import com.twitter.auth.policykeeper.api.storage.StorageInterface
import com.twitter.auth.policykeeper.thriftscala.Code
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService.VerifyRoutePolicies
import com.twitter.auth.policykeeper.thriftscala.Result
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.auth.policykeeper.thriftscala.VerifyRoutePoliciesRequest
import com.twitter.auth.policykeeper.thriftscala.VerifyRoutePoliciesResponse
import com.twitter.scrooge.Request
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VerifyRoutePoliciesServiceTest extends ServiceTestBase {

  private val storage = mock[StorageInterface[String, RouteTag]]
  private val evaluationService = mock[EvaluationServiceInterface[ScalaStaticExpression]]
  private val service =
    new VerifyRoutePoliciesService(
      storage = storage,
      evaluationService = evaluationService,
      statsReceiver = statsReceiver,
      logger = jsonLogger,
      advancedStatsReceiver = advancedStatsReceiver)

  test("test VerifyRoutePoliciesRequest (0 policies, success with code, no custom data)") {
    val routeInformation = RouteInformation(isNgRoute = true, routeTags = None)
    when(
      storage.getAssociatedPolicies(associatedData =
        RouteTags.fromRouteInformation(routeInformation)))
      .thenReturn(Future.value(Seq()))
    Await
      .result(
        service(
          Request(args = VerifyRoutePolicies.Args(request = VerifyRoutePoliciesRequest(
            routeInformation = routeInformation,
            customInput = None,
            authMetadata = None))))
      ).value mustBe VerifyRoutePoliciesResponse(executionResult =
      Result(policyExecutionCode = Code.Noresults, apiErrorCode = None, bouncerRequest = None))
    statsReceiver.counters(List(service.Scope, service.FailClosedPolicyEvaluated)) mustEqual 0L
  }

  test("test VerifyRoutePoliciesRequest (1 policy, success with code, no custom data)") {
    val routeInformation = RouteInformation(isNgRoute = true, routeTags = Some(Set(testPolicyId)))
    when(
      storage.getAssociatedPolicies(associatedData =
        RouteTags.fromRouteInformation(routeInformation)))
      .thenReturn(Future.value(Seq(testPolicy)))
    when(
      evaluationService
        .execute(
          policy = testPolicy,
          routeInformation = Some(routeInformation),
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
          Request(args = VerifyRoutePolicies.Args(request = VerifyRoutePoliciesRequest(
            routeInformation = routeInformation,
            customInput = None,
            authMetadata = None))))
      ).value mustBe VerifyRoutePoliciesResponse(executionResult =
      Result(policyExecutionCode = Code.True, apiErrorCode = Some(501), bouncerRequest = None))
    statsReceiver.counters(List(service.Scope, service.FailClosedPolicyEvaluated)) mustEqual 1L
    statsReceiver.counters(
      List(
        service.Scope,
        service.policyScopeName(testPolicy),
        service.FailClosedPolicyEvaluated)) mustEqual 1L
  }

  test("test VerifyRoutePoliciesRequest (2 policies, mixed, no custom data)") {
    val routeInformation =
      RouteInformation(isNgRoute = true, routeTags = Some(Set(testPolicyId, testAnotherPolicyId)))
    when(
      storage.getAssociatedPolicies(associatedData =
        RouteTags.fromRouteInformation(routeInformation)))
      .thenReturn(Future.value(Seq(testPolicy, testAnotherPolicy)))
    when(
      evaluationService.execute(
        policy = testPolicy,
        routeInformation = Some(routeInformation),
        customInput = None,
        authMetadata = None
      ))
      .thenReturn(
        Future.value(
          EvaluatedActionWithInput(
            Some(RuleAction(actionNeeded = true, apiErrorCode = Some(502))),
            input)))
    when(
      evaluationService.execute(
        policy = testAnotherPolicy,
        routeInformation = Some(routeInformation),
        customInput = None,
        authMetadata = None
      ))
      .thenReturn(Future.value(EvaluatedActionWithInput(None, input)))
    Await
      .result(
        service(
          Request(args = VerifyRoutePolicies.Args(request = VerifyRoutePoliciesRequest(
            routeInformation = routeInformation,
            customInput = None,
            authMetadata = None
          ))))
      ).value mustBe VerifyRoutePoliciesResponse(executionResult =
      Result(policyExecutionCode = Code.Mixed, apiErrorCode = Some(502), bouncerRequest = None))
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

  test(
    "test VerifyRoutePoliciesRequest (1 policy, success with code, no custom data, with bouncer)") {
    val routeInformation =
      RouteInformation(isNgRoute = true, routeTags = Some(Set(testPolicyWithBouncerId)))
    when(
      storage.getAssociatedPolicies(associatedData =
        RouteTags.fromRouteInformation(routeInformation)))
      .thenReturn(Future.value(Seq(testPolicyWithBouncer)))
    when(
      evaluationService
        .execute(
          policy = testPolicyWithBouncer,
          routeInformation = Some(routeInformation),
          customInput = None,
          authMetadata = None))
      .thenReturn(
        Future.value(
          EvaluatedActionWithInput(
            Some(
              RuleAction(
                actionNeeded = true,
                apiErrorCode = None,
                bouncerSettings = Some(testBouncerSettings))),
            testBouncerInput)))
    Await
      .result(
        service(
          Request(args = VerifyRoutePolicies.Args(request = VerifyRoutePoliciesRequest(
            routeInformation = routeInformation,
            customInput = None,
            authMetadata = None))))
      ).value mustBe VerifyRoutePoliciesResponse(executionResult = Result(
      policyExecutionCode = Code.True,
      apiErrorCode = None,
      bouncerRequest = Some(expectedBouncerRequest)))
    statsReceiver.counters(List(service.Scope, service.FailClosedPolicyEvaluated)) mustEqual 1L
    statsReceiver.counters(
      List(
        service.Scope,
        service.policyScopeName(testPolicyWithBouncer),
        service.FailClosedPolicyEvaluated)) mustEqual 1L
  }

  test(
    "test VerifyRoutePoliciesRequest (1 policy, success with code, no custom data, with bouncer, fail-open)") {
    val routeInformation =
      RouteInformation(isNgRoute = true, routeTags = Some(Set(testPolicyWithBouncerId)))
    when(
      storage.getAssociatedPolicies(associatedData =
        RouteTags.fromRouteInformation(routeInformation)))
      .thenReturn(Future.value(Seq(testPolicyWithBouncer.copy(failClosed = Some(false)))))
    when(
      evaluationService
        .execute(
          policy = testPolicyWithBouncer.copy(failClosed = Some(false)),
          routeInformation = Some(routeInformation),
          customInput = None,
          authMetadata = None))
      .thenReturn(
        Future.value(
          EvaluatedActionWithInput(
            Some(
              RuleAction(
                actionNeeded = true,
                apiErrorCode = None,
                bouncerSettings = Some(testBouncerSettings))),
            testBouncerInput)))
    Await
      .result(
        service(
          Request(args = VerifyRoutePolicies.Args(request = VerifyRoutePoliciesRequest(
            routeInformation = routeInformation,
            customInput = None,
            authMetadata = None))))
      ).value mustBe VerifyRoutePoliciesResponse(executionResult =
      Result(policyExecutionCode = Code.False, apiErrorCode = None, bouncerRequest = None))
    statsReceiver.counters(List(service.Scope, service.FailOpenPolicyEvaluated)) mustEqual 1L
    statsReceiver.counters(
      List(
        service.Scope,
        service.policyScopeName(testPolicyWithBouncer),
        service.FailOpenPolicyEvaluated)) mustEqual 1L
  }

}
