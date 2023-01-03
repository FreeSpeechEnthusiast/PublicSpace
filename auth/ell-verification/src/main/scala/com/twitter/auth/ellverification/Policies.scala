package com.twitter.auth.ellverification

import com.twitter.auth.authorizationscope.AuthorizationScope
import com.twitter.auth.authorizationscope.AuthorizationScopesConfig
import com.twitter.auth.policy.DynamicAuthorizationScopesPolicy
import com.twitter.auth.policy.DynamicDataPermissionsPolicy
import com.twitter.tfe.core.routingng.NgRoute

object Policies extends ConfigUtils {

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
