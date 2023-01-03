package com.twitter.auth.urls

import com.twitter.finagle.stats.InMemoryStatsReceiver
import org.apache.commons.validator.routines.UrlValidator
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite
import org.scalatest.MustMatchers
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ClientApplicationUriValidatorSpec
    extends FunSuite
    with OneInstancePerTest
    with MockitoSugar
    with MustMatchers
    with BeforeAndAfter {

  protected val statsReceiver = new InMemoryStatsReceiver

  val ClientApplicationId = 1L

  before {
    statsReceiver.clear()
  }

  val urlValidator = new CustomProtocolClientApplicationUrlValidator(
    // OAuth2 redirect URI must NOT include any fragment component: AUTHPLT-1728
    oauthUrlValidator = new OAuthUrlValidator(
      UrlValidator.ALLOW_ALL_SCHEMES + UrlValidator.NO_FRAGMENTS),
    statsReceiver = statsReceiver.scope("redirect_uri_validator")
  )

  test("custom protocols without path part should be allowed (see AUTHPLT-2309)") {
    urlValidator.parse("twitter://", Some(ClientApplicationId)) mustBe Some(
      ParsedUrl("twitter", "", -1, ""))
  }

  test("custom protocols with domain part should be allowed (see AUTHPLT-2309)") {
    urlValidator.parse("twitter://dom", Some(ClientApplicationId)) mustBe Some(
      ParsedUrl("twitter", "dom", -1, ""))
  }

  test("custom protocols with path part should be allowed (see AUTHPLT-2309)") {
    urlValidator.parse("twitter://dom/path", Some(ClientApplicationId)) mustBe Some(
      ParsedUrl("twitter", "dom", -1, "/path"))
  }

  test("denied protocols should not be allowed") {
    urlValidator.parse("ftp://dom/path", Some(ClientApplicationId)) mustBe None
  }

  test("standard protocols with path part should be allowed") {
    urlValidator.parse("https://dom/path", Some(ClientApplicationId)) mustBe Some(
      ParsedUrl("https", "dom", 443, "/path"))
  }

  test("standard protocols with custom port and path part should be allowed") {
    urlValidator.parse("https://dom:8443/path", Some(ClientApplicationId)) mustBe Some(
      ParsedUrl("https", "dom", 8443, "/path"))
  }

}
