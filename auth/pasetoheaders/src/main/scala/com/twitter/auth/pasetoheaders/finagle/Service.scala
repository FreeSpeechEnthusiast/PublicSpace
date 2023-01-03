package com.twitter.auth.pasetoheaders.finagle

import com.twitter.configbus.subscriber.Subscription
import com.twitter.decider.Feature
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.logging.Logger
import com.twitter.util.Await
import com.twitter.util.Duration
import com.twitter.util.TimeoutException
import com.twitter.conversions.DurationOps._

class Service(
  serviceName: String,
  logger: Option[Logger],
  stats: Option[StatsReceiver],
  loggingEnabledDecider: Option[Feature] = None,
  configWaitTimeout: Duration = 10.seconds) {

  protected val scopedStats: Option[StatsReceiver] = stats.map(_.scope(serviceName))
  private val configLoadingTimeoutCounter =
    scopedStats.map(_.counter("config_loading_timeout"))

  protected lazy val loggerConnection: Option[FinagleLoggerProxy] = {
    logger match {
      case Some(l) => Some(FinagleLoggerProxy(l, loggingEnabledDecider))
      case None => None
    }
  }

  protected lazy val statsConnection: Option[FinagleStatsProxy] = {
    stats match {
      case Some(s) => Some(FinagleStatsProxy(s))
      case None => None
    }
  }

  /**
   * Wait for configbus / tss or other file-based subscription
   * Prevents service to be used without preloaded config file (keys)
   * If config file is missing or incorrect throws [[ServiceBootException]]
   */
  protected def awaitForConfigFileFromSubscription[T](
    subscription: Subscription[T]
  ): Unit = {
    try {
      Await.result(subscription.firstLoadCompleted, configWaitTimeout)
    } catch {
      case _: TimeoutException =>
        configLoadingTimeoutCounter.foreach(_.incr())
        throw ServiceBootException(serviceName, configWaitTimeout)
    }
  }
}

case class ServiceBootException(service: String, timeout: Duration) extends Throwable {
  override def getMessage: String =
    "service boot failed (" + service + "): config is not loaded within requested timeout (" + timeout
      .toString() + ")"
}
