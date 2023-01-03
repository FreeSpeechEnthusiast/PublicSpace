package com.twitter.auth.policy

import com.twitter.tfe.core.routingng.NgRoute
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.auth.authorization.AuthorizationScopeLookup
import com.twitter.auth.authorization.RouteToScopeLookup
import com.twitter.finagle.http.Request
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.tfe.core.decider.DeciderKey
import com.twitter.tfe.core.decider.TfeDecider
import com.twitter.util.Future

class NgRouteScopeEnforcer(
  authorizationScopeLookup: AuthorizationScopeLookup,
  routeToScopeLookup: RouteToScopeLookup,
  statsReceiver: StatsReceiver) {

  private[this] val scopedReceiver = statsReceiver.scope("ng_route_scope_enforcer")
  private[this] val emptyScopesCounter = scopedReceiver.counter("empty_scopes")
  private[this] val scopeAllowedCounter = scopedReceiver.counter("scopes_allowed")
  private[this] val scopeRejectCounter = scopedReceiver.counter("scopes_rejected")
  private[this] val retrieveScopeFromRouteCounter =
    scopedReceiver.counter("retrieve_scope_from_route")
  private[this] val retrieveScopeFromMappingCounter =
    scopedReceiver.counter("retrieve_scope_from_mapping")

  /**
   * @return true if route scopes are subset of request scopes
   */

  def enforce(
    ngRoute: NgRoute,
    authTypeOpt: Option[AuthenticationType] = None,
    decider: TfeDecider,
    request: Request,
    tokenScopes: Set[String]
  ): Future[Boolean] = {
    val routeScopes =
      decider.decide(DeciderKey.tfe_allow_retrieve_scope_from_ng_route.name, request) match {
        case true =>
          retrieveScopeFromRouteCounter.incr()
          ngRoute.scopes
        case _ =>
          retrieveScopeFromMappingCounter.incr()
          routeToScopeLookup.scopesByRouteId(ngRoute.id)
      }

    val routeScopesByAuthType: Set[String] = routeScopes.flatMap(scope =>
      authorizationScopeLookup
        .authorizationScope(
          tokenPrivilege = scope,
          internalGroup = None,
          applicableAuthTypes = authTypeOpt.map(_.originalName),
          includeTestStateScope = false
        ).map(_.name))

    // skip scope enforcement if no scopes annotated on route
    if (routeScopesByAuthType.isEmpty) {
      emptyScopesCounter.incr()
      Future.True
    } else if (routeScopesByAuthType.subsetOf(tokenScopes)) {
      scopeAllowedCounter.incr()
      Future.True
    } else {
      scopeRejectCounter.incr()
      Future.False
    }
  }
}
