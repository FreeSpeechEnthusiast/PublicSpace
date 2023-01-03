package com.twitter.auth.policy

import com.twitter.auth.authorization.FixedScopeEnforcerRouteToScopeLookup
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ScopeEnforcerSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers
    with BeforeAndAfterEach {

  private[this] val statsReceiver = new InMemoryStatsReceiver

  private[this] val enforcer =
    new ScopeEnforcer(FixedScopeEnforcerRouteToScopeLookup, statsReceiver)
  private[this] val testRouteId = "GET/2/tweets/{id}->cluster:des_apiservice_get_2_tweets_id_prod"
  private[this] val invalidRouteId = "GET/2/foo"
  private[this] val missingScopesRouteId = "/2/test/{id}"
  private[this] val SameTokenAndRouteScopes = Set("tweet.read", "users.read")
  private[this] val excessTokenScopesThanRouteScopes = Set("tweet.read", "users.read", "space.read")
  private[this] val emptyTokenScopes = Set("")
  private[this] val lessTokenScopesThanRouteScopes = Set("tweet.read")

  override def beforeEach(): Unit = {
    statsReceiver.clear()
  }

  test("test scope enforcement success") {
    Await.result(enforcer.enforce(SameTokenAndRouteScopes, testRouteId)) mustBe true
    statsReceiver.counters(Seq("scope_enforcer", "scopes_allowed")) mustEqual 1
  }

  test("Scope Enforcer returns true for token having more than required route scopes") {
    Await.result(enforcer.enforce(excessTokenScopesThanRouteScopes, testRouteId)) mustBe true
    statsReceiver.counters(Seq("scope_enforcer", "scopes_allowed")) mustEqual 1
  }

  test("ScopeEnforcer returns false for token having invalid scopes") {
    Await.result(enforcer.enforce(lessTokenScopesThanRouteScopes, testRouteId)) mustBe false
    statsReceiver.counters(Seq("scope_enforcer", "scopes_rejected")) mustEqual 1
  }

  test("ScopeEnforcer returns false for empty token scopes") {
    Await.result(enforcer.enforce(emptyTokenScopes, testRouteId)) mustBe false
    statsReceiver.counters(Seq("scope_enforcer", "scopes_rejected")) mustEqual 1
  }

  test("ScopeEnforcer returns false for missing mapping") {
    Await.result(enforcer.enforce(SameTokenAndRouteScopes, invalidRouteId)) mustBe false
    statsReceiver.counters(Seq("scope_enforcer", "missing_mapping")) mustEqual 1
    statsReceiver.counters(Seq("scope_enforcer", "scopes_rejected")) mustEqual 1
  }

  test("ScopeEnforcer returns false for empty scopes in mapping") {
    Await.result(enforcer.enforce(SameTokenAndRouteScopes, missingScopesRouteId)) mustBe false
    statsReceiver.counters(Seq("scope_enforcer", "missing_mapping")) mustEqual 1
    statsReceiver.counters(Seq("scope_enforcer", "scopes_rejected")) mustEqual 1
  }
}
