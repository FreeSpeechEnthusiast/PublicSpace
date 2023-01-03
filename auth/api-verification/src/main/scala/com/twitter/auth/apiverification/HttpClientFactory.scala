package com.twitter.auth.apiverification

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.Backoff
import com.twitter.finagle.service.RetryBudget
import com.twitter.auth.apiverification.CommonFixtures.{API_TWITTER_DOMAIN, TLS_PORT}
import com.twitter.util.Monitor
import com.twitter.util.logging.Logger

object HttpClientFactory {

  private[this] val budget: RetryBudget = RetryBudget(
    ttl = 10.seconds,
    minRetriesPerSec = 5,
    percentCanRetry = 0.1
  )

  def newApiClient(logger: Logger): Service[Request, Response] = {
    val monitor: Monitor = new Monitor {
      def handle(t: Throwable): Boolean = {
        logger.error(s"HTTP Client Exception: ${t.getStackTraceString}")
        true
      }
    }

    Http.client.withTransport
      .tls(API_TWITTER_DOMAIN)
      .withSession.acquisitionTimeout(5.seconds)
      .withRequestTimeout(10.seconds)
      .withSession.maxLifeTime(30.seconds)
      .withRetryBudget(budget)
      .withRetryBackoff(Backoff.const(10.seconds))
      .withMonitor(monitor)
      .newService(s"$API_TWITTER_DOMAIN:$TLS_PORT")
  }
}
