package com.twitter.auth.policykeeper.api.storage.common

object PolicyId {
  val Pattern = "([a-zA-Z0-9_]+)"
}

/**
 * PolicyId wrapper
 */
final case class PolicyId(policyId: String)
