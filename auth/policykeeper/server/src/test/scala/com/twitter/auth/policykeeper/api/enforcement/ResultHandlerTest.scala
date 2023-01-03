package com.twitter.auth.policykeeper.api.enforcement

import com.twitter.auth.policykeeper.thriftscala.BouncerRequest
import com.twitter.auth.policykeeper.thriftscala.Code
import com.twitter.auth.policykeeper.thriftscala.Result
import com.twitter.bouncer.templates.thriftscala.TSLAAuthChallengeData
import com.twitter.bouncer.templates.thriftscala.Tag
import com.twitter.bouncer.templates.thriftscala.Template
import com.twitter.bouncer.templates.thriftscala.TemplateData
import com.twitter.bouncer.templates.thriftscala.TemplateId
import com.twitter.bouncer.thriftscala.Bounce
import com.twitter.bouncer.thriftscala.BounceExperience
import com.twitter.bouncer.thriftscala.Enrollment
import com.twitter.bouncer.thriftscala.EnrollmentResult
import com.twitter.bouncer.thriftscala.InternalActor
import com.twitter.bouncer.thriftscala.InternalLookup
import com.twitter.bouncer.thriftscala.ModuleEnrollmentRequest
import com.twitter.bouncer.thriftscala.SessionTarget
import com.twitter.bouncer.thriftscala.SimpleService
import com.twitter.bouncer.thriftscala.Target
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finatra.api11.ApiError
import com.twitter.tfe.core.decider.TfeDecider
import com.twitter.tfe.core.bouncer.clients.BouncerClient
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers

@RunWith(classOf[JUnitRunner])
class ResultHandlerTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  before {
    stats.clear()
    request = Request("/").host("some_host")
  }

  private var request: Request = _
  protected val decider = mock[TfeDecider]
  protected val bouncerClient = mock[BouncerClient]
  protected val stats = new InMemoryStatsReceiver
  protected val testClientId = ClientId("test")
  protected val testSuccessEnrollment = Enrollment(
    actor = InternalActor.SimpleService(SimpleService(testClientId.name)),
    template = Template(id = TemplateId("id"), name = "name"),
    enrolledAtMsec = 0L
  )
  protected val testApiError = ApiError.InternalError

  protected val resultHandlingTool = new ResultHandler(
    decider = decider,
    bouncerClient = bouncerClient,
    stats = stats,
    clientId = testClientId)

  protected val bouncerRequest = BouncerRequest(
    bounce = Bounce(
      experience = Some(BounceExperience.FullOptional),
      location = Some("/account/access?feature=auth_challenge&session=sessionHash"),
      errorMessage = None,
      deeplink = None
    ),
    templateIds = Seq(TemplateId("module_auth_challenge")),
    templateData = Some(
      TemplateData(tslaAuthChallengeData =
        Some(TSLAAuthChallengeData(token = "token", tokenKind = 10, authEventType = None)))),
    referringTags = Some(Set(Tag.Tsla, Tag.Module)),
    target = Target.Session(
      SessionTarget(userId = 3L, sessionHash = "sessionHash", feature = Some("auth_challenge")))
  )

  test("test result handling with false result") {
    resultHandlingTool
      .handleResult(
        request = request,
        result = Result(
          policyExecutionCode = Code.False,
          apiErrorCode = None,
          bouncerRequest = None)) mustBe (None)
  }

  test("test result handling with http result") {
    val result = resultHandlingTool
      .handleResult(
        request = request,
        result = Result(
          policyExecutionCode = Code.True,
          apiErrorCode = Some(testApiError.code),
          bouncerRequest = None))
    result mustNot be(None)
    val response = Await.result(result.get)
    response.status mustBe testApiError.status
    response.contentString mustBe
      s"""{"errors":"{\\"message\\":\\"${testApiError.message}\\",\\"code\\":${testApiError.code}}"}""".stripMargin
  }

  test("test result handling with bouncer result") {
    when(
      bouncerClient.enrollInModules(
        lookup = InternalLookup(
          bouncerRequest.target,
          InternalActor.SimpleService(SimpleService(testClientId.name))
        ),
        request = ModuleEnrollmentRequest(
          templateIds = bouncerRequest.templateIds,
          templateData = bouncerRequest.templateData,
          referringTags = bouncerRequest.referringTags
        )
      )) thenReturn Future.value(EnrollmentResult.Enrolled(enrolled = testSuccessEnrollment))
    val result = resultHandlingTool
      .handleResult(
        request = request,
        result = Result(
          policyExecutionCode = Code.True,
          apiErrorCode = None,
          bouncerRequest = Some(bouncerRequest)
        )
      )
    result mustNot be(None)
    stats.counters(List("ResultHandlingUtils", "bouncer_enrollment_success")) mustEqual 1L
    val response = Await.result(result.get)
    response.status mustBe Status(403)
    response.contentString mustBe
      """{"errors":[{"code":326,"message":"To protect our users from spam and other malicious activity, this account is temporarily locked. Please log in to https://twitter.com to unlock your account.","sub_error_code":1,"bounce_location":"/account/access?feature=auth_challenge&session=sessionHash","bounce_deeplink":null}]}""".stripMargin
  }

}
