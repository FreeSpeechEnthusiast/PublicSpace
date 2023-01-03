package com.twitter.auth.authentication.models

object AuthHeaderType extends Enumeration {
  type AuthHeaderType = Value
  val OAuth1, OAuth2 = Value
}
