package com.twitter.auth.authentication

import com.twitter.accounts.util.CryptoUtils
import com.twitter.auth.authentication.models.ActAsUserParams
import com.twitter.auth.authentication.models.OAuth1RequestParams
import com.twitter.auth.authentication.models.OAuth1ThreeLeggedRequestParams
import com.twitter.auth.authentication.models.OAuth1TwoLeggedRequestParams
import com.twitter.auth.models.ClientApplication
import com.twitter.auth.models.OAuth1AccessToken
import com.twitter.auth.models.OAuth2AccessToken
import com.twitter.auth.models.OAuth2AppOnlyToken
import com.twitter.auth.models.OAuth2ClientAccessToken
import com.twitter.auth.models.Organization
import com.twitter.auth.oauth2.thriftscala.ClientType
import com.twitter.common.ip_address_utils.ClientIpAddressUtils
import com.twitter.finatra.tfe.HttpHeaderNames
import com.twitter.joauth.OAuthParams.OAuth1Params
import com.twitter.util.Time
import java.nio.ByteBuffer

object CommonFixtures {
  val TransactionId = "12345"
  val ClientApplicationId = 1L
  val UserId = 1L
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
  val UpdatedAt = 200L
  val CallbackUrl = Some("http://foo.bar")
  val ImageUrl = Some("http://image.com")
  val SupportUrl = Some("http://bar.baz")
  val AppPrivileges = Some(ByteBuffer.allocate(16))
  val Code = "code"
  val CodeGrantType = "code"
  val RefreshTokenGrantType = "refresh_token"
  val State = "state"
  val ClientId = "client_id"
  val RedirectUri = "http://www.redirectUri.com"
  val CodeVerifier = "code_verifier"
  val ResponseType = "code"
  val CodeChallenge = "code_challenge"
  val CodeChallengeMethodS256 = "S256"
  val CodeChallengeMethodPlain = "plain"
  val Scopes: Set[String] = Set("scope_1", "scope_2", "scope_3")
  val ApprovedAt = Some(200L)
  val LastSeenAt = Some(200L)
  val ExpiredAt = 400L
  val EncryptionKeyVersion = Some(1)
  val AccessTokenString = "access_token"
  val AccessTokenHashString = "access_token_hash"
  val RefreshTokenString = "refresh_token"
  val RefreshTokenKey = "refresh_token_key"
  val Token = "token"
  val TokenSignature = "tokenSignature"
  val TokenHash = CryptoUtils.hash(Token)
  val OrphanedToken = "OrphanedToken"
  val Nonce = "nonce"
  val Signature = "signature"
  val EncodedSignature = "iK399J8Vt2rNNHlcPLHHwwQ6Ipg%3D"
  val DecodedSignature = "iK399J8Vt2rNNHlcPLHHwwQ6Ipg="
  val EncodedBearerToken = "AAAAAAAAAAAAAAAAAAAAAC2fegAAAAAACREbuwaI3kTD%2BojWZxVVNaxLwqE%3DOiB" +
    "I1zEdeqUDKAn3JL6a3DvrVZalRTf2ja2566I76lq29hF8Jo"
  val DecodedBearerToken = "AAAAAAAAAAAAAAAAAAAAAC2fegAAAAAACREbuwaI3kTD+ojWZxVVNaxLwqE=OiBI1zE" +
    "deqUDKAn3JL6a3DvrVZalRTf2ja2566I76lq29hF8Jo"
  val SignatureMethod = "HMAC-SHA1"
  val Timestamp = "1555225314"
  val Version = "1.0"

  val DummyHeader = "abcdddddd"

  val Oauth1ThreeLeggedAuthHeader = "oauth oauth_consumer_key=\"consumer_key\", " +
    "oauth_nonce=\"nonce\", " +
    "oauth_signature=\"iK399J8Vt2rNNHlcPLHHwwQ6Ipg%3D\", " +
    "oauth_signature_method=\"HMAC-SHA1\", " +
    "oauth_timestamp=\"1555225314\", " +
    "oauth_token=\"token\", " +
    "oauth_version=\"1.0\","

  val Oauth1TwoLeggedAuthHeader =
    "OAuth oauth_consumer_key=consumer_key, oauth_nonce=nonce, " +
      "oauth_signature=signature, oauth_signature_method=HMAC-SHA1, oauth_timestamp=1555227060, oauth_version=1.0"

