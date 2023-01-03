package com.twitter.auth.pasetoheaders.finagle

import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.configbus.subscriber.Subscription
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Duration

trait TssSubscriber {
  protected val tssPollingInterval: Duration = 5.minutes

  protected def tssSubscription(
    basePath: String,
    filename: String,
    stats: Option[StatsReceiver],
  ): Subscription[PrivateKeysConfiguration] = {
    val statsReceiver: StatsReceiver = stats.getOrElse(NullStatsReceiver)

    println("TSS will use base path: " + basePath)
    val configSource = PollingConfigSourceBuilder()
      .pollPeriod(tssPollingInterval)
      .statsReceiver(statsReceiver)
      .basePath(basePath)
      .build()

    val filenameToMonitor = filename
    println("TSS will monitor: " + filenameToMonitor)

    val configSubscriber: ConfigbusSubscriber =
      new ConfigbusSubscriber(
        statsReceiver = statsReceiver,
        configSource = configSource,
        rootPath = "")
    configSubscriber
      .subscribeAndPublish(
        path = filenameToMonitor,
        initialValue = PrivateKeysConfiguration.EMPTY,
        defaultValue = None)
  }

}
