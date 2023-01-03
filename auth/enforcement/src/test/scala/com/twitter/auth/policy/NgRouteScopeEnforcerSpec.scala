package com.twitter.auth.policy

import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.auth.authorization.AuthorizationScopeLookup
import com.twitter.auth.authorizationscope.AuthorizationScope
import org.mockito.Mockito.when
import com.twitter.auth.authorization.RouteToScopeLookup
import com.twitter.finagle.http.Request
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.tfe.core.decider.DeciderKey
import com.twitter.tfe.core.decider.StubTfeDecider
import com.twitter.tfe.core.routingng.NgRoute
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class NgRouteScopeEnforcerSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers
    with BeforeAndAfterEach {
  var decider = new StubTfeDecider

  private[this] val statsReceiver = new InMemoryStatsReceiver
  private[this] val authorizationScopeLookup = mock[AuthorizationScopeLookup]
  private[this] val routeToScopeLookup = mock[RouteToScopeLookup]
  private[this] val enforcer =
    new NgRouteScopeEnforcer(authorizationScopeLookup, routeToScopeLookup, statsReceiver)
  private[this] val scopesPath = "/2/tweets/{id}"
  private[this] val twoOauth2Scopes = Set("tweet.read", "users.read")
  private[this] val oneOauth2Scope = Set("tweet.read")
  private[this] val emptyScopes: Set[String] = Set()
  private[this] val threeTotalScopes = Set("tweet.read", "users.read", "read_scope")
  private[this] val fourTotalScopes =
    Set("tweet.read", "users.read", "read_scope", "read_write_scope")
  private[this] val oneOauth1Scope = Set("read_scope")
  private[this] val oAuth2AuthType = Some(AuthenticationType.Oauth2)
  private[this] val oAuth1AuthType = Some(AuthenticationType.Oauth1)
  private[this] val oAuth2AuthTypeStr = AuthenticationType.Oauth2.originalName
  private[this] val oAuth1AuthTypeStr = AuthenticationType.Oauth1.originalName

  val read_scope =
    AuthorizationScope(
      id = 0,
      name = "read_scope",
      internalGroup = "legacy",
      appCanViewDescription = None,
      appCanDoDescription = None,
      state = "production",
      ldapOwners = Set("data-products"),
      userRoles = Set(),
      applicableAuthTypes = Set("OAUTH_1")
    )
  val read_write_scope = read_scope.copy(
    id = 1,
    name = "read_write_scope"
  )
  val read_write_dm_scope = read_scope.copy(
    id = 2,
    name = "read_write_dm_scope"
  )
  val tweet_read_scope =
    AuthorizationScope(
      id = 3,
      name = "tweet.read",
      internalGroup = "oauth2",
      appCanViewDescription = Some("Can read tweets."),
      appCanDoDescription = None,
      state = "production",
      ldapOwners = Set("auth-platform"),
      userRoles = Set()
    )
  val users_read_scope =
    AuthorizationScope(
      id = 4,
      name = "users.read",
      internalGroup = "oauth2",
      appCanViewDescription = Some("Can view users."),
      appCanDoDescription = None,
      state = "production",
      ldapOwners = Set("auth-platform"),
      userRoles = Set()
    )

  private[this] val ngRouteWith2Oauth2Scopes = NgRoute.testRoute(
    normalizedPath = scopesPath,
    dataPermissions = Set(),
    scopes = twoOauth2Scopes
  )
  private[this] val ngRouteWithThreeTotalScopes = NgRoute.testRoute(
    normalizedPath = scopesPath,
    dataPermissions = Set(),
    scopes = threeTotalScopes
  )
  private[this] val ngRouteWithOneOauth1Scopes = NgRoute.testRoute(
    normalizedPath = scopesPath,
    dataPermissions = Set(),
    scopes = oneOauth1Scope
  )
  private[this] val ngRouteWithFourTotalScopes = NgRoute.testRoute(
    normalizedPath = scopesPath,
    dataPermissions = Set(),
    scopes = fourTotalScopes
  )
  private[this] val ngRouteWithEmptyScopes = NgRoute.testRoute(
    normalizedPath = scopesPath,
    dataPermissions = Set(),
    scopes = emptyScopes
  )

  private[this] val scopesRequest = {
    val request = Request.apply(ngRouteWith2Oauth2Scopes.normalizedPath)
    request
  }
  private[this] val scopesRequestExtraScopes = {
    val request = Request.apply(ngRouteWithThreeTotalScopes.normalizedPath)
    request
  }

  override def beforeEach(): Unit = {
    statsReceiver.clear()
  }

  test(
    "test scope enforcement success all oauth2 auth type when route has only oauth2 from mapping") {
    when(routeToScopeLookup.scopesByRouteId(ngRouteWith2Oauth2Scopes.id))
      .thenReturn(twoOauth2Scopes)
    when(
      authorizationScopeLookup
        .authorizationScope("tweet.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(tweet_read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("users.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(users_read_scope))
    Await.result(
      enforcer
        .enforce(
          ngRoute = ngRouteWith2Oauth2Scopes,
          authTypeOpt = oAuth2AuthType,
          decider = decider,
          request = scopesRequest,
          tokenScopes = twoOauth2Scopes)) mustBe true
    assert(statsReceiver.counter("ng_route_scope_enforcer", "scopes_allowed")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_mapping")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_route")() == 0)
  }

  test(
    "test scope enforcement success all oauth2 auth type when route has only oauth2 direct from route") {
    decider.setAvailability(DeciderKey.tfe_allow_retrieve_scope_from_ng_route.name, 10000)
    when(
      authorizationScopeLookup
        .authorizationScope("tweet.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(tweet_read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("users.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(users_read_scope))
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWith2Oauth2Scopes,
        authTypeOpt = oAuth2AuthType,
        decider = decider,
        request = scopesRequestExtraScopes,
        tokenScopes = twoOauth2Scopes)) mustBe true
    assert(statsReceiver.counter("ng_route_scope_enforcer", "scopes_allowed")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_mapping")() == 0)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_route")() == 1)
  }

  test("test scope enforcement success with empty route scopes and retrieve scopes from mapping") {
    when(routeToScopeLookup.scopesByRouteId(ngRouteWithEmptyScopes.id))
      .thenReturn(emptyScopes)
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWithEmptyScopes,
        authTypeOpt = oAuth2AuthType,
        decider = decider,
        request = scopesRequest,
        tokenScopes = twoOauth2Scopes)) mustBe true
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_mapping")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "empty_scopes")() == 1)
  }

  test("test scope enforcement success with empty route scopes and retrieve scopes from route") {
    decider.setAvailability(DeciderKey.tfe_allow_retrieve_scope_from_ng_route.name, 10000)
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWithEmptyScopes,
        authTypeOpt = oAuth2AuthType,
        decider = decider,
        request = scopesRequest,
        tokenScopes = twoOauth2Scopes)) mustBe true
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_route")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "empty_scopes")() == 1)
  }

  test(
    "test scope enforcement fail with missing oauth2 request scopes when route has 2 auth types from mapping") {
    when(routeToScopeLookup.scopesByRouteId(ngRouteWithThreeTotalScopes.id))
      .thenReturn(threeTotalScopes)
    when(
      authorizationScopeLookup
        .authorizationScope("tweet.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(tweet_read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("users.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(users_read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("read_scope", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(None)
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWithThreeTotalScopes,
        authTypeOpt = oAuth2AuthType,
        decider = decider,
        request = scopesRequest,
        tokenScopes = oneOauth2Scope)) mustBe false
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_mapping")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "scopes_rejected")() == 1)
  }

  test(
    "test scope enforcement fail with missing oauth2 request scopes when route has 2 auth types from route") {
    decider.setAvailability(DeciderKey.tfe_allow_retrieve_scope_from_ng_route.name, 10000)
    when(
      authorizationScopeLookup
        .authorizationScope("tweet.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(tweet_read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("users.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(users_read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("read_scope", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(None)
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWithThreeTotalScopes,
        authTypeOpt = oAuth2AuthType,
        decider = decider,
        request = scopesRequest,
        tokenScopes = oneOauth2Scope)) mustBe false
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_route")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "scopes_rejected")() == 1)
  }

  test(
    "test scope enforcement success using oauth2 request auth type when route has 2 auth types from mapping") {
    when(routeToScopeLookup.scopesByRouteId(ngRouteWithThreeTotalScopes.id))
      .thenReturn(threeTotalScopes)
    when(
      authorizationScopeLookup
        .authorizationScope("tweet.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(tweet_read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("users.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(users_read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("read_scope", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(None)
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWithThreeTotalScopes,
        authTypeOpt = oAuth2AuthType,
        decider = decider,
        request = scopesRequestExtraScopes,
        tokenScopes = twoOauth2Scopes)) mustBe true
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_mapping")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "scopes_allowed")() == 1)
  }

  test(
    "test scope enforcement success using oauth2 request auth type when route has 2 auth types from route") {
    decider.setAvailability(DeciderKey.tfe_allow_retrieve_scope_from_ng_route.name, 10000)
    when(routeToScopeLookup.scopesByRouteId(ngRouteWithThreeTotalScopes.id))
      .thenReturn(threeTotalScopes)
    when(
      authorizationScopeLookup
        .authorizationScope("tweet.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(tweet_read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("users.read", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(Some(users_read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("read_scope", None, Some(oAuth2AuthTypeStr), false))
      .thenReturn(None)
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWithThreeTotalScopes,
        authTypeOpt = oAuth2AuthType,
        decider = decider,
        request = scopesRequestExtraScopes,
        tokenScopes = twoOauth2Scopes)) mustBe true
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_route")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "scopes_allowed")() == 1)
  }

  test(
    "test scope enforcement success using only oauth1 type when route has 2 auth types from mapping") {
    when(routeToScopeLookup.scopesByRouteId(ngRouteWithThreeTotalScopes.id))
      .thenReturn(threeTotalScopes)
    when(
      authorizationScopeLookup
        .authorizationScope("tweet.read", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(None)
    when(
      authorizationScopeLookup
        .authorizationScope("users.read", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(None)
    when(
      authorizationScopeLookup
        .authorizationScope("read_scope", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(Some(read_scope))
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWithThreeTotalScopes,
        authTypeOpt = oAuth1AuthType,
        decider = decider,
        request = scopesRequestExtraScopes,
        tokenScopes = oneOauth1Scope)) mustBe true
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_mapping")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "scopes_allowed")() == 1)
  }
  test(
    "test scope enforcement success using oauth1 auth type when route has 2 auth types from route") {
    decider.setAvailability(DeciderKey.tfe_allow_retrieve_scope_from_ng_route.name, 10000)
    when(
      authorizationScopeLookup
        .authorizationScope("tweet.read", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(None)
    when(
      authorizationScopeLookup
        .authorizationScope("users.read", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(None)
    when(
      authorizationScopeLookup
        .authorizationScope("read_scope", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(Some(read_scope))
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWithThreeTotalScopes,
        authTypeOpt = oAuth1AuthType,
        decider = decider,
        request = scopesRequestExtraScopes,
        tokenScopes = oneOauth1Scope)) mustBe true
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_route")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "scopes_allowed")() == 1)
  }

  test(
    "test scope enforcement fail oauth1 auth type invalid request scopes when route has 2 auth types from mapping") {
    when(routeToScopeLookup.scopesByRouteId(ngRouteWithFourTotalScopes.id))
      .thenReturn(fourTotalScopes)
    when(
      authorizationScopeLookup
        .authorizationScope("tweet.read", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(None)
    when(
      authorizationScopeLookup
        .authorizationScope("users.read", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(None)
    when(
      authorizationScopeLookup
        .authorizationScope("read_scope", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(Some(read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("read_write_scope", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(Some(read_write_scope))
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWithFourTotalScopes,
        authTypeOpt = oAuth1AuthType,
        decider = decider,
        request = scopesRequest,
        tokenScopes = oneOauth1Scope)) mustBe false
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_mapping")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "scopes_rejected")() == 1)
  }

  test(
    "test scope enforcement fail oauth1 auth type invalid request scopes when route has 2 auth types from route") {
    decider.setAvailability(DeciderKey.tfe_allow_retrieve_scope_from_ng_route.name, 10000)
    when(
      authorizationScopeLookup
        .authorizationScope("tweet.read", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(None)
    when(
      authorizationScopeLookup
        .authorizationScope("users.read", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(None)
    when(
      authorizationScopeLookup
        .authorizationScope("read_scope", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(Some(read_scope))
    when(
      authorizationScopeLookup
        .authorizationScope("read_write_scope", None, Some(oAuth1AuthTypeStr), false))
      .thenReturn(Some(read_write_scope))
    Await.result(
      enforcer.enforce(
        ngRoute = ngRouteWithFourTotalScopes,
        authTypeOpt = oAuth1AuthType,
        decider = decider,
        request = scopesRequest,
        tokenScopes = oneOauth1Scope)) mustBe false
    assert(statsReceiver.counter("ng_route_scope_enforcer", "retrieve_scope_from_route")() == 1)
    assert(statsReceiver.counter("ng_route_scope_enforcer", "scopes_rejected")() == 1)
  }

}
