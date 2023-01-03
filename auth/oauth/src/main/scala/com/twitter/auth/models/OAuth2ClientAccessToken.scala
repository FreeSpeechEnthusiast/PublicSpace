package com.twitter.auth.models

import com.twitter.auth.oauth2.thriftscala.OAuth2ClientAccessTokenStorageThrift
import com.twitter.auth.oauth2.thriftscala.{ClientType => TClientType}

case class OAuth2ClientAccessToken(
  tokenKey: String,
  tokenHash: String,
  clientId: String,
  scopes: Set[String],
  expiresAt: Long,
  createdAt: Long,
  invalidateAt: Option[Long],
  clientType: TClientType) {
  // The OAuth2AccessToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"OAuth2ClientAccessToken(<all fields redacted, tokenHash: ${tokenHash}>)"
}

object OAuth2ClientAccessToken {
  def fromStorageThrift(tToken: OAuth2ClientAccessTokenStorageThrift): OAuth2ClientAccessToken = {
    OAuth2ClientAccessToken(
      tokenKey = tToken.tokenKey,
      tokenHash = tToken.tokenHash,
      clientId = tToken.clientId,
      scopes = tToken.scopes.toSet,
      expiresAt = tToken.expiresAt,
      createdAt = tToken.createdAt,
      invalidateAt = tToken.invalidateAt,
      clientType = tToken.clientType
    )
  }

  def toStorageThrift(token: OAuth2ClientAccessToken): OAuth2ClientAccessTokenStorageThrift = {
    OAuth2ClientAccessTokenStorageThrift(
      tokenKey = token.tokenKey,
      tokenHash = token.tokenHash,
      clientId = token.clientId,
      scopes = token.scopes,
      expiresAt = token.expiresAt,
      createdAt = token.createdAt,
      invalidateAt = token.invalidateAt,
      clientType = token.clientType
    )
  }
}
