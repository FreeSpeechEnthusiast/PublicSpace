package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.authenticationtype.thriftscala.{AuthenticationType => TAuthenticationType}

/**
 * The class is designed to avoid thrift and non-thrift models mixing
 */
case class RouteAuthType(underlying: Int) {
  def toThrift: TAuthenticationType = {
    TAuthenticationType(underlying)
  }

  /**
   * Highlight NoAuthentication and Unknown authentication type
   */
  lazy val requiresAuthentication: Boolean = {
    underlying != TAuthenticationType.NoAuthentication.getValue() &&
    underlying != TAuthenticationType.Unknown.getValue()
  }

  /**
   * Highlight authentication types with user identity
   */
  lazy val hasUserIdentity: Boolean = {
    requiresAuthentication && (
      underlying != TAuthenticationType.Oauth2GuestAuth.getValue() &&
      underlying != TAuthenticationType.Oauth2AppOnly.getValue() &&
      underlying != TAuthenticationType.Oauth2ClientCredential.getValue() &&
      underlying != TAuthenticationType.DeviceAuth.getValue()
    )
  }

  /**
   * Highlight authentication types without scopes support
   */
  lazy val supportsScopes: Boolean = {
    requiresAuthentication && (
      underlying != TAuthenticationType.Oauth1.getValue() &&
      underlying != TAuthenticationType.Oauth1TwoLegged.getValue() &&
      underlying != TAuthenticationType.Oauth1RequestToken.getValue() &&
      underlying != TAuthenticationType.Oauth1Xauth.getValue() &&
      underlying != TAuthenticationType.Session.getValue() &&
      underlying != TAuthenticationType.RestrictedSession.getValue()
    )
  }

  override def toString: String = {
    toThrift.name.toLowerCase
  }
}

object RouteAuthType {
  def fromThrift(thrift: TAuthenticationType): RouteAuthType = {
    RouteAuthType(thrift.getValue())
  }

  val AuthTypeOriginalNameToValueMap: Map[String, Int] =
    TAuthenticationType.list.map(t => t.originalName -> t.value).toMap
}
