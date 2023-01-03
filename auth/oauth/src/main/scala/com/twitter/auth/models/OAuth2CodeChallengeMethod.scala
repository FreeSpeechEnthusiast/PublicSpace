package com.twitter.auth.models

/**
 * Supported encoding methods for OAuth2 PKCE secret
 */
object OAuth2CodeChallengeMethod extends Enumeration {
  type CodeChallengeMethod = Value

  val S256 = Value("s256")
  val Plain = Value("plain")
  val Unknown = Value("unknown")

  def withNameWithDefault(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)
}
