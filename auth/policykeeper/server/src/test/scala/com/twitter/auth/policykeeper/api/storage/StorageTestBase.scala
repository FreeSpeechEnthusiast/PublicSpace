package com.twitter.auth.policykeeper.api.storage

import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.Rule
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.auth.policykeeper.thriftscala.BouncerSettings
import com.twitter.auth.policykeeper.thriftscala.BouncerTargetSettings
import com.twitter.auth.policykeeper.thriftscala.DeepStringMap
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.logging.Logger
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

@RunWith(classOf[JUnitRunner])
trait StorageTestBase
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  protected val statsReceiver = new InMemoryStatsReceiver
  protected val logger = Logger.get()

  protected val testPolicyId = "policy1"
  protected val testOtherPolicyId = "policy2"
  protected val testOneMorePolicyId = "policy4"
  protected val testMissingPolicyId = "policy3"
  protected val invalidPolicyId = "p o l i c y"

  protected val testPolicy = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "exp",
        action = RuleAction(actionNeeded = false, apiErrorCode = None, bouncerSettings = None),
        priority = 0,
        fallbackAction = None)
    ),
    name = "testPolicy",
    description = "",
    failClosed = Some(false)
  )

  protected val testBouncerPolicy = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "exp",
        action = RuleAction(
          actionNeeded = true,
          apiErrorCode = None,
          bouncerSettings = Some(
            BouncerSettings(
              location = Some("/account/access"),
              errorMessage = None,
              deepLink = None,
              experience = Some("FullOptional"),
              templateIds = Seq("module_auth_challenge"),
              templateMapping = Some(Map(
                "redirectUrl" -> DeepStringMap(
                  isMap = false,
                  stringVal = Some("{{input.redirect_after_verify}}"),
                  mapVal = None
                ),
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
                userId = Some("{{auth.userId}}"),
                sessionHash = Some("{{auth.sessionHash}}"),
                feature = Some("auth_challenge")
              )
            ))
        ),
        priority = 0,
        fallbackAction =
          Some(RuleAction(actionNeeded = true, apiErrorCode = Some(214), bouncerSettings = None)),
      )
    ),
    name = "testPolicy",
    description = "",
    failClosed = Some(false),
  )

  protected val testOtherPolicy = Policy(
    policyId = testOtherPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "otherExp",
        action = RuleAction(actionNeeded = false, apiErrorCode = None, bouncerSettings = None),
        priority = 0,
        fallbackAction = None)
    ),
    name = "testOtherPolicy",
    description = "",
    failClosed = Some(false),
  )

  protected val testOneMorePolicy = Policy(
    policyId = testOneMorePolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "oneMoreExp",
        action = RuleAction(actionNeeded = false, apiErrorCode = None, bouncerSettings = None),
        priority = 0,
        fallbackAction = None)
    ),
    name = "testOneMorePolicy",
    description = "",
    failClosed = Some(false),
  )

}
