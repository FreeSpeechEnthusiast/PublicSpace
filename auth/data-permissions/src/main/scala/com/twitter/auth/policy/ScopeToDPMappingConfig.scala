package com.twitter.auth.policy

import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.Var

object ScopeToDPMappingConfig {
  def apply(
    statsReceiver: StatsReceiver,
    env: String,
    configRepoRootOpt: Option[String] = None
  ): ScopeToDPMappingConfig = {
    val configSource = {
      val builder = PollingConfigSourceBuilder()
        .pollPeriod(30.seconds)
        .statsReceiver(statsReceiver)
        .timer(DefaultTimer)
      if (env == "local" || env == "localtostaging" || env == "test") {
        builder
          .basePath(new java.io.File(configRepoRootOpt.getOrElse(".")).getCanonicalPath)
          .versionFilePath(None)
          .build()
      } else {
        // this is prod, staging or devel environment
        builder.build()
      }
    }

    val configbusSubscriber = new ConfigbusSubscriber(statsReceiver, configSource, "")
    new ScopeToDPMappingConfig(configbusSubscriber, statsReceiver)
  }
}

class ScopeToDPMappingConfig(
  configbusSubscriber: ConfigbusSubscriber,
  statsReceiver: StatsReceiver) {
  private val statsScope = statsReceiver.scope("scope_to_dp")
  private val configChangeCounter = statsScope.counter("update_count")

  def watch(
    configFile: String = "auth/oauth2/scope_to_dp_mapping.json"
  ): Var[ScopeToDPMapping] = {
    // Build the subscriber for ScopeToDPMappingConfig
    val subscription = configbusSubscriber
      .subscribeAndPublish(
        path = configFile,
        initialValue = ScopeToDPMapping(Map.empty[String, Set[Int]]),
        defaultValue = None)

    subscription.data.changes respond { _ =>
      configChangeCounter.incr()
    }
    subscription.data
  }
}

case class ScopeToDPMapping(mapping: Map[String, Set[Int]]) {
  private val underlyingMapping: Map[String, Set[Int]] = mapping
  private val underlyingDpIds: Set[Int] = {
    var dps: Set[Int] = Set()
    mapping.values.foreach(s => dps ++= s)
    dps
  }

  def getScopeToDPMapping(scope: String): Set[Long] = {
    underlyingMapping.getOrElse(scope, Set.empty).map(a => a.toLong)
  }

  // TODO: Use DMO Service for DP ids
  def isValidDPId(dpId: Long): Boolean = underlyingDpIds(dpId.toInt)
}
