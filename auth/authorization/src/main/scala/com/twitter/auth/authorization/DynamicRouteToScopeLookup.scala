package com.twitter.auth.authorization

import com.twitter.finagle.stats.StatsReceiver

object DynamicRouteToScopeLookup {
  val DefaultConfigPath = "/auth/oauth2/route_id_to_scope_mapping.json"
  val Prod = "prod"
  val Local = "local"
  val Test = "test"
}

class DynamicRouteToScopeLookup(statsReceiver: StatsReceiver, configPath: String, env: String)
    extends RouteToScopeLookup {
  // Initialize a policy to get routeId to Scope mappings from configbus
  private[auth] val configVar = RouteIdToScopeConfig(statsReceiver, env).watch(configPath)

  override def scopesByRouteId(routeId: String): Set[String] = {
    val allowedScopes: Set[String] = configVar.sample().getRouteIdToScopeMapping(routeId)
    // TO-DO: Convert the allowed scopes set into set of AuthorizationScope objects
    allowedScopes
  }
}
