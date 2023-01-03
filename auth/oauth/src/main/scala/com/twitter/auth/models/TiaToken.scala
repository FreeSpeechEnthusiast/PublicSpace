package com.twitter.auth.models

import com.twitter.passbird.accesstoken.thriftscala.{PassbirdToken => TPassbirdToken}

// Token for TIA. Represent PassbirdToken from
// https://cgit.twitter.biz/source/tree/passbird/thrift-only/src/main/thrift/com/twitter/passbird/AccessToken.thrift#n29.
case class TiaToken(id: Long, signatureBase64: String, additionalFields: Seq[String]) {
  // The OAuth2RefreshToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"TiaToken(<all fields redacted, id: $id>)"
}

object TiaToken {
  def fromThrift(t: TPassbirdToken): TiaToken = {
    TiaToken(id = t.id, signatureBase64 = t.signatureBase64, additionalFields = t.additionalFields)
  }
}
