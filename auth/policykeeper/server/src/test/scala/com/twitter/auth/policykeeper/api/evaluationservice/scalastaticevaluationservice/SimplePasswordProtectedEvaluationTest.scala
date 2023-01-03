package com.twitter.auth.policykeeper.api.evaluationservice.scalastaticevaluationservice

import com.twitter.auth.policykeeper.api.context.LocalContext
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.Rule
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.conversions.DurationOps._
import com.twitter.tsla.authevents.thriftscala.AuthEvent
import com.twitter.tsla.authevents.thriftscala.AuthEventType.PasswordVerified
import com.twitter.util.Await
import com.twitter.util.Time
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import scala.collection.Set

@RunWith(classOf[JUnitRunner])
class SimplePasswordProtectedEvaluationTest extends EvaluationTestBase {

  protected val simplePasswordProtectedPolicyId = "simple_password_protected"
  protected val simplePasswordProtectedPolicy = Policy(
    policyId = simplePasswordProtectedPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "DoesntHaveAccessToken(access_token.hasAccessToken)",
        action = RuleAction(actionNeeded = true, apiErrorCode = Some(214)),
        fallbackAction = Some(RuleAction(actionNeeded = true, apiErrorCode = Some(214))),
        priority = 0
      ),
      Rule(
        expression =
          "TslaPasswordVerifiedTooLongTimeAgo(auth_events.lastPasswordVerifiedTimestampMs, static.i60)",
        action = RuleAction(actionNeeded = true, apiErrorCode = Some(403)),
        fallbackAction = Some(RuleAction(actionNeeded = true, apiErrorCode = Some(403))),
        priority = 1
      )
    ),
    name = simplePasswordProtectedPolicyId,
    description = "",
  )

  test("test simplePasswordProtectedPolicy (valid, successful, all presented)") {
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
              policy = simplePasswordProtectedPolicy,
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

  test("test simplePasswordProtectedPolicy (valid, unsuccessful, password never verified)") {
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
              policy = simplePasswordProtectedPolicy,
              routeInformation = None,
              customInput = None,
              authMetadata = Some(
                AuthMetadata(
                  hasAccessToken = true,
                  authEvents = None,
                  gizmoduckUserId = Some(1L),
                  token = Some("token"),
                  tokenKind = Some(1)
                ))
            )
        ).ruleAction mustBe Some(RuleAction(actionNeeded = true, Some(403)))
    }
  }

  test(
    "test simplePasswordProtectedPolicy (valid, unsuccessful, password verified too long time ago)") {
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
              policy = simplePasswordProtectedPolicy,
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
        ).ruleAction mustBe Some(RuleAction(actionNeeded = true, Some(403)))
    }
  }

  test("test simplePasswordProtectedPolicy (valid, unsuccessful, no access token)") {
    LocalContext.writeToLocalContexts(
      clientApplicationId = None,
      userId = None,
      sessionHash = None,
      authenticatedUserId = None
    ) {
      Await
        .result(
          evaluationService
            .execute(
              policy = simplePasswordProtectedPolicy,
              routeInformation = None,
              customInput = None,
              authMetadata = Some(AuthMetadata(hasAccessToken = false, authEvents = None))
            )
        ).ruleAction mustBe Some(RuleAction(actionNeeded = true, Some(214)))
    }
  }

}
