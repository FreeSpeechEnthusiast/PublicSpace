package com.twitter.auth.policykeeper.api.dataproviderservice

import com.twitter.auth.policykeeper.api.context.LocalContext
import com.twitter.auth.policykeeper.api.dataproviders.DataProviderTimeoutException
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine.ScalaStaticExpressionParser
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.thriftscala.BouncerSettings
import com.twitter.auth.policykeeper.thriftscala.BouncerTargetSettings
import com.twitter.conversions.DurationOps._
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.Rule
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.logging.Logger
import com.twitter.servo.util.MemoizingStatsReceiver
import com.twitter.util.Await
import com.twitter.util.TimeoutException
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DataProviderServiceTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  protected val statsReceiver = new InMemoryStatsReceiver
  protected val perDataProviderStatsReceiver: MemoizingStatsReceiver =
    new MemoizingStatsReceiver(statsReceiver)
  protected val logger = Logger.get()
  protected val jsonLogger = JsonLogger(logger)

  before {
    statsReceiver.clear()
  }

  private val expressionParser = ScalaStaticExpressionParser()
  private val dataProviderService = DataProviderService(
    expressionParser,
    perDataProviderStatsReceiver = perDataProviderStatsReceiver,
    statsReceiver = statsReceiver,
    logger = jsonLogger)

  protected val testPolicyId = "policy1"
  protected val testPartialPolicy = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "CheckAuth(auth.userId, auth.clientApplicationId)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "testPolicy",
    description = "",
  )
  protected val testFullPolicy = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression =
          "CheckAuth(auth.userId, auth.clientApplicationId, auth.sessionHash, auth.authenticatedUserId)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "testPolicy",
    description = "",
  )

  protected val testFullPolicyWithBouncerId = "fullPolicyWithBouncer"
  protected val testFullPolicyWithBouncer = Policy(
    policyId = testFullPolicyWithBouncerId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression =
          "CheckAuth(auth.userId, auth.clientApplicationId, auth.sessionHash, auth.authenticatedUserId)",
        action = RuleAction(
          actionNeeded = true,
          apiErrorCode = None,
          bouncerSettings = Some(
            BouncerSettings(
              location =
                Some("/account/access?feature={{static.i60}}&session={{auth.sessionHash}}"),
              errorMessage = None,
              deepLink = None,
              experience = Some("FullOptional"),
              templateIds = Seq("module_auth_challenge"),
              target = BouncerTargetSettings(
                targetType = "session",
                userId = Some("{{auth.userId}}"),
                sessionHash = Some("{{auth.sessionHash}}"),
                feature = Some("{{static.i90}}"))
            ))
        ),
        priority = 0
      )
    ),
    name = testFullPolicyWithBouncerId,
    description = "",
  )
  protected val testUnknownFieldsPolicy = Policy(
    policyId = testPolicyId,
    decider = None,
    dataProviders = None,
    eligibilityCriteria = None,
    rules = Set(
      Rule(
        expression = "CheckAuth(auth.userId, something.else)",
        action = RuleAction(actionNeeded = false, apiErrorCode = None),
        priority = 0
      )
    ),
    name = "testPolicy",
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

  protected val testFaultyPolicyId = "faultyPolicyId"
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

  test("test data provider service with partially unavailable data (userId is None)") {
    LocalContext.writeToLocalContexts(Some(123L), None, Some("sss"), None, Some(888L)) {
      Await.result(
        dataProviderService.getData(
          policies = Seq(testPartialPolicy),
          routeInformation = None,
          authMetadata = None
        )) mustBe ExpressionInput(
        Map(
          ExpressionInputParameterName(
            "clientApplicationId",
            "auth") -> ExpressionInputParameterValue(123L)
        ))
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataReceived)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataReceived)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          dataProviderService.DataProviderIncompleteResult)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataProviderIncompleteResult)) mustEqual 1L
    }
  }

  test("test data provider service with faulty policy") {
    LocalContext.writeToLocalContexts(Some(123L), Some(456L), Some("sss"), None, Some(888L)) {
      intercept[Exception] {
        Await.result(
          dataProviderService.getData(
            policies = Seq(testFaultyPolicy),
            routeInformation = None,
            authMetadata = None
          ))
      }
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "FaultyDataProvider",
          dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataRequestFailed)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "FaultyDataProvider",
          dataProviderService.DataRequestFailed)) mustEqual 1L
    }
  }

  test("test data provider service with policy requiring partial data") {
    LocalContext.writeToLocalContexts(Some(123L), Some(456L), Some("sss"), None, Some(888L)) {
      Await.result(
        dataProviderService.getData(
          policies = Seq(testPartialPolicy),
          routeInformation = None,
          authMetadata = None
        )) mustBe ExpressionInput(
        Map(
          ExpressionInputParameterName(
            "clientApplicationId",
            "auth") -> ExpressionInputParameterValue(123L),
          ExpressionInputParameterName("userId", "auth") -> ExpressionInputParameterValue(456L),
        ))
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataReceived)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataReceived)) mustEqual 1L
    }
  }

  test("test data provider service with policy requiring all data") {
    LocalContext.writeToLocalContexts(Some(123L), Some(456L), Some("sss"), None, Some(888L)) {
      Await.result(
        dataProviderService.getData(
          policies = Seq(testFullPolicy),
          routeInformation = None,
          authMetadata = None
        )) mustBe ExpressionInput(
        Map(
          ExpressionInputParameterName(
            "clientApplicationId",
            "auth") -> ExpressionInputParameterValue(123L),
          ExpressionInputParameterName("sessionHash", "auth") -> ExpressionInputParameterValue(
            "sss"),
          ExpressionInputParameterName("userId", "auth") -> ExpressionInputParameterValue(456L),
          ExpressionInputParameterName(
            "authenticatedUserId",
            "auth") -> ExpressionInputParameterValue(888L)
        ))
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataReceived)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataReceived)) mustEqual 1L
    }
  }

  test("test data provider service with bouncing policy requiring all data") {
    LocalContext.writeToLocalContexts(Some(123L), Some(456L), Some("sss"), None, Some(888L)) {
      Await.result(
        dataProviderService.getData(
          policies = Seq(testFullPolicyWithBouncer),
          routeInformation = None,
          authMetadata = None
        )) mustBe ExpressionInput(
        Map(
          ExpressionInputParameterName(
            "clientApplicationId",
            "auth") -> ExpressionInputParameterValue(123L),
          ExpressionInputParameterName("sessionHash", "auth") -> ExpressionInputParameterValue(
            "sss"),
          ExpressionInputParameterName("userId", "auth") -> ExpressionInputParameterValue(456L),
          ExpressionInputParameterName(
            "authenticatedUserId",
            "auth") -> ExpressionInputParameterValue(888L),
          ExpressionInputParameterName("i60", "static") -> ExpressionInputParameterValue(60),
          ExpressionInputParameterName("i90", "static") -> ExpressionInputParameterValue(90)
        ))
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataRequested)) mustEqual 2L
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataReceived)) mustEqual 2L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataReceived)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "StaticDataProvider",
          dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "StaticDataProvider",
          dataProviderService.DataReceived)) mustEqual 1L
    }
  }

  test("test data provider service with slow policy with too small timeout") {
    intercept[TimeoutException] {
      Await.result(
        dataProviderService.getData(
          policies = Seq(testSlowPolicy),
          routeInformation = None,
          authMetadata = None
        ),
        10.milliseconds)
    }
    statsReceiver.counters(
      List(dataProviderService.Scope, dataProviderService.DataRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        dataProviderService.Scope,
        "SlowDataProvider",
        dataProviderService.DataRequested)) mustEqual 1L
  }

  test("test data provider service with slow policy with big enough timeout") {
    intercept[DataProviderTimeoutException] {
      Await.result(
        dataProviderService.getData(
          policies = Seq(testSlowPolicy),
          routeInformation = None,
          authMetadata = None
        ),
        2.seconds)
    }
    statsReceiver.counters(
      List(dataProviderService.Scope, dataProviderService.DataRequested)) mustEqual 1L
    statsReceiver.counters(
      List(
        dataProviderService.Scope,
        "SlowDataProvider",
        dataProviderService.DataRequested)) mustEqual 1L
    statsReceiver.counters(
      List(dataProviderService.Scope, dataProviderService.DataRequestTimeout)) mustEqual 1L
    statsReceiver.counters(
      List(
        dataProviderService.Scope,
        "SlowDataProvider",
        dataProviderService.DataRequestTimeout)) mustEqual 1L
  }

  test("test data provider service with unknown params policy") {
    LocalContext.writeToLocalContexts(Some(123L), Some(456L), Some("sss"), None, Some(888L)) {
      Await.result(
        dataProviderService.getData(
          policies = Seq(testUnknownFieldsPolicy),
          routeInformation = None,
          authMetadata = None
        )) mustBe ExpressionInput(
        Map(
          ExpressionInputParameterName("userId", "auth") -> ExpressionInputParameterValue(456L),
        ))
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataRequested)) mustEqual 1L
      statsReceiver.counters(
        List(dataProviderService.Scope, dataProviderService.DataReceived)) mustEqual 1L
      statsReceiver.counters(
        List(
          dataProviderService.Scope,
          "AuthDataProvider",
          dataProviderService.DataReceived)) mustEqual 1L
    }
  }

}
