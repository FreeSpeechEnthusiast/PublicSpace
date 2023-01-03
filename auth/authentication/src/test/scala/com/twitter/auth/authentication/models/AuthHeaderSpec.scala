package com.twitter.auth.authentication.models

import com.twitter.finagle.stats.{InMemoryStatsReceiver, LoadedStatsReceiver}
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class AuthHeaderSpec extends AnyFunSuite with MockitoSugar with Matchers with BeforeAndAfterEach {

  import com.twitter.auth.authentication.CommonFixtures._

  var statsReceiver = new InMemoryStatsReceiver
  LoadedStatsReceiver.self = statsReceiver

  override protected def beforeEach(): Unit = {
    statsReceiver.clear()
  }

  test("AuthHeader - empty strings and OAuth1") {
    // empty header string
    AuthHeader(null) mustBe None
    statsReceiver.counters(Seq("auth_header_parser", "no_auth_header")) mustEqual 1

    // badly formatted header
    AuthHeader(DummyHeader) mustBe None
    statsReceiver.counters(Seq("auth_header_parser", "invalid_auth_header")) mustEqual 1

    // unknown header
    AuthHeader(UnknownHeader) mustBe None
    statsReceiver.counters(Seq("auth_header_parser", "unknown_auth_header")) mustEqual 1

    // oauth1 header
    val oauth1Params = AuthHeader(Oauth1ThreeLeggedAuthHeader).get.generateOAuth1Params()
    statsReceiver.counters(Seq("auth_header_parser", "oauth1_auth_header")) mustEqual 1
    oauth1Params mustNot be(None)
    oauth1Params.consumerKey() mustBe ConsumerKey
    oauth1Params.nonce() mustBe Nonce
    oauth1Params.signature() mustBe EncodedSignature
    oauth1Params.signatureMethod() mustBe SignatureMethod
    oauth1Params.timestampStr() mustBe Timestamp
    oauth1Params.timestampSecs() mustBe 1555225314L
    oauth1Params.token() mustBe Token
    oauth1Params.version() mustBe Version

    // oauth1 missed headers
    val oauth1ParamsMissedParams = AuthHeader(Oauth1HeaderMissedParams).get.generateOAuth1Params()
    statsReceiver.counters(Seq("auth_header_parser", "oauth1_auth_header")) mustEqual 2
    oauth1ParamsMissedParams mustNot be(None)
    oauth1ParamsMissedParams mustNot be(None)
    oauth1ParamsMissedParams.consumerKey() mustBe ConsumerKey
    oauth1ParamsMissedParams.nonce() mustBe Nonce
    oauth1ParamsMissedParams.signatureMethod() mustBe SignatureMethod
    oauth1ParamsMissedParams.timestampStr() mustBe Timestamp
    oauth1ParamsMissedParams.timestampSecs() mustBe 1555225314L
    oauth1ParamsMissedParams.version() mustBe Version
    oauth1ParamsMissedParams.token() mustBe null
    oauth1ParamsMissedParams.signature() mustBe null

    // ensure requirements trigger an error
    assertThrows[java.lang.IllegalArgumentException] {
      AuthHeader(Oauth1ThreeLeggedAuthHeader).get.generateOAuth2Params()
    }
  }

  test("AuthHeader - OAuth2") {
    // oauth2 header
    AuthHeader(Oauth2AuthHeader).get.generateOAuth2Params() mustBe DecodedBearerToken
    statsReceiver.counters(Seq("auth_header_parser", "oauth2_auth_header")) mustEqual 1

    // ensure requirements trigger an error
    assertThrows[java.lang.IllegalArgumentException] {
      AuthHeader(Oauth2AuthHeader).get.generateOAuth1Params()
    }
  }
}
