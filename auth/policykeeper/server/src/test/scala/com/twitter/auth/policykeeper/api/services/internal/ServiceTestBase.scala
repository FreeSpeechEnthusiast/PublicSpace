package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.RouteTag
import com.twitter.auth.policykeeper.thriftscala.BouncerRequest
import com.twitter.auth.policykeeper.thriftscala.BouncerSettings
import com.twitter.auth.policykeeper.thriftscala.BouncerTargetSettings
import com.twitter.auth.policykeeper.thriftscala.DeepStringMap
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.Rule
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.bouncer.templates.thriftscala.TSLAAuthChallengeData
import com.twitter.bouncer.templates.thriftscala.Tag
import com.twitter.bouncer.templates.thriftscala.TemplateData
import com.twitter.bouncer.templates.thriftscala.TemplateId
import com.twitter.bouncer.thriftscala.Bounce
import com.twitter.bouncer.thriftscala.BounceExperience
import com.twitter.bouncer.thriftscala.SessionTarget
import com.twitter.bouncer.thriftscala.Target
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.logging.Logger
import com.twitter.servo.util.MemoizingStatsReceiver
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
trait ServiceTestBase
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter
    with MockitoSugar {

  protected val statsReceiver = new InMemoryStatsReceiver
  protected val advancedStatsReceiver = new MemoizingStatsReceiver(statsReceiver)
  protected val logger = Logger.get()
  protected val jsonLogger = JsonLogger(logger)

  protected val testPolicyId = "policy1"
  protected val testPolicy = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "exp",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0)
    ),
    name = "testPolicy",
    description = "",
    failClosed = Some(true)
  )

  protected val testAnotherPolicyId = "policy2"
  protected val testAnotherPolicy = Policy(
    policyId = testAnotherPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "exp",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0)
    ),
    name = "testAnotherPolicy",
    description = "",
    failClosed = Some(true),
  )

  protected def routeTagFromPolicyId(policyId: String): RouteTag = {
    RouteTag("policy_" + policyId)
  }

  protected val input = ExpressionInput(
    Map(
      ExpressionInputParameterName("clientApplicationId", "auth") -> ExpressionInputParameterValue(
        1L),
      ExpressionInputParameterName("clientApplicationId", "other") -> ExpressionInputParameterValue(
        2L),
      ExpressionInputParameterName("userId") -> ExpressionInputParameterValue(3L)
    ))

  protected val testBouncerSettings = BouncerSettings(
    location = Some("/account/access?feature=auth_challenge&session={{auth.sessionHash}}"),
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
          mapVal = Some(Map(
            "token" -> DeepStringMap(
              isMap = false,
              stringVal = Some("{{access_token.token}}"),
              mapVal = None),
            "tokenKind" -> DeepStringMap(
              isMap = false,
              stringVal = Some("{{access_token.tokenKind}}"),
              mapVal = None)
          ))
        )
      )),
    referringTags = Some(Set("TSLA", "MODULE")),
    target = BouncerTargetSettings(
      targetType = "session",
      userId = Some("{{userId}}"),
      sessionHash = Some("{{auth.sessionHash}}"),
      feature = Some("auth_challenge")
    )
  )

  protected val testBouncerInput = ExpressionInput(
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
    ))

  protected val testPolicyWithBouncerId = "policyWithBouncer"
  protected val testPolicyWithBouncer = Policy(
    policyId = testPolicyWithBouncerId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "exp",
        action = RuleAction(
          actionNeeded = true,
          apiErrorCode = None,
          bouncerSettings = Some(testBouncerSettings)),
        priority = 0)
    ),
    name = "testPolicyWithBouncer",
    description = "",
    failClosed = Some(true),
  )

  protected val expectedBouncerRequest = BouncerRequest(
    bounce = Bounce(
      experience = Some(BounceExperience.FullOptional),
      location = Some("/account/access?feature=auth_challenge&session=sessionHash"),
      errorMessage = None,
      deeplink = None
    ),
    templateIds = Seq(TemplateId("module_auth_challenge")),
    templateData = Some(
      TemplateData(
        redirectUrl = Some("/home"),
        tslaAuthChallengeData =
          Some(TSLAAuthChallengeData(token = "token", tokenKind = 10, authEventType = None)))),
    referringTags = Some(Set(Tag.Tsla, Tag.Module)),
    target = Target.Session(
      SessionTarget(userId = 3L, sessionHash = "sessionHash", feature = Some("auth_challenge")))
  )

}
