package com.twitter.auth.models

import com.twitter.auth.oauth2.thriftscala.{
  OAuth2AuthorizationCodeStorageThrift,
  CodeChallengeMethod => TCodeChallengeMethod
}

case class OAuth2AuthorizationCode(
  codeKey: String,
  codeHash: String,
  state: String,
  clientId: String,
  userId: Long,
  redirectUri: String,
  scopes: Set[String],
  codeChallengeMethod: TCodeChallengeMethod,
  codeChallenge: String,
  expiresAt: Long,
  createdAt: Long,
  approvedAt: Option[Long] = None) {
  // The OAuth2AuthorizationCode has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"OAuth2AuthorizationCode(<all fields redacted, tokenHash: ${codeHash}>)"
}

object OAuth2AuthorizationCode {

  def toStorageThrift(code: OAuth2AuthorizationCode): OAuth2AuthorizationCodeStorageThrift = {
    OAuth2AuthorizationCodeStorageThrift(
      codeKey = code.codeKey,
      codeHash = code.codeHash,
      state = code.state,
      clientId = code.clientId,
      userId = code.userId,
      redirectUri = code.redirectUri,
      scopes = code.scopes,
      codeChallengeMethod = code.codeChallengeMethod,
      codeChallenge = code.codeChallenge,
      expiresAt = code.expiresAt,
      createdAt = code.createdAt,
      approvedAt = code.approvedAt
    )

  }

  def fromStorageThrift(tCode: OAuth2AuthorizationCodeStorageThrift): OAuth2AuthorizationCode = {
    OAuth2AuthorizationCode(
      codeKey = tCode.codeKey,
      codeHash = tCode.codeHash,
      state = tCode.state,
      clientId = tCode.clientId,
      userId = tCode.userId,
      redirectUri = tCode.redirectUri,
      scopes = tCode.scopes.toSet,
      codeChallengeMethod = tCode.codeChallengeMethod,
      codeChallenge = tCode.codeChallenge,
      expiresAt = tCode.expiresAt,
      createdAt = tCode.createdAt,
      approvedAt = tCode.approvedAt
    )
  }
}
