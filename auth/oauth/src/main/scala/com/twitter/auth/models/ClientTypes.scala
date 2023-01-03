package com.twitter.auth.models

object ClientTypes extends Enumeration {
  type ClientTypes = Value

  val ThirdPartyApp = Value("third_party_app")
  val ServiceClient = Value("service_client")
  val Unknown = Value("unknown")

  def withNameWithDefault(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)
}
