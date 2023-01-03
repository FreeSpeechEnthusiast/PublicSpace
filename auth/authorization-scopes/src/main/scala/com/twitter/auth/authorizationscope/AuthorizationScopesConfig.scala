package com.twitter.auth.authorizationscope

import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Await, Var}

object AuthorizationScopesConfig {
  def apply(statsReceiver: StatsReceiver, env: String): AuthorizationScopesConfig = {
    val configSource = {
      val builder = PollingConfigSourceBuilder()
        .pollPeriod(30.seconds)
        .statsReceiver(statsReceiver)
        .timer(DefaultTimer)
      if (env == "local" || env == "localtostaging" || env == "test") {
        builder
          .basePath(new java.io.File(".").getCanonicalPath)
          .versionFilePath(None)
          .build()
      } else {
        // this is prod, staging or devel environment
        builder.build()
      }
    }

    val configbusSubscriber = new ConfigbusSubscriber(statsReceiver, configSource, "")
    new AuthorizationScopesConfig(configbusSubscriber, statsReceiver)
  }
}

class AuthorizationScopesConfig(
  configbusSubscriber: ConfigbusSubscriber,
  statsReceiver: StatsReceiver) {
  private val statsScope = statsReceiver.scope("scopes")
  private val configChangeCounter = statsScope.counter("update_count")

  def getLatestScopes(
    configFile: String = "auth/oauth2/scopes.json"
  ): List[AuthorizationScope] = {
    val subscriber = configbusSubscriber
      .subscribeAndPublish(
        path = configFile,
        initialValue = AuthorizationScopesMap(List.empty),
        defaultValue = None)
    Await.ready(subscriber.firstLoadCompleted)
    subscriber.latest.scopesList
  }

  def watch(
    configFile: String = "auth/oauth2/scopes.json"
  ): Var[AuthorizationScopesMap] = {
    // Build the subscriber for Scope listing
    val subscription = configbusSubscriber
      .subscribeAndPublish(
        path = configFile,
        initialValue = AuthorizationScopesMap(List.empty),
        defaultValue = None)

    subscription.data.changes respond { _ =>
      configChangeCounter.incr()
    }
    subscription.data
  }
}

case class AuthorizationScopesMap(scopesList: List[AuthorizationScope]) {
  private val underlyingList = scopesList
  private val underlyingMap: Map[String, AuthorizationScope] = {
    scopesList match {
      case null => Map.empty
      case _ => scopesList.map(scope => (scope.name.toLowerCase(), scope)).toMap
    }
  }

  def getScope(scopeName: String): AuthorizationScope = {
    underlyingMap.getOrElse(scopeName, null)
  }

  def getScopesList: List[AuthorizationScope] = {
    underlyingList
  }

  def getScopesMap: Map[String, AuthorizationScope] = {
    underlyingMap
  }

}
