package com.twitter.auth.customerauthtooling.cli.commands.converters

import scala.util.matching.Regex

class OptIntConverter extends OptConverter[Int] {
  private val intPattern: Regex = "^([0-9]+)*$".r

  def convert(value: String): Option[Int] = {
    value match {
      case "" => handleEmpty()
      case intPattern(number) => Some(number.toInt)
      case _ => throw InvalidInputException(s"Unrecognized integer value: $value")
    }
  }
}
