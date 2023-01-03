package com.twitter.auth.authorization

import com.twitter.auth.authorizationscope.AuthorizationScopesConfig
import com.twitter.auth.policy.DynamicAuthorizationScopesPolicy
import com.twitter.finagle.stats.StatsReceiver

object DynamicAuthorizationScopeLookup {
  val TestScopeState = "test"
  val DefaultConfigPath = "auth/oauth2/scopes.json"
  val Prod = "prod"
  val Local = "local"
  val Test = "test"
}

class DynamicAuthorizationScopeLookup(
  statsReceiver: StatsReceiver,
  configPath: String,
  env: String)
    extends AuthorizationScopeLookup {

  override val dynamicAuthorizationScopesPolicy =
    new DynamicAuthorizationScopesPolicy(
      AuthorizationScopesConfig(statsReceiver, env).watch(configPath))

  override def authorizationScopes(tokenPrivileges: Set[String]): Set[String] = {
    dynamicAuthorizationScopesPolicy.authorizationScopes(tokenPrivileges)
  }
}
