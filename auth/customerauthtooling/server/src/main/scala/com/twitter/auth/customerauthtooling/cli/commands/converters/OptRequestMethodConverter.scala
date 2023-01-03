package com.twitter.auth.customerauthtooling.cli.commands.converters

import com.twitter.auth.customerauthtooling.thriftscala.RequestMethod

class OptRequestMethodConverter extends OptConverter[RequestMethod] {
  def convert(value: String): Option[RequestMethod] = {
    value match {
      case "" => handleEmpty()
      case _ if RequestMethod.valueOf(value).isDefined => RequestMethod.valueOf(value)
      case _ => throw InvalidInputException(s"Unrecognized request method value: $value")
    }
  }
}
