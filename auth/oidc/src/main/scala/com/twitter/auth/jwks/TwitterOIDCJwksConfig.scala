package com.twitter.auth.jwks

import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.configbus.subscriber.Subscription
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.Await
import com.twitter.util.Duration
import com.twitter.util.Var

import java.util.concurrent.TimeUnit

object TwitterOIDCJwksConfig {
  def apply(
    statsReceiver: StatsReceiver,
    env: String,
    path: Option[String]
  ): TwitterOIDCJwksConfig = {
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
    new TwitterOIDCJwksConfig(
      configbusSubscriber,
      path.getOrElse("auth/oidc/twitter_oidc_public_keys.json"),
      statsReceiver)
  }
}

class TwitterOIDCJwksConfig(
  configbusSubscriber: ConfigbusSubscriber,
  configFile: String,
  statsReceiver: StatsReceiver) {
  private val statsScope = statsReceiver.scope("scopes")
  private val configChangeCounter = statsScope.counter("update_count")
  private val subscription: Subscription[TwitterOIDCJwks] =
    configbusSubscriber
      .subscribeAndPublish(
        path = configFile,
        initialValue = TwitterOIDCJwks(List.empty),
        defaultValue = None)

  // This will ensure server does not start if the config file is missing
  Await.ready(subscription.firstLoadCompleted, Duration(60, TimeUnit.SECONDS))

  def getTwitterOIDCJwksSub(): Subscription[TwitterOIDCJwks] = {
    subscription
  }

  def getTwitterOIDCJwks(
  ): Var[TwitterOIDCJwks] = {
    subscription.data.changes respond { _ =>
      configChangeCounter.incr()
    }
    subscription.data
  }
}

case class TwitterOIDCJwks(keys: List[TwitterOIDCPublicKey]) {
  def getPublickeys: List[TwitterOIDCPublicKey] = {
    keys
  }
}
