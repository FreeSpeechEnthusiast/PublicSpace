package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.bouncer.templates.thriftscala.TSLAAuthChallengeData
import com.twitter.bouncer.templates.thriftscala.TemplateData
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BouncerRequestBuilderTest extends ServiceTestBase {

  protected val bouncerBuilder = BouncerRequestBuilder()

  test("test template data builder") {
    bouncerBuilder
      .buildTemplateData(
        interpolatedBouncerSettings = bouncerBuilder.inputInterpolator.interpolateInputIntoSettings(
          bouncerSettings = testBouncerSettings,
          input = testBouncerInput
        )
      ) mustBe Some(
      TemplateData(
        redirectUrl = Some("/home"),
        tslaAuthChallengeData =
          Some(TSLAAuthChallengeData(token = "token", tokenKind = 10, authEventType = None))))
  }

  test("test bouncer builder") {
    bouncerBuilder
      .bouncerRequest(
        bouncerSettings = Some(testBouncerSettings),
        input = testBouncerInput
      ) mustBe Some(expectedBouncerRequest)
  }

}
