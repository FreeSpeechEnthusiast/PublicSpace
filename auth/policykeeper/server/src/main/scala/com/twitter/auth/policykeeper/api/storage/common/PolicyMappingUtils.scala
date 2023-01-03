package com.twitter.auth.policykeeper.api.storage.common

import scala.util.matching.Regex

object PolicyMappingUtils {
  val StaticMatchPattern: Regex = ("policy_" + PolicyId.Pattern).r
}
