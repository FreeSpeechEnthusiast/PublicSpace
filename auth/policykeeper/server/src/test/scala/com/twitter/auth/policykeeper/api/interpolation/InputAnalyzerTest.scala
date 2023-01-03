package com.twitter.auth.policykeeper.api.interpolation

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class InputAnalyzerTest extends ServiceTestBase {

  protected val inputAnalyzer = InputAnalyzer()

  test("test string analyzer, string without placeholders") {
    inputAnalyzer.requiredInputForString("auth.input") mustBe Set()
  }

  test("test string analyzer, string with placeholders") {
    inputAnalyzer.requiredInputForString(
      "do something with {{userId}} and {{other.clientApplicationId}} and {{ auth.clientApplicationId }}") mustBe Set(
      ExpressionInputParameterName("userId"),
      ExpressionInputParameterName("clientApplicationId", "other"),
      ExpressionInputParameterName("clientApplicationId", "auth")
    )
  }

  test("test bouncer settings interpolation, action is not required") {
    inputAnalyzer
      .requiredInputFor(
        RuleAction(
          actionNeeded = false,
          apiErrorCode = None,
          bouncerSettings = Some(testBouncerSettings))
      ) mustBe
      Set()
  }

  test("test bouncer settings interpolation, http code specified") {
    inputAnalyzer
      .requiredInputFor(
        RuleAction(
          actionNeeded = false,
          apiErrorCode = Some(200),
          bouncerSettings = Some(testBouncerSettings))
      ) mustBe
      Set()
  }

  test("test bouncer settings interpolation") {
    inputAnalyzer
      .requiredInputFor(
        RuleAction(
          actionNeeded = true,
          apiErrorCode = None,
          bouncerSettings = Some(testBouncerSettings))
      ) mustBe
      Set(
        ExpressionInputParameterName("url", "input"),
        ExpressionInputParameterName("token", "access_token"),
        ExpressionInputParameterName("tokenKind", "access_token"),
        ExpressionInputParameterName("sessionHash", "auth"),
        ExpressionInputParameterName("userId")
      )
  }

}
