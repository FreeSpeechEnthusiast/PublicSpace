package com.twitter.auth.customerauthtooling.cli.commands.converters

import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import picocli.CommandLine.ITypeConverter
import scala.util.matching.Regex

class AuthTypeSetConverter extends ITypeConverter[AuthTypeSetWrapper] {
  private val authTypeSetPattern: Regex = "^((\\w+),)*(\\w+)$".r

  def convert(value: String): AuthTypeSetWrapper = {
    value match {
      case authTypeSetPattern(_*) =>
        val authTypes = value.split(",").map(AuthenticationType.valueOf)
        if (authTypes.exists(_.isEmpty)) {
          throw InvalidInputException(s"One or more provided auth types is unknown: $value")
        } else {
          AuthTypeSetWrapper(authTypes.collect { case Some(v) => v }.toSet)
        }
      case _ => throw InvalidInputException(s"Unrecognized comma separated auth types set: $value")
    }
  }
}
