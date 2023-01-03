package com.twitter.auth.models

import com.twitter.accounts.util.CryptoUtils
import com.twitter.passbird.requesttoken.thriftscala.{RequestToken => TRequestToken}

case class OAuth1RequestToken(
  token: String,
  secret: String,
  clientApplication: Long,
  userId: Option[Long],
  callbackUrl: String,
  oauthVerifier: String,
  createdAt: Option[Long],
  authorizedAt: Option[Long],
  invalidatedAt: Option[Long],
  privileges: Option[_root_.java.nio.ByteBuffer] = None) {

  // The OAuth1RequestToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"OAuth1RequestToken(<all fields redacted, tokenHash: ${CryptoUtils.hash(token)}>)"
}

object OAuth1RequestToken {

  def fromThrift(t: TRequestToken): OAuth1RequestToken = {
    OAuth1RequestToken(
      token = t.token,
      secret = t.secret,
      clientApplication = t.clientApplicationId,
      userId = t.userId,
      callbackUrl = t.callbackUrl,
      oauthVerifier = t.oauthVerifier,
      createdAt = t.createdAt,
      authorizedAt = t.authorizedAt,
      invalidatedAt = t.invalidatedAt,
      privileges = t.privileges
    )
  }
}
