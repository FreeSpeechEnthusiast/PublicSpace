package com.twitter.auth.pasetoheaders.finagle

import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.configbus.subscriber.Subscription
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Duration
import com.twitter.util.Resource
import com.google.common.base.Charsets
import com.google.common.io.Files
import java.io.File
import java.io.FileNotFoundException

/**
 * ConfigBusSubscriber provides auto-updatable configuration from different file sources
 *
 * Currently supports:
 *
 * - local files ConfigBusSource.local
 *
 * - configbus using ConfigBusSource.remote
 *
 * - resources using ConfigBusSource.resource
 *
 * - tss using ConfigBusSource.tss
 */
trait ConfigBusSubscriber {
  protected val configBusKeysSource: ConfigBusSource = ConfigBusSource.remote("auth/pasetoheaders")
  protected val configBusKeysFileName = "/public_keys.json"
  protected def configBusKeysConfigFile(): String = {
    configBusKeysSource match {
      case ConfigBusSourceRemote(relativePath) =>
        relativePath + configBusKeysFileName
      case ConfigBusSourceTss(_) =>
        configBusKeysFileName
      case ConfigBusSourceResource(_) => configBusKeysFileName
      case ConfigBusSourceLocal(_) => configBusKeysFileName
    }
  }
  protected val configBusPollingInterval: Duration = 30.seconds
  protected val tssPollingInterval: Duration = 5.minutes

  protected def configBusSubscription[T](
    stats: Option[StatsReceiver],
    initialValue: T
  )(
    implicit m: Manifest[T]
  ): Subscription[T] = {
    val statsReceiver: StatsReceiver = stats.getOrElse(NullStatsReceiver)
    val configSource = configBusKeysSource match {
      case ConfigBusSourceRemote(_) =>
        PollingConfigSourceBuilder()
          .pollPeriod(configBusPollingInterval)
          .statsReceiver(statsReceiver)
          .build()
      case ConfigBusSourceTss(relativePath) =>
        PollingConfigSourceBuilder()
          .pollPeriod(tssPollingInterval)
          .statsReceiver(statsReceiver)
          .basePath("/var/lib/tss/keys/" + relativePath.stripPrefix("/"))
          .build()
      case ConfigBusSourceResource(resourcePath) =>
        // copy resource to temporary folder
        val tmpDir = Files.createTempDir()
        val file = new File(tmpDir, resourcePath + "/" + configBusKeysFileName.stripPrefix("/"))
        Files.createParentDirs(file)
        Files.touch(file)
        // try is required to keep the same behaviour with other all sources
        try {
          val resource = Resource(resourcePath + "/" + configBusKeysFileName.stripPrefix("/"))
          resource.withSource(source =>
            Files
              .asCharSink(file, Charsets.UTF_8)
              .write(source.mkString))
        } catch {
          case _: FileNotFoundException =>
        }
        // use temporary folder's absolute path
        PollingConfigSourceBuilder()
          .basePath(file.getAbsolutePath.stripSuffix(configBusKeysFileName))
          .pollPeriod(configBusPollingInterval)
          .statsReceiver(statsReceiver)
          .build()
      case ConfigBusSourceLocal(absolutePath) =>
        PollingConfigSourceBuilder()
          .basePath(absolutePath)
          .pollPeriod(configBusPollingInterval)
          .statsReceiver(statsReceiver)
          .build()
    }
    val configSubscriber: ConfigbusSubscriber =
      new ConfigbusSubscriber(
        statsReceiver = statsReceiver,
        configSource = configSource,
        rootPath = "")
    configSubscriber
      .subscribeAndPublish(
        path = configBusKeysConfigFile(),
        initialValue = initialValue,
        defaultValue = None)
  }
}
