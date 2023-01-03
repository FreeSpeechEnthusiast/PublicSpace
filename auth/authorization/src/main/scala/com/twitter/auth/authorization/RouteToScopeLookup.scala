package com.twitter.auth.authorization

trait RouteToScopeLookup {

  /**
   * Retrieve policy mappings from Route Id to Scopes.
   *
   * @param routeId routeId
   *
   * @return set of AuthorizationScope objects.
   */
  def scopesByRouteId(routeId: String): Set[String]

}
