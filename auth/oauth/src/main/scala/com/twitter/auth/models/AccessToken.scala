package com.twitter.auth.models

abstract class AccessToken(
  val token: String,
  val tokenHash: String,
  val clientApplicationId: Long,
  val createdAt: Long,
  val updatedAt: Option[Long],
  val lastSeenAt: Option[Long],
  val encryptionKeyVersion: Option[Int],
  // PassbirdToken
  // https://cgit.twitter.biz/source/tree/passbird/thrift-only/src/main/thrift/com/twitter/passbird/AccessToken.thrift#n53
  val tiaToken: Option[TiaToken],
  val refreshTokenKey: Option[String] = None) {

  // The AccessToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"AccessToken(<all fields redacted, tokenHash: ${this.tokenHash}>)"
}
