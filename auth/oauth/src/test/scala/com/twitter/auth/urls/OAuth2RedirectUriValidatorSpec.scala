package com.twitter.auth.urls
import com.twitter.auth.models.ClientApplication
import com.twitter.auth.models.Organization
import com.twitter.auth.urls.UrlErrors.APP_WITHOUT_REDIRECT_URI
import com.twitter.auth.urls.UrlErrors.INVALID_URI_FORMAT
import com.twitter.auth.urls.UrlErrors.REDIRECT_URI_LOCKED
import com.twitter.finagle.stats.InMemoryStatsReceiver
import java.nio.ByteBuffer
import org.apache.commons.validator.routines.UrlValidator
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.MustMatchers
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.BeforeAndAfter

@RunWith(classOf[JUnitRunner])
class OAuth2RedirectUriValidatorSpec
    extends FunSuite
    with OneInstancePerTest
    with MockitoSugar
    with MustMatchers
    with BeforeAndAfter {

  protected val statsReceiver = new InMemoryStatsReceiver

  before {
    statsReceiver.clear()
  }

  val ClientApplicationId = 1L
  val TwitterIphoneAppId = 129032L
  val ClientAppIdStr = "1"
  val UserId = 1L
  val UserIdStr = "1"
  val Id = 1L
  val BatchSize = 1000
  val AppName = "bar"
  val ConsumerKey = "consumer_key"
  val Secret = "secret"
  val Description = Some("blahblahdescription")
  val Url = Some("http://foo.bar")
  val IsActive = true
  val IsWritable = true
  val SupportsLogin = true
  val ParentId = Some(2L)
  val MaxTokens = Some(100)
  val UsedTokens = Some(0)
  val CreatedAt = 100L
  val authorizedAt = 100L
  val UpdatedAt = 200L
  val LastUsedAt = 200L
  val CallbackUrl = Some("http://foo.bar")
  val ImageUrl = Some("http://image.com")
  val SupportUrl = Some("http://bar.baz")
  val AppPrivileges = Some(ByteBuffer.allocate(16))
  val AuthorizationCodeString = "authorizationCode"
  val AuthorizationCodeHash = "Oh9o1PaVg4qjCKv8-2FboM4q0gTY3oFz8HmFWcUgFzo:0:ac"
  val AuthorizationCodeKey = "authorizationKey"
  val CodeGrantType = "code"
  val RefreshTokenGrantType = "refresh_token"
  val State = "state"
  val ClientId = "M1M5R3BMVy13QmpScXkzTUt5OE46MTpjaQ" // encoded
  val OAuth2Secret = "9pRwqJw_hnoGkAIRimoPaCq0AN1aGEh1lg1ZqNvEvGfSivy5dw"
  val TestOrganization = Some(
    Organization(
      name = "org",
      url = Some("http://org.url.com"),
      termsAndConditionsUrl = Some("http://terms.com"),
      privacyPolicyUrl = Some("http://privacy.com")
    ))

  val TestClientApplication = ClientApplication(
    id = ClientApplicationId,
    userId = UserId,
    name = AppName,
    consumerKey = ConsumerKey,
    secret = Secret,
    description = Description,
    url = Url,
    isActive = IsActive,
    isWritable = IsWritable,
    supportsLogin = SupportsLogin,
    parentId = ParentId,
    maxTokens = MaxTokens,
    usedTokens = UsedTokens,
    callbackUrl = CallbackUrl,
    supportUrl = SupportUrl,
    imageUrl = ImageUrl,
    appPrivileges = AppPrivileges,
    organization = TestOrganization,
    updatedAt = UpdatedAt,
    createdAt = CreatedAt,
    supportsOauth2 = Some(true),
    oauth2ClientId = Some(ClientId),
    oauth2Secret = Some(OAuth2Secret)
  )

  val urlValidator = new ThreadLocal[ClientApplicationUrlValidator] {
    override def initialValue = {
      new CustomProtocolClientApplicationUrlValidator(
        // OAuth2 redirect URI must NOT include any fragment component: AUTHPLT-1728
        oauthUrlValidator = new OAuthUrlValidator(
          UrlValidator.ALLOW_ALL_SCHEMES + UrlValidator.NO_FRAGMENTS),
        statsReceiver = statsReceiver.scope("redirect_uri_validator")
      )
    }
  }
  private val validator = new OAuth2RedirectUriValidator(
    urlValidator,
    false,
    statsReceiver.scope("oauth2_redirect_uri_validator"))

  test("custom protocols should be allowed (see AUTHPLT-2184)") {
    validator.validateRedirectUri(
      "twitter://",
      Some(TestClientApplication.copy(callbackUrl = Some("twitter://")))) mustBe None
  }

  test("custom protocols should be allowed (see AUTHPLT-2184), with path") {
    validator.validateRedirectUri(
      "twitter://test",
      Some(TestClientApplication.copy(callbackUrl = Some("twitter://test")))) mustBe None
  }

  test("standard protocols should be allowed") {
    validator.validateRedirectUri(
      "https://twitter.com/callback",
      Some(
        TestClientApplication.copy(callbackUrl = Some("https://twitter.com/callback")))) mustBe None
  }

  test("different port is not allowed (callback)") {
    validator.validateRedirectUri(
      "https://twitter.com/callback",
      Some(
        TestClientApplication.copy(callbackUrl =
          Some("https://twitter.com:80/callback")))) mustBe Some(REDIRECT_URI_LOCKED)
  }

  test("different port is not allowed (redirect uri)") {
    validator.validateRedirectUri(
      "https://twitter.com:80/callback",
      Some(
        TestClientApplication.copy(callbackUrl =
          Some("https://twitter.com/callback")))) mustBe Some(REDIRECT_URI_LOCKED)
  }

  test("empty redirect url is not allowed") {
    validator.validateRedirectUri(
      "",
      Some(
        TestClientApplication.copy(callbackUrl =
          Some("https://twitter.com/callback")))) mustBe Some(INVALID_URI_FORMAT)
  }

  test("empty callbackUrl is not allowed") {
    validator.validateRedirectUri(
      "https://twitter.com/callback",
      Some(TestClientApplication.copy(callbackUrl = Some("")))) mustBe Some(
      APP_WITHOUT_REDIRECT_URI)
  }

  test("invalid url is not allowed") {
    validator.validateRedirectUri(
      "twitter.com/callback",
      Some(
        TestClientApplication.copy(callbackUrl =
          Some("https://twitter.com/callback")))) mustBe Some(INVALID_URI_FORMAT)
  }

  test("invalid callbackUrl is not allowed") {
    validator.validateRedirectUri(
      "https://twitter.com/callback",
      Some(TestClientApplication.copy(callbackUrl = Some("twitter.com/callback")))) mustBe Some(
      REDIRECT_URI_LOCKED)
  }
}
