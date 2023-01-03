package com.twitter.auth.models

/**
 * OAuth2 Grant Types supported
 * - Authorization Code (https://oauth.net/2/grant-types/authorization-code)
 * - Refresh Token (https://oauth.net/2/grant-types/refresh-token)
 *
 * Refer to https://dev.twitter.com/docs/application-permission-model/faq
 */
object OAuth2GrantTypes extends Enumeration {
  type OAuth2GrantTypes = Value

  val AuthorizationCode = Value("authorization_code")
  val RefreshToken = Value("refresh_token")
  val ClientCredentials = Value("client_credentials")
  val Unknown = Value("unknown")

  def withNameWithDefault(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)
}
