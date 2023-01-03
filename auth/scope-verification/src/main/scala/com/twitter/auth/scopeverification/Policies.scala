package com.twitter.auth.scopeverification

import com.twitter.auth.authorization.DynamicRouteToScopeLookup
import com.twitter.auth.authorizationscope.AuthorizationScope
import com.twitter.auth.authorizationscope.AuthorizationScopesConfig
import com.twitter.auth.policy.DynamicAuthorizationScopesPolicy
import com.twitter.auth.policy.DynamicDataPermissionsPolicy
import com.twitter.tfe.core.routingng.NgRoute

object Policies extends ConfigUtils {

  private val RouteIdToScopeConfigPath = "/auth/oauth2/route_id_to_scope_mapping.json"
  private val Prod = "prod"

  private val dynamicDataPermissionsPolicy =
    new DynamicDataPermissionsPolicy(StatsReceiver, ScopeToDPMappingPath, Env)

  private val dynamicAuthorizationScopesPolicy =
    new DynamicAuthorizationScopesPolicy(
      AuthorizationScopesConfig(StatsReceiver, Env).watch(ScopesPath))

  private val dynamicRouteToScopeLookup =
    new DynamicRouteToScopeLookup(StatsReceiver, RouteIdToScopeConfigPath, Prod)

  def getScopesByRouteId(routeId: String): Set[String] = {
    dynamicRouteToScopeLookup.scopesByRouteId(routeId)
  }

  def getRouteIdToScopeMapping: Map[String, Set[String]] = {
    dynamicRouteToScopeLookup.configVar.sample().mapping
  }

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