  val Oauth1HeaderMissedParams = "oauth oauth_consumer_key=\"consumer_key\", " +
    "oauth_nonce=\"nonce\", " +
    "oauth_signature_method=\"HMAC-SHA1\", " +
    "oauth_timestamp=\"1555225314\", " +
    "oauth_version=\"1.0\","

  val Oauth1UnknownAuthHeader =
    "OAuth oauth_nonce=nonce, oauth_signature=signature, " +
      "oauth_signature_method=HMAC-SHA1, oauth_timestamp=1555227060, oauth_token=token, oauth_version=1.0"

  val Oauth2AuthHeader = "Bearer " + EncodedBearerToken

  val UnknownHeader = "xxx oauth_consumer_key=\"consumer_key\", " +
    "oauth_nonce=\"nonce\", " +
    "oauth_signature=\"signature\", " +
    "oauth_signature_method=\"HMAC-SHA1\", " +
    "oauth_timestamp=\"1555225314\", " +
    "oauth_token=\"token\", " +
    "oauth_version=\"1.0\","

  // scopes
  val IsWritableScope = "is_writable"
  val AppOnlyScope = "AppOnly"
  val GuestAuthScope = "Guest"
  val ClientCredentialScope = "ClientCredential"

  val ScopesOAuth1 = Set[String](IsWritableScope)
  val ScopesAppOnly = Set[String](AppOnlyScope)
  val ScopesGuest = Set[String](GuestAuthScope)
  val ScopesClientCredential = Set[String](ClientCredentialScope)
  val BearerToken = "bearerToken123"
  val SessionId = "sessionId456"
  val OtherParam = Map[String, Seq[String]]()

  // Act-As User Params
  val ActAsUserId = 99L
  val ActAsUserParamsHeader = ActAsUserParams(None, Some(ActAsUserId))
  val ActAsUserParamsCookie = ActAsUserParams(Some(ActAsUserId), None)
  val ActAsUserParamsBothSame = ActAsUserParams(Some(ActAsUserId), Some(ActAsUserId))
  val ActAsUserParamsBothDifferent = ActAsUserParams(Some(22L), Some(ActAsUserId))

  val OAuthAuthenticateUrl = "/oauth/authenticate?"
  val CookieMap = Option(Map("auth_token" -> SessionId))
  val OtherParams = Map(
    "X-TFE-Requested-At" -> "1555227062063",
    "x-transport-ssl-protocol" -> "TLSv1.2",
    "x-tfe-version" -> "tsa/1.dev/5eef10.DEBUG",
    "X-Twitter-Internal" -> "internal",
    "finagle-ctx-com.twitter.finagle.retries" -> "0",
    "Content-Length" -> "0",
    "x-http2-scheme" -> "https",
    "X-Forwarded-For" -> "192.168.50.1",
    "x-tsa-client-protocol" -> "HTTP/1.1",
    HttpHeaderNames.X_TWITTER_AUDIT_IP -> "192.168.50.1", // backward compatible, keep until migration is completed.
    HttpHeaderNames.X_TWITTER_AUDIT_IP_THRIFT -> ClientIpAddressUtils
      .convertToClientIpAddressBase64Encoded("192.168.50.1").get,
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
    "X-TFE-Transaction-ID" -> "12345"
  )

  val Oauth1ThreeLeggedParams =
    new OAuth1Params(Token, ConsumerKey, Nonce, 123L, "123", Signature, SignatureMethod, Version)
  val Oauth1TwoLeggedParams =
    new OAuth1Params(null, ConsumerKey, Nonce, 123L, "123", Signature, SignatureMethod, Version)
  val Oauth1UnknownParams =
    new OAuth1Params(Token, null, Nonce, 123L, "123", Signature, SignatureMethod, Version)

  val Oauth1ThreeLeggedAuthParams: OAuth1RequestParams = OAuth1ThreeLeggedRequestParams(
    TransactionId,
    None,
    Oauth1ThreeLeggedParams,
    None,
    Some("https"),
    Some("api.twitter.com"),
    Some(443),
    Some("/1.0/test.html"),
    "GET",
    None,
    None)
  val Oauth1TwoLeggedAuthParams: OAuth1RequestParams = OAuth1TwoLeggedRequestParams(
    TransactionId,
    None,
    Oauth1TwoLeggedParams,
    None,
    Some("https"),
    Some("api.twitter.com"),
    Some(443),
    Some("/1.0/test.html"),
    "GET",
    None,
    None)

