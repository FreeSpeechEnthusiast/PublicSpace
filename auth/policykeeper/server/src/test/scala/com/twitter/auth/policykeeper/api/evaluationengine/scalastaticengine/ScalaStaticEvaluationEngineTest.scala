package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputTypeCastException
import com.twitter.auth.policykeeper.api.evaluationengine.IncompleteInputException
import com.twitter.auth.policykeeper.api.evaluationengine.InvalidRuleExpression
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.services.PassbirdService
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.Rule
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.logging.Logger
import com.twitter.servo.util.MemoizingStatsReceiver
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar.mock

@RunWith(classOf[JUnitRunner])
class ScalaStaticEvaluationEngineTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  protected val statsReceiver = new InMemoryStatsReceiver
  protected val perPolicyStatsReceiver: MemoizingStatsReceiver =
    new MemoizingStatsReceiver(statsReceiver)
  protected val logger = Logger.get()
  protected val jsonLogger = JsonLogger(logger)
  protected val passbirdServiceMock = mock[PassbirdService]

  before {
    statsReceiver.clear()
  }

  private val evaluationEngine: ScalaStaticEvaluationEngine =
    ScalaStaticEvaluationEngine(
      statsReceiver = statsReceiver,
      logger = jsonLogger,
      perPolicyStatsReceiver = perPolicyStatsReceiver,
      passbirdService = passbirdServiceMock)

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

  protected val testPolicyWithFallback = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "DemoAuthRule(auth.authenticatedUserId)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0,
        fallbackAction = Some(RuleAction(actionNeeded = true, apiErrorCode = Some(400)))
      )
    ),
    name = "testPolicyWithFallback",
    description = "",
  )

  protected val testPolicyWithUnknownExpression = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "boom(auth.authenticatedUserId)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "testPolicy",
    description = "",
  )

  protected val testPolicyWithInvalidExpression = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "DemoAuthRule(auth.authenticatedUserId,",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "testPolicy",
    description = "",
  )

  protected val testInput =
    ExpressionInput(
      Map(
        ExpressionInputParameterName(
          "authenticatedUserId",
          "auth") -> ExpressionInputParameterValue(1)
      )
    )

  protected val testExtraInput =
    ExpressionInput(
      Map(
        ExpressionInputParameterName("something", "auth") -> ExpressionInputParameterValue("true"),
        ExpressionInputParameterName(
          "authenticatedUserId",
          "auth") -> ExpressionInputParameterValue(100L),
        ExpressionInputParameterName("somethingelse", "auth") -> ExpressionInputParameterValue(
          "true"),
      )
    )

  protected val testOtherInput =
    ExpressionInput(
      Map(
        ExpressionInputParameterName(
          "authenticatedUserId",
          "auth") -> ExpressionInputParameterValue(0),
      )
    )

  protected val testIncompleteInput =
    ExpressionInput(
      Map(
      )
    )

  protected val testBadInput =
    ExpressionInput(
      Map(
        ExpressionInputParameterName(
          "authenticatedUserId",
          "auth") -> ExpressionInputParameterValue("a"),
      )
    )

  protected val testPolicyPositionArg = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "DemoEndpointRule(someSmallInt, someBigInt, someBoolArg)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "testPolicy",
    description = "",
  )

  protected val testPolicyAnotherPositionArg = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "DemoEndpointRule(someBigInt, someSmallInt, someBoolArg)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "testPolicy",
    description = "",
  )

  protected val testPositionArgInput =
    ExpressionInput(
      Map(
        ExpressionInputParameterName("someSmallInt") -> ExpressionInputParameterValue("20"),
        ExpressionInputParameterName("someBigInt") -> ExpressionInputParameterValue("1100"),
        ExpressionInputParameterName("someBoolArg") -> ExpressionInputParameterValue("true")
      )
    )

  protected val testPositionArgInputNonString =
    ExpressionInput(
      Map(
        ExpressionInputParameterName("someSmallInt") -> ExpressionInputParameterValue(20),
        ExpressionInputParameterName("someBigInt") -> ExpressionInputParameterValue(1100),
        ExpressionInputParameterName("someBoolArg") -> ExpressionInputParameterValue(true)
      )
    )

  test("test ScalaStaticEvaluationEngine validation (valid)") {
    Await.result(evaluationEngine.validatePolicyWithInput(testPolicy, testInput)) mustBe true
  }

  test("test ScalaStaticEvaluationEngine validation (valid, extra input)") {
    Await.result(evaluationEngine.validatePolicyWithInput(testPolicy, testExtraInput)) mustBe true
  }

  test("test ScalaStaticEvaluationEngine validation (invalid)") {
    Await.result(
      evaluationEngine.validatePolicyWithInput(testPolicy, testIncompleteInput)) mustBe false
  }

  test("test ScalaStaticEvaluationEngine evaluation (valid)") {
    Await.result(evaluationEngine.execute(testPolicy, testInput)) mustBe Some(
      RuleAction(false, None))
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyRequested)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationEngine evaluation (valid, extra input)") {
    Await.result(evaluationEngine.execute(testPolicy, testExtraInput)) mustBe Some(
      RuleAction(false, None))
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyRequested)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationEngine evaluation (valid, other input)") {
    Await.result(evaluationEngine.execute(testPolicy, testOtherInput)) mustBe None
  }

  test("test ScalaStaticEvaluationEngine evaluation (invalid, incomplete input)") {
    Await.result(
      evaluationEngine
        .validatePolicyWithInput(testPolicy, testIncompleteInput)) mustBe false
    intercept[IncompleteInputException] {
      Await.result(evaluationEngine.execute(testPolicy, testIncompleteInput)) mustBe false
    }
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyFailedDueToIncompleteInput)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyFailedDueToIncompleteInput)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationEngine evaluation (invalid, bad input)") {
    Await.result(
      evaluationEngine
        .validatePolicyWithInput(testPolicy, testBadInput)) mustBe true
    intercept[ExpressionInputTypeCastException] {
      Await.result(evaluationEngine.execute(testPolicy, testBadInput))
    }
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.PolicyFailedDueToInputCastException)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyFailedDueToInputCastException)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationEngine evaluation (valid, incomplete input, fallback action)") {
    Await.result(evaluationEngine.execute(testPolicyWithFallback, testIncompleteInput)) mustBe Some(
      RuleAction(true, Some(400)))
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyRequested)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationEngine evaluation (invalid, unknown expression)") {
    Await.result(
      evaluationEngine
        .validatePolicyWithInput(testPolicyWithUnknownExpression, testInput)) mustBe true
    intercept[UnknownExpressionClassException] {
      Await.result(evaluationEngine.execute(testPolicyWithUnknownExpression, testInput))
    }
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyFailedDueToOtherException)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyFailedDueToOtherException)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationEngine validation (invalid, bad expression)") {
    Await.result(
      evaluationEngine
        .validatePolicyWithInput(testPolicyWithInvalidExpression, testInput)) mustBe false
    intercept[InvalidRuleExpression] {
      Await.result(evaluationEngine.execute(testPolicyWithInvalidExpression, testInput))
    }
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.PolicyFailedDueToInvalidRuleExpression)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyFailedDueToInvalidRuleExpression)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationEngine (valid, positioned args, 3 params)") {
    Await.result(evaluationEngine.execute(testPolicyPositionArg, testPositionArgInput)) mustBe Some(
      RuleAction(false, None))
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyRequested)) mustEqual 1L
  }

  test(
    "test ScalaStaticEvaluationEngine (valid, positioned args, 3 params, arguments order changed)") {
    Await.result(
      evaluationEngine.execute(testPolicyAnotherPositionArg, testPositionArgInput)) mustBe None
    statsReceiver.counters(
      List(evaluationEngine.Scope, evaluationEngine.PolicyRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        evaluationEngine.Scope,
        evaluationEngine.policyScopeName(testPolicy),
        evaluationEngine.PolicyRequested)) mustEqual 1L
  }

  test("test ScalaStaticEvaluationEngine (valid, positioned args, 3 params, other input types)") {
    Await.result(
      evaluationEngine.execute(testPolicyPositionArg, testPositionArgInputNonString)) mustBe Some(
      RuleAction(false, None))
  }

  test(
    "test ScalaStaticEvaluationEngine (valid, positioned args, 3 params, arguments order changed, other input types)") {
    Await.result(
      evaluationEngine
        .execute(testPolicyAnotherPositionArg, testPositionArgInputNonString)) mustBe None
  }

}
