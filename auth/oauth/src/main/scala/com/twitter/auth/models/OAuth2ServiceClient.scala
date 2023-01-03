package com.twitter.auth.models

import com.twitter.auth.oauth2.thriftscala.OAuth2ServiceClientStorageThrift

case class OAuth2ServiceClient(
  clientId: String,
  clientSecret: Option[String]) {
  // The OAuth2AccessToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"OAuth2ServiceClient(<all fields redacted, id: ${clientId}>)"
}

object OAuth2ServiceClient {
  def fromStorageThrift(tClient: OAuth2ServiceClientStorageThrift): OAuth2ServiceClient = {
    OAuth2ServiceClient(
      clientId = tClient.clientId,
      clientSecret = tClient.clientSecret
    )
  }

  def toStorageThrift(tClient: OAuth2ServiceClient): OAuth2ServiceClientStorageThrift = {
    OAuth2ServiceClientStorageThrift(
      clientId = tClient.clientId,
      clientSecret = tClient.clientSecret
    )
  }
}
