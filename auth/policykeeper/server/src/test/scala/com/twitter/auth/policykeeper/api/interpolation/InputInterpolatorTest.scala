package com.twitter.auth.policykeeper.api.interpolation

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.thriftscala.BouncerSettings
import com.twitter.auth.policykeeper.thriftscala.BouncerTargetSettings
import com.twitter.auth.policykeeper.thriftscala.DeepStringMap
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class InputInterpolatorTest extends ServiceTestBase {

  protected val interpolator = InputInterpolator()

  protected val input = ExpressionInput(
    Map(
      ExpressionInputParameterName("clientApplicationId", "auth") -> ExpressionInputParameterValue(
        1L),
      ExpressionInputParameterName("clientApplicationId", "other") -> ExpressionInputParameterValue(
        2L),
      ExpressionInputParameterName("userId") -> ExpressionInputParameterValue(3L),
      ExpressionInputParameterName("token", "access_token") -> ExpressionInputParameterValue(
        "token"),
      ExpressionInputParameterName("tokenKind", "access_token") -> ExpressionInputParameterValue(
        10L),
      ExpressionInputParameterName("sessionHash", "auth") -> ExpressionInputParameterValue(
        "sessionHash"),
      ExpressionInputParameterName("url", "input") -> ExpressionInputParameterValue("/home"),
    ))

  test("test string interpolation, string without placeholders") {
    interpolator.interpolateInputIntoString("auth.input", input) mustBe "auth.input"
  }

  test("test string interpolation, string with placeholders") {
    interpolator.interpolateInputIntoString(
      "do something with {{userId}} and {{other.clientApplicationId}} and {{ auth.clientApplicationId }}",
      input) mustBe "do something with 3 and 2 and 1"
  }

  test("test string interpolation, string with placeholders and unknown vars") {
    interpolator.interpolateInputIntoString(
      "do something {{ unknown }} with {{userId}} and {{other.clientApplicationId}} and {{ auth.clientApplicationId }}",
      input) mustBe "do something  with 3 and 2 and 1"
  }

  test("test deepstringmap interpolation") {
    interpolator.interpolateInputIntoDeepStringMap(
      DeepStringMap(
        isMap = true,
        stringVal = None,
        mapVal = Some(
          Map(
            "token" -> DeepStringMap(
              isMap = false,
              stringVal = Some("{{access_token.token}}"),
              mapVal = None),
            "tokenKind" -> DeepStringMap(
              isMap = false,
              stringVal = Some("{{access_token.tokenKind}}"),
              mapVal = None)
          ))
      ),
      input
    ) mustBe DeepStringMap(
      isMap = true,
      stringVal = None,
      mapVal = Some(
        Map(
          "token" -> DeepStringMap(isMap = false, stringVal = Some("token"), mapVal = None),
          "tokenKind" -> DeepStringMap(isMap = false, stringVal = Some("10"), mapVal = None)
        ))
    )
  }

  test("test bouncer settings interpolation") {
    interpolator
      .interpolateInputIntoSettings(
        testBouncerSettings,
        input
      ) mustBe BouncerSettings(
      location = Some("/account/access?feature=auth_challenge&session=sessionHash"),
      errorMessage = None,
      deepLink = None,
      experience = Some("FullOptional"),
      templateIds = Seq("module_auth_challenge"),
      templateMapping = Some(
        Map(
          "redirectUrl" -> DeepStringMap(isMap = false, stringVal = Some("/home"), mapVal = None),
          "tslaAuthChallengeData" -> DeepStringMap(
            isMap = true,
            stringVal = None,
            mapVal = Some(
              Map(
                "token" -> DeepStringMap(isMap = false, stringVal = Some("token"), mapVal = None),
                "tokenKind" -> DeepStringMap(isMap = false, stringVal = Some("10"), mapVal = None)
              ))
          )
        )),
      referringTags = Some(Set("TSLA", "MODULE")),
      target = BouncerTargetSettings(
        targetType = "session",
        userId = Some("3"),
        sessionHash = Some("sessionHash"),
        feature = Some("auth_challenge")
      )
    )
  }

}
