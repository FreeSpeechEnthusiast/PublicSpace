package com.twitter.auth.policykeeper.api.exceptions

import scala.util.control.NoStackTrace

sealed abstract class PolicyKeeperException(message: Option[String] = None)
    extends Exception(message.getOrElse("n/a"))
    with NoStackTrace

case class UnknownPolicyKeeperException(message: String)
    extends PolicyKeeperException(Some(message))
