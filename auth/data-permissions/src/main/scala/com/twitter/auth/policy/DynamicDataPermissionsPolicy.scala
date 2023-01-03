package com.twitter.auth.policy

import com.twitter.finagle.stats.StatsReceiver

class DynamicDataPermissionsPolicy(
  statsReceiver: StatsReceiver,
  configFile: String,
  env: String,
  configRepoRootOpt: Option[String] = None)
    extends DataPermissionsPolicy {
  // Subscribe to `configFile` to get updates on scope to dp mapping
  private val configVar =
    ScopeToDPMappingConfig(statsReceiver, env, configRepoRootOpt).watch(configFile)

  override def dataPermissionIds(scope: String): Set[Long] = {
    configVar.sample().getScopeToDPMapping(scope)
  }

  override def isValidDPId(dpId: Long): Boolean = {
    configVar.sample().isValidDPId(dpId)
  }
}
