package com.twitter.auth.sso.models

import com.twitter.auth.sso.thriftscala.{SsoProvider => TSsoProvider}

sealed trait SsoProvider

case class SsoProviderInfo(
  providerSsoId: SsoId,
  emailAddress: Email,
  emailVerified: Option[Boolean] = None,
  displayName: Option[String] = None,
  avatarImageUrl: Option[String] = None)

object SsoProvider {

  case object Apple extends SsoProvider
  case object Google extends SsoProvider
  case object Test extends SsoProvider

  val SSOProviderVector = Vector(Apple, Google, Test)

  def fromString(ssoProviderLowerCaseStr: String): Option[SsoProvider] = {
    SSOProviderVector.find(_.toString.toLowerCase == ssoProviderLowerCaseStr)
  }

  def toThrift(provider: SsoProvider): TSsoProvider = {
    provider match {
      case Apple => TSsoProvider.Apple
      case Google => TSsoProvider.Google
      case Test => TSsoProvider.Test
    }
  }

}
