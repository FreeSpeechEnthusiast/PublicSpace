package com.twitter.auth.authorization

import com.twitter.auth.policy.DynamicDataPermissionsPolicy
import com.twitter.finagle.stats.StatsReceiver

class DynamicDataPermissionLookup(
  statsReceiver: StatsReceiver,
  configPath: String,
  env: String,
  configRepoRootOpt: Option[String] = None)
    extends DataPermissionLookup {
  // Initialize a policy to get scope to DP mappings from configbus
  private val dynamicDataPermissionsPolicy =
    new DynamicDataPermissionsPolicy(statsReceiver, configPath, env, configRepoRootOpt)

  override def dataPermissionIds(scopes: Set[String]): Set[Long] = {
    scopes.flatMap(dynamicDataPermissionsPolicy.dataPermissionIds)
  }

  override def filterValidDPIds(dpIds: Set[Long]): Set[Long] = {
    dpIds.filter(dynamicDataPermissionsPolicy.isValidDPId(_))
  }

  override def areAllValidDPIds(dpIds: Set[Long]): Boolean = {
    val validDPs = filterValidDPIds(dpIds)
    validDPs.size == dpIds.size
  }
}
