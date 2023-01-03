package com.twitter.auth.customerauthtooling.cli.commands.converters

import scala.util.matching.Regex

class OptStringConverter extends OptConverter[String] {
  private val strPattern: Regex = "^([0-9a-zA-Z-:>_/.]+)*$".r

  def convert(value: String): Option[String] = {
    value match {
      case "" => handleEmpty()
      case strPattern(_*) => Some(value)
      case _ => throw InvalidInputException(s"Unrecognized string value: $value")
    }
  }
}
