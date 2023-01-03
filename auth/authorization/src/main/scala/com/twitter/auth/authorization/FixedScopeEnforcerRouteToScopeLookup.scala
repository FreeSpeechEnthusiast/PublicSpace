package com.twitter.auth.authorization

/**
 * Note: This implementation of RouteToScopeLookup should only be used for testing
 */
object FixedScopeEnforcerRouteToScopeLookup extends RouteToScopeLookup {

  private[this] val mapping = Map[String, Set[String]](
    "GET/2/tweets/{id}->cluster:des_apiservice_get_2_tweets_id_prod" -> Set(
      "tweet.read",
      "users.read"),
    "GET/2/users/{id}/following/spaces->cluster:des_apiservice_users_id_following_spaces_prod_mtls" -> Set(
      "users.read",
      "tweet.read",
      "space.read",
      "account.follows.read"
    ),
    "/2/test/{id}" -> Set()
  )

  override def scopesByRouteId(routeId: String): Set[String] =
    mapping.getOrElse(routeId, Set())
}
