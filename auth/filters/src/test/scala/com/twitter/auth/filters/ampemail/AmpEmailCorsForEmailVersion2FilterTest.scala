package com.twitter.auth.filters.ampemail

import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.http.exceptions.ForbiddenException
import com.twitter.util.Await
import com.twitter.util.Future
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

@RunWith(classOf[JUnitRunner])
class AmpEmailCorsForEmailVersion2FilterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  protected val statsReceiver = new InMemoryStatsReceiver

  before {
    statsReceiver.clear()
  }

  private val nonAmpRequest = Request("/endpoint").host("api.twitter.com")
  private val ampRequest = Request("/endpoint")
    .host("api.twitter.com")
  ampRequest.headerMap.set("AMP-Email-Sender", "info@twitter.com")

  private val ampRequestNotAuthorized = Request("/endpoint")
    .host("api.twitter.com")
  ampRequestNotAuthorized.headerMap.set("AMP-Email-Sender", "info@unknown.com")

  private[this] def filter: AmpEmailCorsForEmailVersion2Filter[Request] =
    new AmpEmailCorsForEmailVersion2Filter(statsReceiver)

  val testService = new Service[Request, Response] {
    override def apply(request: Request): Future[Response] = Future.value(Response(Status.Ok))
  }

  private[this] def getResult(request: Request): Response =
    Await.result(filter.apply(request, testService))

  test("test filter with non amp request") {
    val response = getResult(nonAmpRequest)
    response.status mustBe Status(200)
    response.headerMap.size mustBe (0)
    statsReceiver.counters(
      List("AmpEmailCorsForEmailVersion2Filter", "amp_email_header_not_present")) mustEqual 1L
  }

  test("test filter with amp request") {
    val response = getResult(ampRequest)
    response.status mustBe Status(200)
    response.headerMap.size mustBe (1)
    response.headerMap.get("AMP-Email-Allow-Sender") mustBe (Some("info@twitter.com"))
    statsReceiver.counters(
      List("AmpEmailCorsForEmailVersion2Filter", "amp_email_header_present")) mustEqual 1L
    statsReceiver.counters(
      List("AmpEmailCorsForEmailVersion2Filter", "amp_email_header_sender_authorized")) mustEqual 1L
  }

  test("test filter with amp request (non authorized)") {
    intercept[ForbiddenException] {
      getResult(ampRequestNotAuthorized)
    }
    statsReceiver.counters(
      List("AmpEmailCorsForEmailVersion2Filter", "amp_email_header_present")) mustEqual 1L
    statsReceiver.counters(
      List(
        "AmpEmailCorsForEmailVersion2Filter",
        "amp_email_header_sender_not_authorized")) mustEqual 1L
  }
}
