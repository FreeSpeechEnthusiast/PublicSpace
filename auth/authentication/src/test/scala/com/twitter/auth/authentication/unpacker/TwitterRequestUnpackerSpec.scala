package com.twitter.auth.authentication.unpacker

import com.twitter.auth.authentication.models.ActAsUserParams
import com.twitter.auth.authentication.models.AuthRequest
import com.twitter.auth.authentication.models.BadRequestParams
import com.twitter.auth.authentication.models.GuestAuthRequestParams
import com.twitter.auth.authentication.models.OAuth1RequestParams
import com.twitter.auth.authentication.models.OAuth1ThreeLeggedRequestParams
import com.twitter.auth.authentication.models.OAuth1TwoLeggedRequestParams
import com.twitter.auth.authentication.models.OAuth2ClientCredentialRequestParams
import com.twitter.auth.authentication.models.OAuth2SessionRequestParams
import com.twitter.auth.authentication.models.SessionRequestParams
import com.twitter.auth.authentication.models.TiaAuthRequestParams
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode.AuthTypeHeaderMismatch
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode.ContributorsIndicatorUnexpected
import com.twitter.decider.Feature
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.joauth.OAuthParams.StandardOAuthParamsHelperImpl
import com.twitter.joauth.Unpacker.KeyValueCallback
import com.twitter.joauth.keyvalue.KeyValueHandler
import com.twitter.joauth.keyvalue.KeyValueHandler.UrlEncodingNormalizingKeyValueHandler
import com.twitter.joauth.Normalizer
import com.twitter.joauth.Request
import com.twitter.joauth.keyvalue.KeyValueParser.HeaderKeyValueParser
import com.twitter.joauth.keyvalue.KeyValueParser.QueryKeyValueParser
import com.twitter.util.Base64Long
import java.util
import java.util.Collections
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class TwitterRequestUnpackerSpec
    extends AnyFunSuite
    with MockitoSugar
    with Matchers
    with BeforeAndAfterEach {

  case class UnpackerHelper(allowFloatingPointTimestamps: Boolean)
      extends StandardOAuthParamsHelperImpl {

    override def parseTimestamp(s: String): java.lang.Long = {
      if (allowFloatingPointTimestamps) try {
        s.toDouble.toLong
      } catch {
        case _: Throwable => null
      }
      else {
        super.parseTimestamp(s)
      }
    }
  }

  private val statsReceiver = new InMemoryStatsReceiver
  private val allowFloatingPointTimestamps = true
  private val helper = UnpackerHelper(allowFloatingPointTimestamps)
  private val useNewRequestBodyParser = mock[Feature]

  object KeyValueCallback extends KeyValueCallback {
    def invoke(kvHandler: KeyValueHandler) = new UrlEncodingNormalizingKeyValueHandler(kvHandler)
  }

  when(useNewRequestBodyParser.isAvailable).thenReturn(true)

  val unpacker = new TwitterRequestUnpacker(
    helper,
    Normalizer.getStandardNormalizer,
    QueryKeyValueParser,
    HeaderKeyValueParser,
    KeyValueCallback,
    KeyValueCallback,
    KeyValueCallback,
    (_, _) => true,
    true,
    true,
    true,
    useNewRequestBodyParser,
    statsReceiver
  )

  // Act-As User Params
  private[this] val actAsUserId = 99L
  private[this] val actAsUserParamsHeader = Some(ActAsUserParams(None, Some(actAsUserId)))
  private[this] val actAsUserParamsCookie = Some(ActAsUserParams(Some(actAsUserId), None))
  private[this] val actAsUserParamsBothSame = Some(
    ActAsUserParams(Some(actAsUserId), Some(actAsUserId)))

  private[this] val encodedBearerToken = "AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xn" +
    "Zz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpXXX"
  private[this] val decodedBearerToken = "AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xn" +
    "Zz4puTs=1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpXXX"
  private[this] val clientAccessToken =
    "TXpvaE1NTkRNMFRoWDJSZ01mc0ttUUV0bWc1ellsakJ6cDZmdXBNQURDMXpkOjE2NDczNjQ1NzUyOTQ6MToxOmN0OjE"
  private[this] val sessionId = "sessionId456"
  private[this] val clientTokenCookie = "123-nonce-signature"
  private[this] val guestToken = "1"
  private[this] val transactionID = "12345"
  private[this] val token = "token"
  private[this] val consumerKey = "consumerKey"
  private[this] val nonce = "nonce"
  private[this] val signature = "signature"
  private[this] val method = "HMAC-SHA1"
  private[this] val userId = "1234"
  private[this] val invalidUserId = "userId"
  private[this] val clientAppId = "12345"
  private[this] val passbirdTokenSignature = "signature"
  private[this] val url = "http://twitter.com?"
  private[this] val path = "http://twitter.com"
  private[this] val verb = "GET"
  private[this] val body = "key1=value1&key2=value2&key3=value3&key4&key5="
  private[this] val urlEncoded = "application/x-www-form-urlencoded"

  private[this] val oauth1ThreeLeggedAuthHeader =
    "OAuth oauth_consumer_key=consumerKey, oauth_nonce=nonce, " +
      "oauth_signature=signature, oauth_signature_method=HMAC-SHA1, oauth_timestamp=123, " +
      "oauth_token=token, oauth_version=1.0"
  private[this] val oauth1TwoLeggedAuthHeader =
    "OAuth oauth_consumer_key=consumerKey, oauth_nonce=nonce, " +
      "oauth_signature=signature, oauth_signature_method=HMAC-SHA1, oauth_timestamp=123, oauth_version=1.0"
  private[this] val oauth2AuthHeader = "Bearer " + encodedBearerToken
  private[this] val oauth2ClientCredentialAuthHeader = "Bearer " + clientAccessToken

  private[this] val cookieMap = Option(
    Map("auth_token" -> sessionId, "client_token" -> clientTokenCookie))
  private[this] val aauCookieMap = Option(Map("aa_u" -> Base64Long.toBase64(actAsUserId)))
  private[this] val oauth2SessionCookieMap = Option(
    Map("auth_token" -> sessionId, "aa_u" -> Base64Long.toBase64(actAsUserId)))
  private[this] val buf: util.ArrayList[Request.Pair] = new util.ArrayList[Request.Pair]
  buf.add(new Request.Pair("key1", "value1"))
  buf.add(new Request.Pair("key2", "value2"))
  buf.add(new Request.Pair("key3", "value3"))
  buf.add(new Request.Pair("key4", ""))
  buf.add(new Request.Pair("key5", ""))

  private[this] val otherParams = Map(
    "X-TFE-Requested-At" -> "1555227062063",
    "x-transport-ssl-protocol" -> "TLSv1.2",
    "x-tsa-host" -> "tsabox",
    "X-TFE-Processed-By-TFE" -> "true",
    "x-transport-cert-serial" -> "01",
    "Cookie" -> "",
    "user-agent" -> "twurl2/0.0 finagle-http/0.0",
    "Host" -> "api.twitter.com",
    "x-http2-stream-id" -> "1",
    "X-TFE-Canonical-Host" -> "api.twitter.com",
    "x-forwarded-proto" -> "https",
    "X-TFE-Port" -> "443",
    "X-TFE-Transaction-ID" -> transactionID
  )

  private[this] val guestAuthParams = Map(
    "X-TFE-Requested-At" -> "1555227062063",
    "x-transport-ssl-protocol" -> "TLSv1.2",
    "x-tsa-host" -> "tsabox",
    "X-Guest-Token" -> guestToken,
    "Host" -> "api.twitter.com",
    "x-forwarded-proto" -> "https",
    "X-TFE-Port" -> "443",
    "X-TFE-Transaction-ID" -> transactionID
  )

  private[this] val tiaAuthParams = Map(
    "X-TFE-Transaction-ID" -> transactionID,
    "Host" -> "api.twitter.com",
    "x-forwarded-proto" -> "https",
    "X-TFE-Port" -> "443",
    "X-TFE-User-Assertion-Signature" -> passbirdTokenSignature,
    "X-TFE-Client-Application-ID" -> clientAppId
  )
  private[this] val authTypeHeader = "X-Twitter-Auth-Type" -> "oauth2Session"

  private[this] val oauth1ThreeLeggedRequest = AuthRequest(
    headerParams =
      otherParams + ("authorization" -> oauth1ThreeLeggedAuthHeader) + ("Content-Type" -> urlEncoded),
    url = Some(url),
    body = Some(body),
    method = Some(verb))
  private[this] val oauth1TwoLeggedRequest = AuthRequest(
    headerParams =
      otherParams + ("authorization" -> oauth1TwoLeggedAuthHeader) + ("Content-Type" -> urlEncoded),
    body = Some(body),
    url = Some(url),
    method = Some(verb))
  private[this] val oauth2SessionRequest = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth2AuthHeader),
    cookies = cookieMap,
    url = Some(url))
  private[this] val oauth2ClientCredentialRequest = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth2ClientCredentialAuthHeader),
    url = Some(url))
  private[this] val dummyRequest =
    AuthRequest(headerParams = Map("X-TFE-Transaction-ID" -> transactionID), url = Some(url))

  private[this] val oauth1ThreeLeggedRequestAauHeader = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth1ThreeLeggedAuthHeader)
      + ("X-Act-As-User-Id" -> actAsUserId.toString),
    url = Some(url),
    method = Some(verb))
  private[this] val oauth1ThreeLeggedRequestAauCookie = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth1ThreeLeggedAuthHeader),
    cookies = aauCookieMap,
    url = Some(url),
    method = Some(verb))
  private[this] val oauth1ThreeLeggedRequestAauBothSame = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth1ThreeLeggedAuthHeader)
      + ("X-Act-As-User-Id" -> actAsUserId.toString),
    cookies = aauCookieMap,
    url = Some(url),
    method = Some(verb)
  )
  private[this] val oauth1ThreeLeggedRequestAauBothDifferent = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth1ThreeLeggedAuthHeader)
      + ("X-Act-As-User-Id" -> "22"),
    cookies = aauCookieMap,
    url = Some(url),
    method = Some(verb)
  )

  private[this] val oauth1TwoLeggedRequestAauHeader = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth1TwoLeggedAuthHeader)
      + ("X-Act-As-User-Id" -> actAsUserId.toString),
    url = Some(url),
    method = Some(verb))
  private[this] val oauth1TwoLeggedRequestAauCookie = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth1TwoLeggedAuthHeader),
    cookies = aauCookieMap,
    url = Some(url),
    method = Some(verb))
  private[this] val oauth1TwoLeggedRequestAauBothSame = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth1TwoLeggedAuthHeader)
      + ("X-Act-As-User-Id" -> actAsUserId.toString),
    cookies = aauCookieMap,
    url = Some(url),
    method = Some(verb)
  )
  private[this] val oauth1TwoLeggedRequestAauBothDifferent = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth1TwoLeggedAuthHeader)
      + ("X-Act-As-User-Id" -> "22"),
    cookies = aauCookieMap,
    url = Some(url),
    method = Some(verb))

  private[this] val oauth2SessionRequestAauHeader = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth2AuthHeader)
      + ("X-Act-As-User-Id" -> actAsUserId.toString),
    cookies = cookieMap,
    url = Some(url))
  private[this] val oauth2SessionRequestAauCookie = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth2AuthHeader),
    cookies = oauth2SessionCookieMap,
    url = Some(url))
  private[this] val oauth2SessionRequestAauBothSame = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth2AuthHeader)
      + ("X-Act-As-User-Id" -> actAsUserId.toString),
    cookies = oauth2SessionCookieMap,
    url = Some(url))
  private[this] val oauth2SessionRequestAauBothDifferent = AuthRequest(
    headerParams = otherParams + ("authorization" -> oauth2AuthHeader)
      + ("X-Act-As-User-Id" -> "22"),
    cookies = oauth2SessionCookieMap,
    url = Some(url))

  private[this] val sessionRequest =
    AuthRequest(otherParams, cookieMap, url = Some("/oauth/authenticate"))
  private[this] val sessionRequestWithCallback =
    AuthRequest(otherParams, cookieMap, url = Some("/oauth/authenticate?callback=1#"))
  private[this] val guestAuthRequest = AuthRequest(
    headerParams = guestAuthParams + ("authorization" -> oauth2AuthHeader),
    url = Some(url))
  private[this] val guestAuthRequestWithSessionCookie = AuthRequest(
    headerParams = guestAuthParams + ("authorization" -> oauth2AuthHeader),
    cookies = oauth2SessionCookieMap,
    url = Some(url))
  private[this] val validTiaAuthRequest =
    AuthRequest(headerParams = tiaAuthParams + ("X-TFE-User-ID" -> userId), url = Some(url))
  private[this] val invalidTiaAuthRequest =
    AuthRequest(headerParams = tiaAuthParams + ("X-TFE-User-ID" -> invalidUserId), url = Some(url))

  private[this] val oauth1ThreeLeggedRequestAuthTypeHeaderMismatch = AuthRequest(
    headerParams = otherParams +
      ("authorization" -> oauth1ThreeLeggedAuthHeader) + authTypeHeader,
    url = Some(url),
    method = Some(verb))
  private[this] val oauth1TwoLeggedRequestAuthTypeHeaderMismatch = AuthRequest(
    headerParams = otherParams +
      ("authorization" -> oauth1TwoLeggedAuthHeader) + authTypeHeader,
    url = Some(url),
    method = Some(verb))
  private[this] val guestAuthRequestAuthAuthTypeHeaderMismatch = AuthRequest(
    headerParams = guestAuthParams +
      ("authorization" -> oauth2AuthHeader) + authTypeHeader,
    url = Some(url),
    method = Some(verb))

  //OAuth1 Three Legged test cases
  test("unpack OAuth1 Three Legged Requests") {
    val requestParams = unpacker.fromRequest(oauth1ThreeLeggedRequest)
    when(useNewRequestBodyParser.isAvailable).thenReturn(true)

    requestParams match {
      case Some(p: OAuth1RequestParams) =>
        p.passportId mustBe transactionID
        p.actAsUserParams mustBe None
        p.oauthParams.token mustBe token
        p.oauthParams.consumerKey mustBe consumerKey
        p.oauthParams.nonce mustBe nonce
        p.oauthParams.timestampSecs mustBe 123L
        p.oauthParams.timestampStr mustBe "123"
        p.oauthParams.signature mustBe signature
        p.oauthParams.signatureMethod mustBe method
        p.oauthParams.version mustBe "1.0"
        p.otherParams mustBe Some(buf)
        p.scheme mustBe Option("https")
        p.host mustBe Option("api.twitter.com")
        p.port mustBe Option(443)
        p.verb mustBe Some(verb)
        p.path mustBe path
        p.ip mustBe None
        p.twitterClient mustBe None
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test("unpack OAuth1 Three Legged Requests with act as user header") {
    val requestParams = unpacker.fromRequest(oauth1ThreeLeggedRequestAauHeader)
    requestParams match {
      case Some(p: OAuth1RequestParams) =>
        p.passportId mustBe transactionID
        p.actAsUserParams mustBe actAsUserParamsHeader
        p.oauthParams.token mustBe token
        p.oauthParams.consumerKey mustBe consumerKey
        p.oauthParams.nonce mustBe nonce
        p.oauthParams.timestampSecs mustBe 123L
        p.oauthParams.timestampStr mustBe "123"
        p.oauthParams.signature mustBe signature
        p.oauthParams.signatureMethod mustBe method
        p.oauthParams.version mustBe "1.0"
        p.otherParams mustBe Option(Collections.emptyList[Request.Pair])
        p.scheme mustBe Option("https")
        p.host mustBe Option("api.twitter.com")
        p.port mustBe Option(443)
        p.verb mustBe Some(verb)
        p.path mustBe path
        p.ip mustBe None
        p.twitterClient mustBe None
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test("unpack OAuth1 Three Legged Requests with act as user cookie") {
    val requestParams = unpacker.fromRequest(oauth1ThreeLeggedRequestAauCookie)
    requestParams match {
      case Some(p: BadRequestParams) =>
        p.passportId mustBe transactionID
        p.authResult mustBe ContributorsIndicatorUnexpected
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test("unpack OAuth1 Three Legged Requests with act as user header and cookie match each other") {
    val requestParams = unpacker.fromRequest(oauth1ThreeLeggedRequestAauBothSame)
    requestParams match {
      case Some(p: OAuth1ThreeLeggedRequestParams) =>
        p.passportId mustBe transactionID
        p.actAsUserParams mustBe actAsUserParamsBothSame
        p.oauthParams.token mustBe token
        p.oauthParams.consumerKey mustBe consumerKey
        p.oauthParams.nonce mustBe nonce
        p.oauthParams.timestampSecs mustBe 123L
        p.oauthParams.timestampStr mustBe "123"
        p.oauthParams.signature mustBe signature
        p.oauthParams.signatureMethod mustBe method
        p.oauthParams.version mustBe "1.0"
        p.otherParams mustBe Option(Collections.emptyList[Request.Pair])
        p.scheme mustBe Option("https")
        p.host mustBe Option("api.twitter.com")
        p.port mustBe Option(443)
        p.verb mustBe Some(verb)
        p.path mustBe path
        p.ip mustBe None
        p.twitterClient mustBe None
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test(
    "unpack OAuth1 Three Legged Requests with act as user header and cookie mismatch each other") {
    val requestParams = unpacker.fromRequest(oauth1ThreeLeggedRequestAauBothDifferent)
    requestParams match {
      case Some(p: OAuth1ThreeLeggedRequestParams) =>
        p.passportId mustBe transactionID
        p.actAsUserParams mustBe None
        p.oauthParams.token mustBe token
        p.oauthParams.consumerKey mustBe consumerKey
        p.oauthParams.nonce mustBe nonce
        p.oauthParams.timestampSecs mustBe 123L
        p.oauthParams.timestampStr mustBe "123"
        p.oauthParams.signature mustBe signature
        p.oauthParams.signatureMethod mustBe method
        p.oauthParams.version mustBe "1.0"
        p.otherParams mustBe Option(Collections.emptyList[Request.Pair])
        p.scheme mustBe Option("https")
        p.host mustBe Option("api.twitter.com")
        p.port mustBe Option(443)
        p.verb mustBe Some(verb)
        p.path mustBe path
        p.ip mustBe None
        p.twitterClient mustBe None
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  //OAuth1 Two Legged test cases
  test("unpack OAuth1 Two Legged Requests") {
    val requestParams = unpacker.fromRequest(oauth1TwoLeggedRequest)

    requestParams match {
      case Some(p: OAuth1TwoLeggedRequestParams) =>
        p.passportId mustBe transactionID
        p.actAsUserParams mustBe None
        p.oauthParams.token mustBe null
        p.oauthParams.consumerKey mustBe consumerKey
        p.oauthParams.nonce mustBe nonce
        p.oauthParams.timestampSecs mustBe 123L
        p.oauthParams.timestampStr mustBe "123"
        p.oauthParams.signature mustBe signature
        p.oauthParams.signatureMethod mustBe method
        p.oauthParams.version mustBe "1.0"
        p.otherParams mustBe Some(buf)
        p.scheme mustBe Option("https")
        p.host mustBe Option("api.twitter.com")
        p.port mustBe Option(443)
        p.verb mustBe Some(verb)
        p.path mustBe path
        p.ip mustBe None
        p.twitterClient mustBe None
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test("unpack OAuth1 Two Legged Requests with act as user header") {
    val requestParams = unpacker.fromRequest(oauth1TwoLeggedRequestAauHeader)
    requestParams match {
      case Some(p: OAuth1TwoLeggedRequestParams) =>
        p.passportId mustBe transactionID
        p.actAsUserParams mustBe actAsUserParamsHeader
        p.oauthParams.token mustBe null
        p.oauthParams.consumerKey mustBe consumerKey
        p.oauthParams.nonce mustBe nonce
        p.oauthParams.timestampSecs mustBe 123L
        p.oauthParams.timestampStr mustBe "123"
        p.oauthParams.signature mustBe signature
        p.oauthParams.signatureMethod mustBe method
        p.oauthParams.version mustBe "1.0"
        p.otherParams mustBe Option(Collections.emptyList[Request.Pair])
        p.scheme mustBe Option("https")
        p.host mustBe Option("api.twitter.com")
        p.port mustBe Option(443)
        p.verb mustBe Some(verb)
        p.path mustBe path
        p.ip mustBe None
        p.twitterClient mustBe None
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test("unpack OAuth1 Two Legged Requests with act as user cookie") {
    val requestParams = unpacker.fromRequest(oauth1TwoLeggedRequestAauCookie)
    requestParams match {
      case Some(p: OAuth1TwoLeggedRequestParams) =>
        p.passportId mustBe transactionID
        p.actAsUserParams mustBe actAsUserParamsCookie
        p.oauthParams.token mustBe null
        p.oauthParams.consumerKey mustBe consumerKey
        p.oauthParams.nonce mustBe nonce
        p.oauthParams.timestampSecs mustBe 123L
        p.oauthParams.timestampStr mustBe "123"
        p.oauthParams.signature mustBe signature
        p.oauthParams.signatureMethod mustBe method
        p.oauthParams.version mustBe "1.0"
        p.otherParams mustBe Option(Collections.emptyList[Request.Pair])
        p.scheme mustBe Option("https")
        p.host mustBe Option("api.twitter.com")
        p.port mustBe Option(443)
        p.verb mustBe Some(verb)
        p.path mustBe path
        p.ip mustBe None
        p.twitterClient mustBe None
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test("unpack OAuth1 Two Legged Requests with act as user header and cookie match each other") {
    val requestParams = unpacker.fromRequest(oauth1TwoLeggedRequestAauBothSame)
    requestParams match {
      case Some(p: OAuth1TwoLeggedRequestParams) =>
        p.passportId mustBe transactionID
        p.actAsUserParams mustBe actAsUserParamsBothSame
        p.oauthParams.token mustBe null
        p.oauthParams.consumerKey mustBe consumerKey
        p.oauthParams.nonce mustBe nonce
        p.oauthParams.timestampSecs mustBe 123L
        p.oauthParams.timestampStr mustBe "123"
        p.oauthParams.signature mustBe signature
        p.oauthParams.signatureMethod mustBe method
        p.oauthParams.version mustBe "1.0"
        p.otherParams mustBe Option(Collections.emptyList[Request.Pair])
        p.scheme mustBe Option("https")
        p.host mustBe Option("api.twitter.com")
        p.port mustBe Option(443)
        p.verb mustBe Some(verb)
        p.path mustBe path
        p.ip mustBe None
        p.twitterClient mustBe None
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test("unpack OAuth1 Two Legged Requests with act as user header and cookie mismatch each other") {
    val requestParams = unpacker.fromRequest(oauth1TwoLeggedRequestAauBothDifferent)
    requestParams match {
      case Some(p: OAuth1TwoLeggedRequestParams) =>
        p.passportId mustBe transactionID
        p.actAsUserParams mustBe None
        p.oauthParams.token mustBe null
        p.oauthParams.consumerKey mustBe consumerKey
        p.oauthParams.nonce mustBe nonce
        p.oauthParams.timestampSecs mustBe 123L
        p.oauthParams.timestampStr mustBe "123"
        p.oauthParams.signature mustBe signature
        p.oauthParams.signatureMethod mustBe method
        p.oauthParams.version mustBe "1.0"
        p.otherParams mustBe Option(Collections.emptyList[Request.Pair])
        p.scheme mustBe Option("https")
        p.host mustBe Option("api.twitter.com")
        p.port mustBe Option(443)
        p.verb mustBe Some(verb)
        p.path mustBe path
        p.ip mustBe None
        p.twitterClient mustBe None
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test("unpack OAuth2ClientCredential request") {
    val requestParams = unpacker.fromRequest(oauth2ClientCredentialRequest)
    requestParams mustBe Some(
      OAuth2ClientCredentialRequestParams(
        passportId = transactionID,
        clientAccessToken = clientAccessToken,
        otherParams = Map[String, Seq[String]](),
        path = path,
        ip = None,
        twitterClient = None
      )
    )
  }

  //OAuth2 session test cases
  test("unpack OAuth2 session request") {
    val requestParams = unpacker.fromRequest(oauth2SessionRequest)
    requestParams mustBe Some(
      OAuth2SessionRequestParams(
        passportId = transactionID,
        actAsUserParams = None,
        bearerToken = decodedBearerToken,
        session = sessionId,
        otherParams = Map[String, Seq[String]](),
        path = path,
        ip = None,
        twitterClient = None,
        guestToken = None
      ))
  }

  test("unpack OAuth2 session request with act as user header") {
    val requestParams = unpacker.fromRequest(oauth2SessionRequestAauHeader)
    requestParams mustBe Some(
      OAuth2SessionRequestParams(
        passportId = transactionID,
        actAsUserParams = actAsUserParamsHeader,
        bearerToken = decodedBearerToken,
        session = sessionId,
        otherParams = Map[String, Seq[String]](),
        path = path,
        ip = None,
        twitterClient = None,
        guestToken = None
      ))
  }

  test("unpack OAuth2 session request with act as user cookie") {
    val requestParams = unpacker.fromRequest(oauth2SessionRequestAauCookie)
    requestParams mustBe Some(
      OAuth2SessionRequestParams(
        passportId = transactionID,
        actAsUserParams = actAsUserParamsCookie,
        bearerToken = decodedBearerToken,
        session = sessionId,
        otherParams = Map[String, Seq[String]](),
        path = path,
        ip = None,
        twitterClient = None,
        guestToken = None
      ))
  }

  test("unpack OAuth2 session request with act as user header and cookie match each other") {
    val requestParams = unpacker.fromRequest(oauth2SessionRequestAauBothSame)
    requestParams mustBe Some(
      OAuth2SessionRequestParams(
        passportId = transactionID,
        actAsUserParams = actAsUserParamsBothSame,
        bearerToken = decodedBearerToken,
        session = sessionId,
        otherParams = Map[String, Seq[String]](),
        path = path,
        ip = None,
        twitterClient = None,
        guestToken = None
      ))
  }

  test("unpack OAuth2 session request with act as user header and cookie mismatch each other") {
    val requestParams = unpacker.fromRequest(oauth2SessionRequestAauBothDifferent)
    requestParams mustBe Some(
      OAuth2SessionRequestParams(
        passportId = transactionID,
        actAsUserParams = None,
        bearerToken = decodedBearerToken,
        session = sessionId,
        otherParams = Map[String, Seq[String]](),
        path = path,
        ip = None,
        twitterClient = None,
        guestToken = None
      ))
  }

  //Session auth test case
  test("unpack session request") {
    val requestParams = unpacker.fromRequest(sessionRequest)
    requestParams mustBe Some(
      SessionRequestParams(
        passportId = transactionID,
        actAsUserParams = None,
        authToken = sessionId,
        clientTokenCookie = Some(clientTokenCookie),
        otherParams = Map[String, Seq[String]]()
      ))
  }

  test("unpack session request with callback") {
    val requestParams = unpacker.fromRequest(sessionRequestWithCallback)
    requestParams mustBe Some(
      SessionRequestParams(
        passportId = transactionID,
        actAsUserParams = None,
        authToken = sessionId,
        clientTokenCookie = Some(clientTokenCookie),
        otherParams = Map("callback" -> Seq("1"))
      ))
  }

  //Guest auth test case
  test("unpack guest auth request") {
    val requestParams = unpacker.fromRequest(guestAuthRequest)
    requestParams mustBe Some(
      GuestAuthRequestParams(
        passportId = transactionID,
        bearerToken = decodedBearerToken,
        otherParams = Map[String, Seq[String]](),
        path = path,
        ip = None,
        twitterClient = None,
        guestToken = Some(1L)
      ))
  }

  test("unpack guest auth request with session cookie") {
    val requestParams = unpacker.fromRequest(guestAuthRequestWithSessionCookie)
    requestParams mustBe Some(
      OAuth2SessionRequestParams(
        passportId = transactionID,
        actAsUserParams = actAsUserParamsCookie,
        bearerToken = decodedBearerToken,
        session = sessionId,
        otherParams = Map[String, Seq[String]](),
        path = path,
        ip = None,
        twitterClient = None,
        guestToken = Some(1L)
      ))
  }

  //Tia auth test case
  test("unpack tia auth request with valid user ID") {
    val requestParams = unpacker.fromRequest(validTiaAuthRequest)
    requestParams mustBe Some(
      TiaAuthRequestParams(
        passportId = transactionID,
        userId = Some(userId.toLong),
        signature = passbirdTokenSignature,
        clientAppId = Some(clientAppId),
        additionalFields = Seq[String](transactionID)
      ))
  }

  test("unpack tia auth request with invalid user ID") {
    val requestParams = unpacker.fromRequest(invalidTiaAuthRequest)
    requestParams mustBe Some(
      TiaAuthRequestParams(
        passportId = transactionID,
        userId = None,
        signature = passbirdTokenSignature,
        clientAppId = Some(clientAppId),
        additionalFields = Seq[String](transactionID)
      ))
  }

  // test cases for oauth2session auth header mismatch
  test("unpack OAuth1 Three Legged Request with auth type header mismatch") {
    val requestParams = unpacker.fromRequest(oauth1ThreeLeggedRequestAuthTypeHeaderMismatch)
    requestParams match {
      case Some(p: BadRequestParams) =>
        p.passportId mustBe transactionID
        p.authResult mustBe AuthTypeHeaderMismatch
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test("unpack OAuth1 Two Legged Request with auth type header mismatch") {
    val requestParams = unpacker.fromRequest(oauth1TwoLeggedRequestAuthTypeHeaderMismatch)
    requestParams match {
      case Some(p: BadRequestParams) =>
        p.passportId mustBe transactionID
        p.authResult mustBe AuthTypeHeaderMismatch
      case _ => throw new RuntimeException("Should not be here")
    }
  }

  test("unpack guest request with auth type header mismatch") {
    val requestParams = unpacker.fromRequest(guestAuthRequestAuthAuthTypeHeaderMismatch)
    requestParams match {
      case Some(p: BadRequestParams) =>
        p.passportId mustBe transactionID
        p.authResult mustBe AuthTypeHeaderMismatch
      case _ => throw new RuntimeException("Should not be here")
    }
  }

}
