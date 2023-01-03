package com.twitter.auth.utils
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.MustMatchers
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class OAuth2CredentialUtilsSpec
    extends FunSuite
    with OneInstancePerTest
    with MockitoSugar
    with MustMatchers {

  private val ValidAuthorizationCodeString =
    "S0ZEaFFMVzM3bzBibGtsT0tVRldSUDI1TVZIUjd5V0h5b0JBbEhzWWw2SUtZOjE2MjAzNTE1NzkxNTg6MTowOmFjOjE"
  private val ValidAccessTokenString =
    "Yk1xLU16dG5wZUlLNFdrSFUyUVFseXl3d040RzBrNEMxVDhQTTBmZkxyNUtrOjE2MjAzNTIwMTA4OTg6MToxOmF0OjE"
  //randomString:timestamp:formatVersion:secretId:typeOfToken:tokenManagerService
  private val ValidDecodedAccessTokenString =
    "bMq-MztnpeIK4WkHU2QQlyywwN4G0k4C1T8PM0ffLr5Kk:1620352010898:1:1:at:1"
  // see ValidDecodedAccessTokenString to know why secretId id is 1
  private val AccessTokenSecretId = 1
  private val ValidAccessTokenHash = "PAEUNS6q8b7k2JHefjrASZx4x0NP4B7Sxd3frmCUsEk:1:at"
  private val ValidRefreshTokenString =
    "ODZoSW5BdXJOancxZHRpcmYwa0I3UFhKM2tvNkNfemRKSktRYU9NckNDcGxWOjE2MjAzNTIwMTA4OTg6MTowOnJ0OjE"
  //randomString:timestamp:formatVersion:secretId:typeOfToken:tokenManagerService
  private val ValidDecodedRefreshTokenString =
    "86hInAurNjw1dtirf0kB7PXJ3ko6C_zdJJKQaOMrCCplV:1620352010898:1:0:rt:1"
  // see ValidDecodedRefreshTokenString to know why secretId id is 0
  private val RefreshTokenSecretId = 0
  private val ValidRefreshTokenHash = "Oh9o1PaVg4qjCKv8-2FboM4q0gTY3oFz8HmFWcUgFzo:0:rt"
  private val ValidClientAccessTokenHash = "pWgziNDufC7pscJx87ctA2RxyXu4QohlTRiwAd6V-S4:0:ct"
  private val ValidClientAccessTokenString =
    "bVpOellhX3E0R0p1MWRYOW4tVkdUR3djZ3JZT1Q1dmhRR0FTc2NXbzBwaTN4OjE2NDQwMTMwODkxMTk6MTowOmN0OjE"
  private val ValidDecodedClientAccessTokenString =
    "mZNzYa_q4GJu1dX9n-VGTGwcgrYOT5vhQGASscWo0pi3x:1644013089119:1:0:ct:1"
  // see ValidDecodedClientAccessTokenString to know why secretId id is 0
  private val ClientAccessTokenSecretId = 0
  private val TestKey = "test"
  private val Sha256HashForTestKey = "n4bQgYhMfWWaL-qgxVrQFaO_TxsrC4Is0V1sFbDwCgg"

  private val ValidOAuth2ClientId = "ZmNNWUozR09VUXpib21HNDdmQ0Q6MTpjaQ"

  test("isFlightAuthManagedToken returns true for access token") {
    assert(OAuth2CredentialUtils.isFlightAuthManagedToken(ValidAccessTokenString))
  }

  test("isFlightAuthManagedToken returns true for refresh token") {
    assert(OAuth2CredentialUtils.isFlightAuthManagedToken(ValidRefreshTokenString))
  }

  test("isFlightAuthManagedToken returns true for authorization code") {
    assert(OAuth2CredentialUtils.isFlightAuthManagedToken(ValidAuthorizationCodeString))
  }

  test("isFlightAuthManagedToken returns true for client access token") {
    assert(OAuth2CredentialUtils.isFlightAuthManagedToken(ValidClientAccessTokenString))
  }

  test("isFlightAuthManagedToken returns false for invalid string") {
    assert(!OAuth2CredentialUtils.isFlightAuthManagedToken("random invalid string"))
  }

  test("isFlightAuthManagedHashToken returns true for flightauth managed hash") {
    assert(OAuth2CredentialUtils.isFlightAuthManagedHashToken(ValidAccessTokenHash))
    assert(OAuth2CredentialUtils.isFlightAuthManagedHashToken(ValidRefreshTokenHash))
    assert(OAuth2CredentialUtils.isFlightAuthManagedHashToken(ValidClientAccessTokenHash))
  }

  test("isFlightAuthManagedHashToken returns false for invalid string") {
    assert(!OAuth2CredentialUtils.isFlightAuthManagedHashToken("random invalid string"))
    assert(!OAuth2CredentialUtils.isFlightAuthManagedHashToken(ValidRefreshTokenString))
    assert(!OAuth2CredentialUtils.isFlightAuthManagedHashToken(ValidAccessTokenString))
    assert(!OAuth2CredentialUtils.isFlightAuthManagedHashToken(ValidClientAccessTokenString))
  }

  test("decodeTokenType returns access token") {
    assert(
      OAuth2CredentialUtils.decodeTokenType(
        ValidAccessTokenString) == OAuth2CredentialType.ACCESS_TOKEN)
  }

  test("decodeTokenType returns refresh token") {
    assert(
      OAuth2CredentialUtils.decodeTokenType(
        ValidRefreshTokenString) == OAuth2CredentialType.REFRESH_TOKEN)
  }

  test("decodeTokenType returns authorization code") {
    assert(
      OAuth2CredentialUtils.decodeTokenType(
        ValidAuthorizationCodeString) == OAuth2CredentialType.AUTHORIZATION_CODE)
  }

  test("decodeTokenType returns client access token") {
    assert(
      OAuth2CredentialUtils.decodeTokenType(
        ValidClientAccessTokenString) == OAuth2CredentialType.CLIENT_ACCESS_TOKEN)
  }

  test("decodeTokenType returns unknown code") {
    assert(OAuth2CredentialUtils.decodeTokenType("invalid") == OAuth2CredentialType.UNKNOWN)
  }

  test("decodeSecretId returns valid secretId for access token") {
    val secretIdOpt = OAuth2CredentialUtils.decodeSecretId(ValidDecodedAccessTokenString)
    assert(secretIdOpt.isDefined)
    assert(secretIdOpt.get == AccessTokenSecretId)
  }

  test("decodeSecretId returns valid secretId for client access token") {
    val secretIdOpt = OAuth2CredentialUtils.decodeSecretId(ValidDecodedClientAccessTokenString)
    assert(secretIdOpt.isDefined)
    assert(secretIdOpt.get == ClientAccessTokenSecretId)
  }

  test("decodeSecretId returns valid secretId for refresh token") {
    val secretIdOpt = OAuth2CredentialUtils.decodeSecretId(ValidDecodedRefreshTokenString)
    assert(secretIdOpt.isDefined)
    assert(secretIdOpt.get == RefreshTokenSecretId)
  }

  test("decodeSecretId returns valid secretId for invalid token") {
    assert(OAuth2CredentialUtils.decodeSecretId("invalid").isEmpty)
  }

  test("decodeSecretIdFromTokenHash returns secretId for refresh token hash") {
    val secretIdOpt = OAuth2CredentialUtils.decodeSecretIdFromTokenHash(ValidRefreshTokenHash)
    assert(secretIdOpt.isDefined)
    assert(secretIdOpt.get == RefreshTokenSecretId)
  }

  test("decodeSecretIdFromTokenHash returns secretId for access token hash") {
    val secretIdOpt = OAuth2CredentialUtils.decodeSecretIdFromTokenHash(ValidAccessTokenHash)
    assert(secretIdOpt.isDefined)
    // see ValidAccessTokenHash token string to see why secretId is 1
    assert(secretIdOpt.get == AccessTokenSecretId)
  }

  test("decodeSecretIdFromTokenHash returns secretId for client access token hash") {
    val secretIdOpt = OAuth2CredentialUtils.decodeSecretIdFromTokenHash(ValidClientAccessTokenHash)
    assert(secretIdOpt.isDefined)
    // see ValidClientAccessTokenHash token string to see why secretId is 1
    assert(secretIdOpt.get == ClientAccessTokenSecretId)
  }

  test("decodeSecretIdFromTokenHash returns none for random string") {
    val secretIdOpt = OAuth2CredentialUtils.decodeSecretIdFromTokenHash("invalid")
    assert(secretIdOpt.isEmpty)
  }

  test("decodeTokenTypeFromTokenHash returns refresh token for valid refresh token hash") {
    val resultOpt = OAuth2CredentialUtils.decodeTokenTypeFromTokenHash(ValidRefreshTokenHash)
    assert(resultOpt.isDefined);
    assert(resultOpt.get == OAuth2CredentialType.REFRESH_TOKEN);
  }

  test("decodeTokenTypeFromTokenHash returns access token for valid access token hash") {
    val resultOpt = OAuth2CredentialUtils.decodeTokenTypeFromTokenHash(ValidAccessTokenHash)
    assert(resultOpt.isDefined);
    assert(resultOpt.get == OAuth2CredentialType.ACCESS_TOKEN);
  }

  test("decodeTokenTypeFromTokenHash returns access token for valid client access token hash") {
    val resultOpt = OAuth2CredentialUtils.decodeTokenTypeFromTokenHash(ValidClientAccessTokenHash)
    assert(resultOpt.isDefined);
    assert(resultOpt.get == OAuth2CredentialType.CLIENT_ACCESS_TOKEN);
  }

  test("decodeTokenTypeFromTokenHash returns unknown for invalid string") {
    val resultOpt = OAuth2CredentialUtils.decodeTokenTypeFromTokenHash("invalid token string")
    assert(resultOpt.isEmpty);
  }

  test("hashKey correctly hashes refresh token string") {
    val resultOpt = OAuth2CredentialUtils.hashKey(
      key = ValidRefreshTokenString,
      isHashed = false,
      secretId = RefreshTokenSecretId,
      tokenType = OAuth2CredentialType.REFRESH_TOKEN)
    assert(resultOpt == ValidRefreshTokenHash);
  }

  test("hashKey correctly hashes access token string") {
    val resultOpt = OAuth2CredentialUtils.hashKey(
      key = ValidAccessTokenString,
      isHashed = false,
      secretId = AccessTokenSecretId,
      tokenType = OAuth2CredentialType.ACCESS_TOKEN)
    assert(resultOpt == ValidAccessTokenHash);
  }

  test("hashKey correctly hashes client access token string") {
    val resultOpt = OAuth2CredentialUtils.hashKey(
      key = ValidClientAccessTokenString,
      isHashed = false,
      secretId = ClientAccessTokenSecretId,
      tokenType = OAuth2CredentialType.CLIENT_ACCESS_TOKEN)
    assert(resultOpt == ValidClientAccessTokenHash);
  }

  test("hash correctly hashes key to sha-256") {
    assert(OAuth2CredentialUtils.hash(TestKey) == Sha256HashForTestKey);
  }

  test("createOauth2ClientId return valid client id") {
    val oauth2ClientId = OAuth2CredentialUtils.createOauth2ClientId()
    assert(OAuth2CredentialUtils.isValidOauth2ClientId(oauth2ClientId))
  }

  test("isValidOauth2ClientId validate client ids") {
    assert(OAuth2CredentialUtils.isValidOauth2ClientId(ValidOAuth2ClientId))
    assert(!OAuth2CredentialUtils.isValidOauth2ClientId("abc"))
    assert(!OAuth2CredentialUtils.isValidOauth2ClientId(ValidOAuth2ClientId + " "))
    assert(!OAuth2CredentialUtils.isValidOauth2ClientId(" "))
    assert(!OAuth2CredentialUtils.isValidOauth2ClientId(""))
    assert(!OAuth2CredentialUtils.isValidOauth2ClientId("<IMG SRC=j&#X41vascript:alert('test2')>"))
  }
}
