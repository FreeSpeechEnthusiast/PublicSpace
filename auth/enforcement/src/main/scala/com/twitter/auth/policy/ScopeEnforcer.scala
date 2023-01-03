package com.twitter.auth.policy

import com.twitter.auth.authorization.RouteToScopeLookup
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Future

class ScopeEnforcer(
  routeToScopeLookup: RouteToScopeLookup,
  statsReceiver: StatsReceiver) {

  private[this] val scopedReceiver = statsReceiver.scope("scope_enforcer")
  private[this] val scopeAllowedCounter = scopedReceiver.counter("scopes_allowed")
  private[this] val scopeRejectCounter = scopedReceiver.counter("scopes_rejected")
  private[this] val missingMappingCounter = scopedReceiver.counter("missing_mapping")

  /**
   * Determine if a routeId contains the expected scopes.
   *
   * @param routeId    contains a routeId as a String.
   * @param tokenScopes   contains  a set of expected scopes.
   * @return true if all the expected scopes are present in the routeId to Scope mapping.
   */
  def enforce(
    tokenScopes: Set[String],
    routeId: String
  ): Future[Boolean] = {

    // Lookup allowed scopes with routeId
    val routeScopes: Set[String] = routeToScopeLookup.scopesByRouteId(routeId)

    /**
     *  Given the set of token scopes, it returns true only if token scopes are
     *  greater than or equal to the route scopes in the mappings.
     */
    if (routeScopes.isEmpty) {
      missingMappingCounter.incr()
      scopeRejectCounter.incr()
      Future.False
    } else if (routeScopes.subsetOf(tokenScopes)) {
      scopeAllowedCounter.incr()
      Future.True
    } else {
      scopeRejectCounter.incr()
      Future.False
    }
  }
}
