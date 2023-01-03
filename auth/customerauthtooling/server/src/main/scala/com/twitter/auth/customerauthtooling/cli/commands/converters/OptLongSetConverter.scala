package com.twitter.auth.customerauthtooling.cli.commands.converters

import scala.util.matching.Regex

class OptLongSetConverter extends OptConverter[LongSetWrapper] {
  override protected def handleEmpty(): Option[LongSetWrapper] = Some(LongSetWrapper(Set()))

  private val intSetPattern: Regex = "^(([0-9]+),)*([0-9]+)$".r

  def convert(value: String): Option[LongSetWrapper] = {
    value match {
      case intSetPattern(_*) => Some(LongSetWrapper(value.split(",").map(_.toLong).toSet))
      case "" => handleEmpty()
      case _ => throw InvalidInputException(s"Unrecognized comma separated long set: $value")
    }
  }
}
