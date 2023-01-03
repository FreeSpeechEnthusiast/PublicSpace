package com.twitter.auth.customerauthtooling.cli.commands.converters

import scala.util.matching.Regex

class OptStringSetConverter extends OptConverter[StringSetWrapper] {
  override protected def handleEmpty(): Option[StringSetWrapper] = Some(StringSetWrapper(Set()))

  private val strSetPattern: Regex = "^(([0-9a-zA-Z-:>_/.]+),)*([0-9a-zA-Z-:>_/.]+)$".r

  def convert(value: String): Option[StringSetWrapper] = {
    value match {
      case strSetPattern(_*) => Some(StringSetWrapper(value.split(",").toSet))
      case "" => handleEmpty()
      case _ => throw InvalidInputException(s"Unrecognized comma separated string set: $value")
    }
  }
}
