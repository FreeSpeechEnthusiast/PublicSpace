package com.twitter.auth.models

import com.twitter.accounts.util.CryptoUtils
import com.twitter.passbird.accesstoken.thriftscala.{AccessToken => TAccessToken}
import java.nio.ByteBuffer

case class SessionToken(
  token: String,
  tokenHash: String,
  userId: Long,
  isWritable: Boolean,
  privileges: Option[ByteBuffer],
  // PassbirdToken
  // https://cgit.twitter.biz/source/tree/passbird/thrift-only/src/main/thrift/com/twitter/passbird/AccessToken.thrift#n53
  tiaToken: Option[TiaToken],
  createdAt: Long,
  updatedAt: Option[Long],
  lastSeenAt: Option[Long],
  encryptionKeyVersion: Option[Int]) {

  // The SessionToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"SessionToken(<all fields redacted, tokenHash: ${this.tokenHash}>)"
}

object SessionToken {

  def fromThrift(t: TAccessToken): SessionToken = {
    SessionToken(
      token = t.token,
      tokenHash = CryptoUtils.hash(t.token),
      createdAt = t.createdAt,
      updatedAt = Some(t.updatedAt),
      lastSeenAt = t.lastSeenAtMsec,
      encryptionKeyVersion = t.encryptionKeyVersion,
      tiaToken = t.passbirdToken.map(TiaToken.fromThrift),
      userId = t.userId,
      privileges = t.privileges,
      isWritable = t.isWritable
    )
  }
}
