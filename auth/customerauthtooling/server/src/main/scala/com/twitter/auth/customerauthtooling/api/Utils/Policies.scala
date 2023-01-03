package com.twitter.auth.customerauthtooling.api.Utils

import com.twitter.auth.authorizationscope.AuthorizationScope
import com.twitter.auth.authorizationscope.AuthorizationScopesConfig
import com.twitter.auth.policy.DynamicAuthorizationScopesPolicy
import com.twitter.auth.policy.DynamicDataPermissionsPolicy
import com.twitter.finagle.stats.DefaultStatsReceiver
import com.twitter.tfe.core.routingng.NgRoute

object Policies {

  val Env = "prod"
  //val ConfigRoot = "/usr/local/config"
  val ConfigRoot = "/Users/akashp/workspace/config"
  //val RoutingConfigRoot = "/usr/local/config-routing"
  val RoutingConfigRoot = "/Users/akashp/workspace/config-routing"
  val CanaryRoutesPath = "/routing/tfe/ngroutes/canary"
  val ProductionRoutesPath = "/routing/tfe/ngroutes/production"
  val ScopeToDPMappingPath = "auth/oauth2/scope_to_dp_mapping.json"
  val ScopesPath = "auth/oauth2/scopes.json"
  val StatsReceiver = DefaultStatsReceiver.scope("configs")

  private val dynamicDataPermissionsPolicy =
    new DynamicDataPermissionsPolicy(StatsReceiver, ScopeToDPMappingPath, Env)

  private val dynamicAuthorizationScopesPolicy =
    new DynamicAuthorizationScopesPolicy(
      AuthorizationScopesConfig(StatsReceiver, Env).watch(ScopesPath))

  def getDataPermissionsByScope(scope: String): Set[Long] = {
    dynamicDataPermissionsPolicy.dataPermissionIds(scope)
  }

  def getAuthorizationScopeByScope(scope: String): Option[AuthorizationScope] = {
    dynamicAuthorizationScopesPolicy.authorizationScope(scope)
  }
  def getRouteIdToNgRouteMapping: Map[String, NgRoute] = {
    NgRouteConfigs.getRouteIdToNgRouteMapping
  }
}
