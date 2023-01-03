package com.twitter.auth.authorization

import scala.util.control.NoStackTrace

sealed abstract class AuthorizationException(message: Option[String] = None)
    extends Exception(message.getOrElse("n/a"))
    with NoStackTrace

case class InvalidPolicyException(message: String)
    extends AuthorizationException(Some(s"Invalid Policy: $message"))

case class InvalidPassportException(message: String)
    extends AuthorizationException(Some(s"Invalid Passport: $message"))
