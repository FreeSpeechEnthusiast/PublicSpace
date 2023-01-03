package com.twitter.auth.strato

import com.twitter.finagle.mtls.authentication.ServiceIdentifier
import com.twitter.finagle.ssl.OpportunisticTls
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.flightauth.thriftscala.GuestToken
import com.twitter.stitch.Stitch
import com.twitter.strato.client.Client
import com.twitter.strato.client.Strato
import com.twitter.strato.thrift.ScroogeConvImplicits._
import com.twitter.util.Duration
import com.twitter.util.Future

object StratoWrapper {
  def apply(
    serviceIdentifier: ServiceIdentifier,
    perRequestTimeout: Duration,
    requestTimeout: Duration,
    statsReceiver: StatsReceiver,
    opportunisticLevel: OpportunisticTls.Level = OpportunisticTls.Required
  ): Client = {
    Strato.client
      .withRequestTimeout(requestTimeout)
      .withPerRequestTimeout(perRequestTimeout)
      .withMutualTls(
        serviceIdentifier,
        opportunisticLevel = opportunisticLevel
      ) // replace with flightAuthServiceIdentifier
      .withStatsReceiver(statsReceiver)
      .build()
  }
}

class StratoWrapper(client: Client) {
  private lazy val guestTokenFetcher =
    client.fetcher[String, Unit, GuestToken]("flightauth/guestToken")
  private lazy val guestTokenPutter =
    client.putter[String, GuestToken]("flightauth/guestToken")
  private lazy val guestTokenDeleter =
    client.deleter[String]("flightauth/guestToken")

  def getGuestTokenById(key: String): Future[Option[GuestToken]] = {
    val guestTokenStitch = guestTokenFetcher.fetch(key)
    val guestTokenFutureFetch = Stitch.run(guestTokenStitch).map(_.v)
    guestTokenFutureFetch
  }

  def putGuestToken(key: String, guestToken: GuestToken): Future[Unit] = {
    Stitch.run(guestTokenPutter.put(key, guestToken))
  }

  def deleteGuestToken(key: String): Future[Unit] = {
    Stitch.run(guestTokenDeleter.delete(key))
  }

}
