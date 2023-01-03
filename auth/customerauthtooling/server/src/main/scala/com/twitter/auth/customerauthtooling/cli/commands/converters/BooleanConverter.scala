package com.twitter.auth.customerauthtooling.cli
package commands.converters

import picocli.CommandLine.ITypeConverter

class BooleanConverter extends ITypeConverter[Boolean] {
  def convert(value: String): Boolean = {
    value.toLowerCase() match {
      case "true" | "1" | "yes" | "y" => true
      case "false" | "0" | "no" | "n" => false
      case _ => throw InvalidInputException(s"Unrecognized boolean value: $value")
    }
  }
}
