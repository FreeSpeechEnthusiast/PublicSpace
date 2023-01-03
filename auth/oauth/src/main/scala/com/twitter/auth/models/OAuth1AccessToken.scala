package com.twitter.auth.models

import com.twitter.accounts.util.CryptoUtils
import com.twitter.passbird.accesstoken.thriftscala.{AccessToken => TAccessToken}
import java.nio.ByteBuffer

case class OAuth1AccessToken(
  override val token: String,
  override val tokenHash: String,
  override val clientApplicationId: Long,
  override val createdAt: Long,
  override val updatedAt: Option[Long],
  override val lastSeenAt: Option[Long],
  override val encryptionKeyVersion: Option[Int],
  override val tiaToken: Option[TiaToken],
  secret: String,
  userId: Long,
  privileges: Option[ByteBuffer],
  isWritable: Boolean,
  authorizedAt: Long,
  invalidatedAt: Option[Long])
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

  // The OAuth2AccessToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"OAuth1AccessToken(<all fields redacted, tokenHash: ${this.tokenHash}>)"
}

object OAuth1AccessToken {

  def fromThrift(t: TAccessToken): OAuth1AccessToken = {
    OAuth1AccessToken(
      token = t.token,
      tokenHash = CryptoUtils.hash(t.token),
      clientApplicationId = t.clientApplicationId,
      createdAt = t.createdAt,
      updatedAt = Some(t.updatedAt),
      lastSeenAt = t.lastSeenAtMsec,
      encryptionKeyVersion = t.encryptionKeyVersion,
      tiaToken = t.passbirdToken.map(TiaToken.fromThrift),
      secret = t.secret,
      userId = t.userId,
      privileges = t.privileges,
      isWritable = t.isWritable,
      authorizedAt = t.authorizedAt,
      invalidatedAt = t.invalidatedAt
    )
  }
}
