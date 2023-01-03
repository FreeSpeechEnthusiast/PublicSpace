package com.twitter.auth.apiverification.clients

import com.twitter.bijection.scrooge.TBinaryProtocol
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.mtls.authentication.ServiceIdentifier
import com.twitter.finagle.mtls.client.MtlsStackClient._
import com.twitter.finagle.ssl.OpportunisticTls
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.flightauth.thriftscala.{
  FlightauthService,
  GetAllAccessTokensByUserAndClientAppIdRequest,
  GetAllRefreshTokensByUserAndClientAppIdRequest,
  OAuth2AccessTokenAPIThrift,
  OAuth2RefreshTokenAPIThrift
}
import com.twitter.util.{Await, Duration}
import com.twitter.util.logging.Logger

object FlightAuthClient {
  val FlightAuthDest = "/s/flightauth/flightauth"
  val ApiVerificationClientId = "api-verification"
  val FlightAuth = "flightauth-auth-client"
  val ConnectTimeoutMS: Duration = 5000.milliseconds
  val RequestTimeoutMS: Duration = 5000.milliseconds
  val ServiceIdentifierName: String = "twtr:svc:passbird:passbird:prod:atla"

  private[this] val muxClient = {
    com.twitter.server.Init()
    ThriftMux.client
      .withClientId(ClientId(ApiVerificationClientId))
      .withLabel(FlightAuth)
      .withSession.acquisitionTimeout(ConnectTimeoutMS)
      .withRequestTimeout(RequestTimeoutMS)
      .withMutualTls(
        ServiceIdentifier.fromCommonName(ServiceIdentifierName).get
      )
      .withOpportunisticTls(OpportunisticTls.Required)
      .withProtocolFactory(new TBinaryProtocol.Factory)
      .newClient(FlightAuthDest, FlightAuth)
      .toService
  }

  private[this] val flightAuthClient = new FlightauthService.FinagledClient(muxClient)

  private[this] def getAllAccessTokensByUserAndClientAppId(
    userId: Long,
    clientApplicationId: Long
  ): Seq[OAuth2AccessTokenAPIThrift] = {
    Await
      .result(
        flightAuthClient.getAllAccessTokensByUserAndClientAppId(
          GetAllAccessTokensByUserAndClientAppIdRequest(
            userId = userId,
            clientApplicationId = clientApplicationId,
            filterInvalidated = Some(true))
        )).accessTokens
  }

  private[this] def getAllRefreshTokensByUserAndClientAppId(
    userId: Long,
    clientApplicationId: Long
  ): Seq[OAuth2RefreshTokenAPIThrift] = {
    Await
      .result(
        flightAuthClient.getAllRefreshTokensByUserAndClientAppId(
          GetAllRefreshTokensByUserAndClientAppIdRequest(
            userId = userId,
            clientApplicationId = clientApplicationId
          ))).refreshTokens
  }

  def getAndAssertAccessTokens(
    userId: Long,
    clientApplicationId: Long,
    expectedSize: Int,
    logger: Logger,
    statsReceiver: StatsReceiver
  ): Unit = {
    logger.info(
      s"FlightAuth client: getAllAccessTokensByUserAndClientAppId(userId: $userId, clientApplicationId: $clientApplicationId")
    val tokens = getAllAccessTokensByUserAndClientAppId(userId, clientApplicationId)
    if (tokens.size != expectedSize) {
      logger.info(
        s"FlightAuth client: getAllAccessTokensByUserAndClientAppId mismatched token size, expected: $expectedSize actual: ${tokens.size}.")
      statsReceiver
        .scope("flightauth_client").scope("get_all_access_tokens").counter("mismatch").incr()
    } else {
      logger.info(
        s"FlightAuth client: getAllAccessTokensByUserAndClientAppId matched token size $expectedSize.")
      statsReceiver
        .scope("flightauth_client").scope("get_all_access_tokens").counter("match").incr()
    }
  }

  def getAndAssertRefreshTokens(
    userId: Long,
    clientApplicationId: Long,
    expectedSize: Int,
    logger: Logger,
    statsReceiver: StatsReceiver
  ): Unit = {
    logger.info(
      s"FlightAuth client: getAllRefreshTokensByUserAndClientAppId(userId: $userId, clientApplicationId: $clientApplicationId")
    val tokens = getAllRefreshTokensByUserAndClientAppId(userId, clientApplicationId)
    if (tokens.size != expectedSize) {
      logger.info(
        s"FlightAuth client: getAllRefreshTokensByUserAndClientAppId mismatched token size, expected: $expectedSize actual: ${tokens.size}.")
      statsReceiver
        .scope("flightauth_client").scope("get_all_refresh_tokens").counter("mismatch").incr()
    } else {
      logger.info(
        s"FlightAuth client: getAllRefreshTokensByUserAndClientAppId matched token size $expectedSize.")
      statsReceiver
        .scope("flightauth_client").scope("get_all_refresh_tokens").counter("match").incr()
    }
  }
}
