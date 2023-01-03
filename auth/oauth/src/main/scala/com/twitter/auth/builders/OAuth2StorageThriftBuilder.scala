package com.twitter.auth.builders

import com.twitter.auth.oauth2.thriftscala.OAuth2AccessTokenStorageThrift
import com.twitter.auth.oauth2.thriftscala.OAuth2AuthorizationCodeStorageThrift
import com.twitter.auth.oauth2.thriftscala.OAuth2ClientAccessTokenStorageThrift
import com.twitter.auth.oauth2.thriftscala.OAuth2RefreshTokenStorageThrift
import com.twitter.auth.oauth2.thriftscala.OAuth2ServiceClientStorageThrift
import com.twitter.auth.oauth2.thriftscala.ClientType
import com.twitter.auth.oauth2.thriftscala.OAuthTokenType
import com.twitter.util.Time

object OAuth2StorageThriftBuilder {

  def createOAuth2ServiceClient(
    clientId: String,
    clientSecret: Option[String]
  ): OAuth2ServiceClientStorageThrift = {
    OAuth2ServiceClientStorageThrift(
      clientId = clientId,
      clientSecret = clientSecret
    )
  }

  def createOAuth2AccessToken(
    tokenKey: String,
    tokenHash: String,
    refreshTokenKey: Option[String],
    clientApplicationId: Long,
    userId: Long,
    scopes: Set[String] = Set.empty,
    expiresAt: Long,
    createdAt: Long,
    lastSeenAt: Option[Long],
    updatedAt: Option[Long],
    ttl: Option[Long] = None,
    authorizedAt: Option[Long]
  ): OAuth2AccessTokenStorageThrift = {
    OAuth2AccessTokenStorageThrift(
      tokenKey = tokenKey,
      tokenHash = tokenHash,
      clientApplicationId = clientApplicationId,
      userId = userId,
      scopes = scopes,
      expiresAt = expiresAt,
      createdAt = createdAt,
      updatedAt = updatedAt,
      lastSeenAt = lastSeenAt,
      tokenType = OAuthTokenType.Oauth2AccessToken,
      refreshTokenKey = refreshTokenKey,
      ttl = ttl,
      authorizedAt = authorizedAt
    )
  }

  def createOAuth2ClientAccessToken(
    tokenKey: String,
    tokenHash: String,
    clientId: String,
    scopes: Set[String] = Set.empty,
    expiresAt: Long,
    createdAt: Long,
    invalidateAt: Option[Long],
    clientType: ClientType
  ): OAuth2ClientAccessTokenStorageThrift = {
    OAuth2ClientAccessTokenStorageThrift(
      tokenKey = tokenKey,
      tokenHash = tokenHash,
      clientId = clientId,
      scopes = scopes,
      expiresAt = expiresAt,
      createdAt = createdAt,
      invalidateAt = invalidateAt,
      clientType = clientType
    )
  }

  def createOAuth2RefreshToken(
    tokenKey: String,
    tokenHash: String,
    accessTokenKey: String,
    clientApplicationId: Long,
    userId: Long,
    expiresAt: Long,
    createdAt: Long,
    scopes: Set[String] = Set.empty,
    authorizedAt: Option[Long]
  ): OAuth2RefreshTokenStorageThrift = {
    OAuth2RefreshTokenStorageThrift(
      tokenKey = tokenKey,
      tokenHash = tokenHash,
      accessTokenKey = accessTokenKey,
      clientApplicationId = clientApplicationId,
      userId = userId,
      expiresAt = expiresAt,
      createdAt = createdAt,
      scopes = scopes,
      authorizedAt = authorizedAt
    )
  }

  def createOAuth2AuthorizationCode(
    codeKey: String,
    codeHash: String,
    state: String,
    clientId: String,
    userId: Long,
    redirectUri: String,
    scopes: Set[String] = Set.empty,
    codeChallengeMethod: com.twitter.auth.oauth2.thriftscala.CodeChallengeMethod,
    codeChallenge: String,
    expiresAt: Long,
    createdAt: Long = Time.now.inMilliseconds,
    approvedAt: Option[Long] = None
  ): OAuth2AuthorizationCodeStorageThrift = {
    OAuth2AuthorizationCodeStorageThrift(
      codeKey = codeKey,
      codeHash = codeHash,
      state = state,
      clientId = clientId,
      userId = userId,
      redirectUri = redirectUri,
      scopes = scopes,
      expiresAt = expiresAt,
      createdAt = createdAt,
      codeChallengeMethod = codeChallengeMethod,
      codeChallenge = codeChallenge,
      approvedAt = approvedAt
    )
  }

  def createOAuth2AmpEmailToken(
    tokenKey: String,
    tokenHash: String,
    clientApplicationId: Long,
    userId: Long,
    scopes: Set[String] = Set.empty,
    expiresAt: Long,
    createdAt: Long,
    lastSeenAt: Option[Long],
    updatedAt: Option[Long],
    ttl: Option[Long] = None
  ): OAuth2AccessTokenStorageThrift = {
    OAuth2AccessTokenStorageThrift(
      tokenKey = tokenKey,
      tokenHash = tokenHash,
      clientApplicationId = clientApplicationId,
      userId = userId,
      scopes = scopes,
      expiresAt = expiresAt,
      createdAt = createdAt,
      updatedAt = updatedAt,
      lastSeenAt = lastSeenAt,
      tokenType = OAuthTokenType.Oauth2AccessToken,
      ttl = ttl,
      refreshTokenKey = None,
      authorizedAt = None
    )
  }
}
