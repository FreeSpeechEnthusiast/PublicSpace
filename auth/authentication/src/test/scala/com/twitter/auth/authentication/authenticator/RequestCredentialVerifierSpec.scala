package com.twitter.auth.authentication.authenticator

import com.twitter.auth.authentication.utils.CredentialVerifierUtils
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode._
import org.junit.runner.RunWith
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class RequestCredentialVerifierSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers {

  import com.twitter.auth.authentication.CommonFixtures._

  private[this] val clientApplication = TestClientApplication

  test("verify valid OAuth1 access token") {
    CredentialVerifierUtils.verifyOAuth1AccessToken(ValidOAuth1AccessToken) mustBe Ok
  }

  test("verify invalid OAuth1 access token") {
    CredentialVerifierUtils.verifyOAuth1AccessToken(InvalidOAuth1AccessToken) mustBe BadAccessToken
  }

  test("verify orphaned OAuth1 access token") {
    CredentialVerifierUtils.verifyOAuth1AccessToken(
      OrphanedOAuth1AccessToken) mustBe OrphanedAccessToken
  }

  test("verify valid OAuth2 app-only token") {
    CredentialVerifierUtils.verifyOAuth2AppOnlyToken(ValidOAuth2AppOnlyToken) mustBe Ok
  }

  test("verify invalid OAuth2 app-only token") {
    CredentialVerifierUtils.verifyOAuth2AppOnlyToken(
      InValidOAuth2AppOnlyToken) mustBe BadAccessToken
  }

  test("verify valid OAuth2 client access token") {
    CredentialVerifierUtils.verifyOAuth2ClientAccessToken(ValidOAuth2ClientAccessToken) mustBe Ok
  }

  test("verify invalid OAuth2 client access token") {
    CredentialVerifierUtils.verifyOAuth2ClientAccessToken(
      InValidOAuth2ClientAccessToken) mustBe BadClientAccessToken
  }

  test("verify OAuth1 three legged request with valid access token") {
    RequestCredentialVerifier.verifyOAuth1ThreeLeggedRequestWithAccessToken(
      ValidOAuth1AccessToken,
      clientApplication,
      Oauth1ThreeLeggedAuthParams) mustBe TimestampOutOfRange
  }

  test("verify OAuth1 three legged request with invalid access token") {
    RequestCredentialVerifier.verifyOAuth1ThreeLeggedRequestWithAccessToken(
      InvalidOAuth1AccessToken,
      clientApplication,
      Oauth1ThreeLeggedAuthParams) mustBe BadAccessToken
  }

  test("verify OAuth1 three legged request with orphaned access token") {
    RequestCredentialVerifier.verifyOAuth1ThreeLeggedRequestWithAccessToken(
      OrphanedOAuth1AccessToken,
      clientApplication,
      Oauth1ThreeLeggedAuthParams) mustBe OrphanedAccessToken
  }

  test("verify OAuth1 two legged request with valid access token") {
    RequestCredentialVerifier.verifyOAuth1ThreeLeggedRequestWithAccessToken(
      ValidOAuth1AccessToken,
      clientApplication,
      Oauth1TwoLeggedAuthParams) mustBe TimestampOutOfRange
  }
}
