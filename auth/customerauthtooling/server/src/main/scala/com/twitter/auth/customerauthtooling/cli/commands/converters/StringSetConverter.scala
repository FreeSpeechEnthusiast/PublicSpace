package com.twitter.auth.customerauthtooling.cli.commands.converters

import picocli.CommandLine.ITypeConverter
import scala.util.matching.Regex

class StringSetConverter extends ITypeConverter[StringSetWrapper] {
  private val strSetPattern: Regex = "^(([0-9a-zA-Z-:>_/.]+),)*([0-9a-zA-Z-:>_/.]+)$".r

  def convert(value: String): StringSetWrapper = {
    value match {
      case strSetPattern(_*) => StringSetWrapper(value.split(",").toSet)
      case _ => throw InvalidInputException(s"Unrecognized comma separated string set: $value")
    }
  }
}
