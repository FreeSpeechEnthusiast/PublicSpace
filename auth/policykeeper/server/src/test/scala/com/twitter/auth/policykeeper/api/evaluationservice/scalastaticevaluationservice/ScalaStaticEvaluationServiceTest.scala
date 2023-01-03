package com.twitter.auth.policykeeper.api.evaluationservice.scalastaticevaluationservice

import com.twitter.auth.policykeeper.api.context.LocalContext
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationservice.DataProviderWaitDeadlineExceededException
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.Rule
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ScalaStaticEvaluationServiceTest extends EvaluationTestBase {

  protected val testPolicyId = "policy1"
  protected val testPolicy = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "DemoAuthRule(auth.authenticatedUserId)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "testPolicy",
    description = "",
  )

  protected val testPolicyDisablerId = "policyDisabler"
  protected val testPolicyDisabler = Policy(
    policyId = testPolicyId,
    decider = Some("policyDisabler"),
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "DemoAuthRule(auth.authenticatedUserId)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "testPolicyDisabler",
    description = "",
  )

  protected val testSlowPolicyId = "slowPolicyId"
  protected val testSlowPolicy = Policy(
    policyId = testSlowPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "DemoAuthRule(unittests_slow.var1)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "slowPolicy",
    description = "",
  )

  protected val testFaultyPolicyId = "slowPolicyId"
  protected val testFaultyPolicy = Policy(
    policyId = testFaultyPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "DemoAuthRule(unittests_faulty.var1)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "faultyPolicy",
    description = "",
  )

  test("test ScalaStaticEvaluationService (valid, successful, with custom input)") {
    Await
      .result(
        evaluationService
          .execute(
            policy = testPolicy,
            routeInformation = None,
            customInput = Some(
              ExpressionInput(
                Map(
                  ExpressionInputParameterName(
                    "authenticatedUserId",
                    "auth") -> ExpressionInputParameterValue(1L)
                )
              )),
            authMetadata = None
          )
      ).ruleAction mustBe Some(RuleAction(false, None))
    statsReceiver.counters(
      List(
        evaluationService.Scope,
        evaluationService.policyScopeName(testPolicy),
        evaluationService.PolicyPassed)) mustEqual 1L
    statsReceiver.counters(
      List(evaluationService.Scope, evaluationService.PolicyPassed)) mustEqual 1L
  }

  test(
    "test ScalaStaticEvaluationService (valid, successful, with custom input, with disabler false)") {
    when(decider.isAvailable("policyDisabler")).thenReturn(false)
    Await
      .result(
        evaluationService
          .execute(
            policy = testPolicyDisabler,
            routeInformation = None,
            customInput = Some(
              ExpressionInput(
                Map(
                  ExpressionInputParameterName(
                    "authenticatedUserId",
                    "auth") -> ExpressionInputParameterValue(1L)
                )
              )),
            authMetadata = None
          )
      ).ruleAction mustBe None
    statsReceiver.counters(
      List(
        evaluationService.Scope,
        evaluationService.policyScopeName(testPolicyDisabler),
        evaluationService.PolicyDisabled)) mustEqual 1L
    statsReceiver.counters(
      List(evaluationService.Scope, evaluationService.PolicyDisabled)) mustEqual 1L
  }

  test(
    "test ScalaStaticEvaluationService (valid, successful, with custom input, with disabler true)") {
    when(decider.isAvailable("policyDisabler")).thenReturn(true)
    Await
      .result(
        evaluationService
          .execute(
            policy = testPolicyDisabler,
            routeInformation = None,
            customInput = Some(
              ExpressionInput(
                Map(
                  ExpressionInputParameterName(
                    "authenticatedUserId",
                    "auth") -> ExpressionInputParameterValue(1L)
                )
              )),
            authMetadata = None
          )
      ).ruleAction mustBe Some(RuleAction(false, None))
    statsReceiver.counters(
      List(
        evaluationService.Scope,
        evaluationService.policyScopeName(testPolicyDisabler),
        evaluationService.PolicyPassed)) mustEqual 1L
    statsReceiver.counters(
      List(evaluationService.Scope, evaluationService.PolicyPassed)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationService (valid, unsuccessful, with custom input)") {
    Await
      .result(
        evaluationService
          .execute(
            policy = testPolicy,
            routeInformation = None,
            customInput = Some(
              ExpressionInput(
                Map(
                  ExpressionInputParameterName(
                    "authenticatedUserId",
                    "auth") -> ExpressionInputParameterValue(0L)
                )
              )),
            authMetadata = None
          )
      ).ruleAction mustBe None
    statsReceiver.counters(
      List(
        evaluationService.Scope,
        evaluationService.policyScopeName(testPolicy),
        evaluationService.PolicyFailed)) mustEqual 1L
    statsReceiver.counters(
      List(evaluationService.Scope, evaluationService.PolicyFailed)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationService (valid, successful, without custom input)") {
    LocalContext.writeToLocalContexts(Some(123L), Some(1L), Some("sss"), None, Some(1L)) {
      Await
        .result(
          evaluationService
            .execute(
              policy = testPolicy,
              routeInformation = None,
              customInput = None,
              authMetadata = None
            )
        ).ruleAction mustBe Some(RuleAction(false, None))
      statsReceiver.counters(
        List(
          evaluationService.Scope,
          evaluationService.policyScopeName(testPolicy),
          evaluationService.PolicyPassed)) mustEqual 1L
      statsReceiver.counters(
        List(evaluationService.Scope, evaluationService.PolicyPassed)) mustEqual 1L
    }
  }

  test(
    "test ScalaStaticEvaluationService (valid, unsuccessful, without custom input, slow data provider)") {
    intercept[DataProviderWaitDeadlineExceededException] {
      Await
        .result(
          evaluationService
            .execute(
              policy = testSlowPolicy,
              routeInformation = None,
              customInput = None,
              authMetadata = None
            )
        )
    }
    statsReceiver.counters(
      List(
        evaluationService.Scope,
        evaluationService.policyScopeName(testSlowPolicy),
        evaluationService.PolicyDataProviderTimeout)) mustEqual 1L
    statsReceiver.counters(
      List(evaluationService.Scope, evaluationService.PolicyDataProviderTimeout)) mustEqual 1L
  }

  test(
    "test ScalaStaticEvaluationService (valid, unsuccessful, without custom input, faulty data provider)") {
    intercept[Exception] {
      Await
        .result(
          evaluationService
            .execute(
              policy = testFaultyPolicy,
              routeInformation = None,
              customInput = None,
              authMetadata = None
            )
        )
    }
    statsReceiver.counters(
      List(
        evaluationService.Scope,
        evaluationService.policyScopeName(testFaultyPolicy),
        evaluationService.PolicyDataProviderFailed)) mustEqual 1L
    statsReceiver.counters(
      List(evaluationService.Scope, evaluationService.PolicyDataProviderFailed)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationService (valid, unsuccessful, without custom input)") {
    LocalContext.writeToLocalContexts(Some(123L), Some(0L), Some("sss"), None, Some(0L)) {
      Await
        .result(
          evaluationService
            .execute(
              policy = testPolicy,
              routeInformation = None,
              customInput = None,
              authMetadata = None
            )
        ).ruleAction mustBe None
      statsReceiver.counters(
        List(
          evaluationService.Scope,
          evaluationService.policyScopeName(testPolicy),
          evaluationService.PolicyFailed)) mustEqual 1L
      statsReceiver.counters(
        List(evaluationService.Scope, evaluationService.PolicyFailed)) mustEqual 1L
    }
  }

  test("test ScalaStaticEvaluationService (valid, custom input override)") {
    LocalContext.writeToLocalContexts(Some(123L), Some(0L), Some("sss"), None, Some(0L)) {
      Await
        .result(
          evaluationService
            .execute(
              policy = testPolicy,
              routeInformation = None,
              customInput = Some(
                ExpressionInput(
                  Map(
                    ExpressionInputParameterName(
                      "authenticatedUserId",
                      "auth") -> ExpressionInputParameterValue(1L)
                  )
                )),
              authMetadata = None
            )
        ).ruleAction mustBe Some(RuleAction(false, None))
      statsReceiver.counters(
        List(
          evaluationService.Scope,
          evaluationService.policyScopeName(testPolicy),
          evaluationService.PolicyPassed)) mustEqual 1L
      statsReceiver.counters(
        List(evaluationService.Scope, evaluationService.PolicyPassed)) mustEqual 1L
    }
  }

}
