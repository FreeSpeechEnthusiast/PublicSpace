package com.twitter.auth.models

import com.twitter.accounts.util.CryptoUtils
import com.twitter.passbird.accesstoken.thriftscala.{AccessToken => TAccessToken}

case class OAuth2AppOnlyToken(
  override val token: String,
  override val tokenHash: String,
  override val clientApplicationId: Long,
  override val createdAt: Long,
  override val updatedAt: Option[Long],
  override val lastSeenAt: Option[Long],
  override val encryptionKeyVersion: Option[Int],
  override val tiaToken: Option[TiaToken],
  isWritable: Boolean,
  authorizedAt: Long,
  invalidateAt: Option[Long])
    extends AccessToken(
      token = token,
      tokenHash = tokenHash,
      clientApplicationId = clientApplicationId,
      createdAt = createdAt,
      updatedAt = updatedAt,
      lastSeenAt = lastSeenAt,
      encryptionKeyVersion = encryptionKeyVersion,
      tiaToken = tiaToken
    ) {
  // The OAuth2AppOnlyToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"OAuth2AppOnlyToken(<all fields redacted, tokenHash: ${this.tokenHash}>)"
}

object OAuth2AppOnlyToken {

  def fromThrift(t: TAccessToken): OAuth2AppOnlyToken = {
    OAuth2AppOnlyToken(
      token = t.token,
      tokenHash = CryptoUtils.hash(t.token),
      clientApplicationId = t.clientApplicationId,
      createdAt = t.createdAt,
      updatedAt = Some(t.updatedAt),
      lastSeenAt = t.lastSeenAtMsec,
      encryptionKeyVersion = t.encryptionKeyVersion,
      tiaToken = t.passbirdToken.map(TiaToken.fromThrift),
      isWritable = t.isWritable,
      authorizedAt = t.authorizedAt,
      invalidateAt = t.invalidatedAt
    )
  }
}
