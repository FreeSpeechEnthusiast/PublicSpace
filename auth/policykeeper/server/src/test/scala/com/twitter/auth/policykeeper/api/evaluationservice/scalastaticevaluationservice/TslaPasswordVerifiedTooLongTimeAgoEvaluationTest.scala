package com.twitter.auth.policykeeper.api.evaluationservice.scalastaticevaluationservice

import com.twitter.auth.policykeeper.api.context.LocalContext
import com.twitter.auth.policykeeper.api.evaluationengine.IncompleteInputException
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.Rule
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.tsla.authevents.thriftscala.AuthEvent
import com.twitter.tsla.authevents.thriftscala.AuthEventType.PasswordVerified
import com.twitter.util.Await
import com.twitter.util.Time
import scala.collection.Set
import com.twitter.conversions.DurationOps._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TslaPasswordVerifiedTooLongTimeAgoEvaluationTest extends EvaluationTestBase {

  protected val tslaPasswordVerifiedTooLongTimeAgoId = "tslaPasswordVerifiedTooLongTimeAgo"
  protected val tslaPasswordVerifiedTooLongTimeAgo = Policy(
    policyId = tslaPasswordVerifiedTooLongTimeAgoId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression =
          "TslaPasswordVerifiedTooLongTimeAgo(auth_events.lastPasswordVerifiedTimestampMs, static.i60)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "tslaPasswordVerifiedTooLongTimeAgo",
    description = "",
  )

  test("test ScalaStaticEvaluationService (valid, successful, all presented)") {
    LocalContext.writeToLocalContexts(
      clientApplicationId = None,
      userId = Some(1L),
      sessionHash = Some("sessionHash"),
      authenticatedUserId = None
    ) {
      Await
        .result(
          evaluationService
            .execute(
              policy = tslaPasswordVerifiedTooLongTimeAgo,
              routeInformation = None,
              customInput = None,
              authMetadata = Some(AuthMetadata(
                hasAccessToken = true,
                authEvents = Some(Seq(AuthEvent(PasswordVerified, Some(Time.now.inMilliseconds)))),
                gizmoduckUserId = Some(1L),
                token = Some("token"),
                tokenKind = Some(1)
              ))
            )
        ).ruleAction mustBe None
    }
  }

  test(
    "test ScalaStaticEvaluationService (valid, unsuccessful, password verified too long time ago)") {
    LocalContext.writeToLocalContexts(
      clientApplicationId = None,
      userId = Some(1L),
      sessionHash = Some("sessionHash"),
      authenticatedUserId = None
    ) {
      Await
        .result(
          evaluationService
            .execute(
              policy = tslaPasswordVerifiedTooLongTimeAgo,
              routeInformation = None,
              customInput = None,
              authMetadata = Some(AuthMetadata(
                hasAccessToken = true,
                authEvents =
                  Some(Seq(AuthEvent(PasswordVerified, Some((Time.now - 2.hour).inMilliseconds)))),
                gizmoduckUserId = Some(1L),
                token = Some("token"),
                tokenKind = Some(1)
              ))
            )
        ).ruleAction mustBe Some(RuleAction(false, None))
    }
  }

  test("test ScalaStaticEvaluationService (valid, unsuccessful, no access token)") {
    LocalContext.writeToLocalContexts(
      clientApplicationId = None,
      userId = None,
      sessionHash = None,
      authenticatedUserId = None
    ) {
      intercept[IncompleteInputException] {
        Await
          .result(
            evaluationService
              .execute(
                policy = tslaPasswordVerifiedTooLongTimeAgo,
                routeInformation = None,
                customInput = None,
                authMetadata = Some(AuthMetadata(hasAccessToken = false, authEvents = None))
              )
          )
      }
    }
  }

}
