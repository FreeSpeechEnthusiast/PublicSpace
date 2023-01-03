package com.twitter.auth.customerauthtooling.cli.commands.converters

class OptBooleanConverter extends OptConverter[Boolean] {
  def convert(value: String): Option[Boolean] = {
    value.toLowerCase() match {
      case "true" | "1" | "yes" | "y" => Some(true)
      case "false" | "0" | "no" | "n" => Some(false)
      case "" => handleEmpty()
      case _ => throw InvalidInputException(s"Unrecognized boolean value: $value")
    }
  }
}
