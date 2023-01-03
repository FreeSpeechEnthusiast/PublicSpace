package com.twitter.auth.authentication.models

import com.twitter.finagle.http.HeaderMap

case class AuthRequest(
  headerParams: scala.collection.Map[String, String],
  cookies: Option[scala.collection.Map[String, String]] = None,
  body: Option[String] = None,
  url: Option[String] = None,
  method: Option[String] = None,
  ampEmailToken: Option[String] = None) {
  val headerMap: HeaderMap = HeaderMap()
  for ((k, v) <- headerParams) headerMap.add(k, v)

  // The AuthRequest has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"AuthRequest(<all fields redacted>)"
}
