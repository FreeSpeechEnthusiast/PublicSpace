package com.twitter.auth.customerauthtooling.cli.commands.converters

import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType

case class AuthTypeSetWrapper(private val underlying: Set[AuthenticationType]) {
  def get(): Set[AuthenticationType] = {
    underlying
  }
}
