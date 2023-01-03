package com.twitter.auth.scopeverification

import com.twitter.finagle.stats.DefaultStatsReceiver

trait ConfigUtils {

  val Env = "prod"
  val ConfigRoot = "/usr/local/config"
  val RoutingConfigRoot = "/usr/local/config-routing"
  val CanaryRoutesPath = "/routing/tfe/ngroutes/canary"
  val ProductionRoutesPath = "/routing/tfe/ngroutes/production"
  val ScopeToDPMappingPath = "auth/oauth2/scope_to_dp_mapping.json"
  val ScopesPath = "auth/oauth2/scopes.json"
  val StatsReceiver = DefaultStatsReceiver.scope("configs")

}
