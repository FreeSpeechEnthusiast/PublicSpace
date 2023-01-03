package com.twitter.auth.authentication.models

import com.twitter.auth.authenforcement.thriftscala.{LegacyMetadata, Passport}
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode

case class AuthResponse(
  passport: Option[Passport] = None,
  authResultCode: Option[AuthResultCode] = None,
  externalAuthResultCode: Option[Int] = None,
  useNewAuthNFilter: Option[Boolean] = None,
  authenticationType: Option[AuthenticationType] = None,
  legacyMetadata: Option[LegacyMetadata] = None,
  userAssertionSignature: Option[String] = None) {

  override def toString: String =
    s"AuthResponse(<all fields redacted>)"
}
