package com.twitter.auth.authorization

import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.Var

object RouteIdToScopeConfig {
  def apply(statsReceiver: StatsReceiver, env: String): RouteIdToScopeConfig = {
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
    new RouteIdToScopeConfig(configbusSubscriber, statsReceiver)
  }
}

class RouteIdToScopeConfig(
  configbusSubscriber: ConfigbusSubscriber,
  statsReceiver: StatsReceiver) {
  private val statsScope = statsReceiver.scope("routeId_to_scope")
  private val configChangeCounter = statsScope.counter("update_count")

  def watch(
    configFile: String = "/auth/oauth2/route_id_to_scope_mapping.json"
  ): Var[RouteIdToScopeMapping] = {
    //build the subscriber for RouteIdToScopeConfig
    val subscription = configbusSubscriber.subscribeAndPublish(
      path = configFile,
      initialValue = RouteIdToScopeMapping(Map.empty[String, Set[String]]),
      defaultValue = None
    )

    subscription.data.changes respond { _ =>
      configChangeCounter.incr()
    }
    subscription.data
  }
}

case class RouteIdToScopeMapping(mapping: Map[String, Set[String]]) {
  private val underlyingMapping: Map[String, Set[String]] = mapping

  def getRouteIdToScopeMapping(routeId: String): Set[String] = {
    underlyingMapping.getOrElse(routeId, Set.empty)
  }
}
