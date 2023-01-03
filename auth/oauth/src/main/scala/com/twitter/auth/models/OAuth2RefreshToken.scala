package com.twitter.auth.models

import com.twitter.auth.oauth2.thriftscala.OAuth2RefreshTokenStorageThrift

case class OAuth2RefreshToken(
  tokenKey: String,
  tokenHash: String,
  accessTokenKey: String,
  clientApplicationId: Long,
  scopes: Set[String],
  userId: Long,
  expiresAt: Long,
  createdAt: Long,
  authorizedAt: Option[Long]) {
  // The OAuth2RefreshToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"OAuth2RefreshToken(<all fields redacted, tokenHash: ${tokenHash}>)"
}

object OAuth2RefreshToken {
  def fromStorageThrift(tToken: OAuth2RefreshTokenStorageThrift): OAuth2RefreshToken = {
    OAuth2RefreshToken(
      tokenKey = tToken.tokenKey,
      tokenHash = tToken.tokenHash,
      accessTokenKey = tToken.accessTokenKey,
      clientApplicationId = tToken.clientApplicationId,
      userId = tToken.userId,
      expiresAt = tToken.expiresAt,
      createdAt = tToken.createdAt,
      scopes = tToken.scopes.toSet,
      authorizedAt = tToken.authorizedAt
    )
  }

  def toStorageThrift(token: OAuth2RefreshToken): OAuth2RefreshTokenStorageThrift = {
    OAuth2RefreshTokenStorageThrift(
      tokenKey = token.tokenKey,
      tokenHash = token.tokenHash,
      accessTokenKey = token.accessTokenKey,
      clientApplicationId = token.clientApplicationId,
      userId = token.userId,
      expiresAt = token.expiresAt,
      createdAt = token.createdAt,
      scopes = token.scopes,
      authorizedAt = token.authorizedAt
    )
  }
}
