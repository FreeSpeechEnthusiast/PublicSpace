package com.twitter.auth.models

import com.twitter.auth.oauth2.thriftscala.{OAuth2AccessTokenStorageThrift, OAuthTokenType}

case class OAuth2AccessToken(
  tokenKey: String,
  override val tokenHash: String,
  override val clientApplicationId: Long,
  override val createdAt: Long,
  override val updatedAt: Option[Long],
  override val lastSeenAt: Option[Long],
  override val tiaToken: Option[TiaToken],
  override val refreshTokenKey: Option[String],
  userId: Long,
  scopes: Set[String],
  expiresAt: Long,
  invalidateAt: Option[Long],
  authorizedAt: Option[Long])
    extends AccessToken(
      token = tokenKey,
      tokenHash = tokenHash,
      clientApplicationId = clientApplicationId,
      createdAt = createdAt,
      updatedAt = updatedAt,
      lastSeenAt = lastSeenAt,
      tiaToken = tiaToken,
      refreshTokenKey = refreshTokenKey,
      encryptionKeyVersion = None
    ) {

  // The OAuth2AccessToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"OAuth2AccessToken(<all fields redacted, tokenHash: ${this.tokenHash}>)"
}

object OAuth2AccessToken {

  def fromStorageThrift(tToken: OAuth2AccessTokenStorageThrift): OAuth2AccessToken = {
    OAuth2AccessToken(
      tokenKey = tToken.tokenKey,
      tokenHash = tToken.tokenHash,
      clientApplicationId = tToken.clientApplicationId,
      createdAt = tToken.createdAt,
      updatedAt = tToken.updatedAt,
      lastSeenAt = tToken.lastSeenAt,
      tiaToken = None,
      userId = tToken.userId,
      scopes = tToken.scopes.toSet,
      expiresAt = tToken.expiresAt,
      refreshTokenKey = tToken.refreshTokenKey,
      invalidateAt = tToken.invalidateAt,
      authorizedAt = tToken.authorizedAt
    )
  }

  def toStorageThrift(token: OAuth2AccessToken): OAuth2AccessTokenStorageThrift = {
    OAuth2AccessTokenStorageThrift(
      tokenKey = token.tokenKey,
      tokenHash = token.tokenHash,
      clientApplicationId = token.clientApplicationId,
      userId = token.userId,
      scopes = token.scopes,
      expiresAt = token.expiresAt,
      createdAt = token.createdAt,
      updatedAt = token.updatedAt,
      lastSeenAt = token.lastSeenAt,
      tokenType = OAuthTokenType.Oauth2AccessToken,
      refreshTokenKey = token.refreshTokenKey,
      authorizedAt = token.authorizedAt
    )
  }

}
