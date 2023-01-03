package com.twitter.auth.models

/**
 * Supported response types as part of OAuth2 authorization process
 */
object OAuth2ResponseType extends Enumeration {
  type OAuth2ResponseType = Value

  val Code = Value("code")
  val Unknown = Value("unknown")

  def withNameWithDefault(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)
}