  val InvalidationFixDateInSeconds: Long =
    Time.at("2500-03-07 00:00:00 -0800").inSeconds

  val ValidOAuth2AccessToken: OAuth2AccessToken = OAuth2AccessToken(
    tokenKey = TokenSignature,
    tokenHash = TokenHash,
    clientApplicationId = ClientApplicationId,
    createdAt = 1L,
    updatedAt = Some(2L),
    lastSeenAt = Some(4L),
    userId = UserId,
    scopes = Scopes,
    expiresAt = Time.now.inSeconds + 1000L,
    tiaToken = None,
    refreshTokenKey = Some(RefreshTokenKey),
    invalidateAt = None,
    authorizedAt = Some(1L)
  )

  val ValidOAuth2ClientAccessToken: OAuth2ClientAccessToken = OAuth2ClientAccessToken(
    tokenKey = TokenSignature,
    tokenHash = TokenHash,
    clientId = ClientId,
    scopes = Scopes,
    expiresAt = Time.now.inSeconds + 1000L,
    createdAt = 1L,
    invalidateAt = None,
    clientType = ClientType.ServiceClient
  )

  val InValidOAuth2ClientAccessToken: OAuth2ClientAccessToken = OAuth2ClientAccessToken(
    tokenKey = TokenSignature,
    tokenHash = TokenHash,
    clientId = ClientId,
    scopes = Scopes,
    expiresAt = Time.now.inSeconds + 1000L,
    createdAt = 1L,
    invalidateAt = Some(Time.now.inSeconds),
    clientType = ClientType.ServiceClient
  )

  val ValidOAuth2AppOnlyToken: OAuth2AppOnlyToken = OAuth2AppOnlyToken(
    token = Token,
    tokenHash = TokenHash,
    clientApplicationId = ClientApplicationId,
    createdAt = 1L,
    updatedAt = Some(2L),
    lastSeenAt = Some(4L),
    encryptionKeyVersion = EncryptionKeyVersion,
    isWritable = true,
    authorizedAt = 3L,
    invalidateAt = None,
    tiaToken = None
  )

  val InValidOAuth2AppOnlyToken: OAuth2AppOnlyToken = OAuth2AppOnlyToken(
    token = Token,
    tokenHash = TokenHash,
    clientApplicationId = ClientApplicationId,
    createdAt = 1L,
    updatedAt = Some(2L),
    lastSeenAt = Some(4L),
    encryptionKeyVersion = EncryptionKeyVersion,
    isWritable = true,
    authorizedAt = 3L,
    invalidateAt = Some(Time.now.inSeconds),
    tiaToken = None
  )

  val ValidOAuth1AccessToken: OAuth1AccessToken = OAuth1AccessToken(
    userId = UserId,
    clientApplicationId = ClientApplicationId,
    token = Token,
    tokenHash = TokenHash,
    secret = Secret,
    createdAt = 1L,
    updatedAt = Some(2L),
    authorizedAt = 3L,
    lastSeenAt = Some(4L),
    invalidatedAt = Some(InvalidationFixDateInSeconds),
    isWritable = true,
    encryptionKeyVersion = EncryptionKeyVersion,
    privileges = None,
    tiaToken = None
  )

  val InvalidOAuth1AccessToken: OAuth1AccessToken = OAuth1AccessToken(
    userId = UserId,
    clientApplicationId = ClientApplicationId,
    token = Token,
    tokenHash = TokenHash,
    secret = Secret,
    createdAt = 1L,
    updatedAt = Some(2L),
    authorizedAt = 3L,
    lastSeenAt = Some(4L),
    invalidatedAt = Some(Time.now.inSeconds),
    isWritable = true,
    encryptionKeyVersion = EncryptionKeyVersion,
    privileges = None,
    tiaToken = None
  )

  val OrphanedOAuth1AccessToken: OAuth1AccessToken = OAuth1AccessToken(
    userId = UserId,
    clientApplicationId = ClientApplicationId,
    token = Token,
    tokenHash = TokenHash,
    secret = OrphanedToken,
    createdAt = 1L,
    updatedAt = Some(2L),
    authorizedAt = 3L,
    lastSeenAt = Some(4L),
    invalidatedAt = Some(Time.now.inSeconds),
    isWritable = true,
    encryptionKeyVersion = EncryptionKeyVersion,
    tiaToken = None,
    privileges = None
  )

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
    createdAt = CreatedAt,
    updatedAt = UpdatedAt
  )
}
