package com.twitter.auth.apiverification

import com.twitter.auth.apiverification.clients.FlightAuthClient
import com.twitter.auth.apiverification.models.AccessTokenResponse
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Await
import com.twitter.util.logging.Logger

object OAuth2ApiRunner {

  def runOAuth2AuthCodePKCE(
    clientId: String,
    redirectUri: String,
    scopes: String,
    state: String,
    codeChallenge: String,
    codeVerifier: String,
    codeChallengeMethod: String,
    csrfToken: String,
    appOnlyToken: String,
    sessionToken: String,
    client: Service[Request, Response],
    userId: Long,
    clientApplicationId: Long,
    expectedAccessTokenSize: Int,
    expectedRefreshTokenSize: Int,
    logger: Logger,
    statsReceiver: StatsReceiver
  ): AccessTokenResponse = {
    // Create Auth Code Request
    var authCode = runCreateOAuth2AuthCode(
      clientId = clientId,
      redirectUri = redirectUri,
      scopes = scopes,
      state = state,
      codeChallenge = codeChallenge,
      codeChallengeMethod = codeChallengeMethod,
      csrfToken = csrfToken,
      appOnlyToken = appOnlyToken,
      sessionToken = sessionToken,
      client = client,
      logger = logger,
      statsReceiver = statsReceiver.scope("create_oauth2_auth_code")
    )
    // Approval Auth Code Request
    authCode = runApproveOAuth2AuthCode(
      authCode = authCode,
      csrfToken = csrfToken,
      appOnlyToken = appOnlyToken,
      sessionToken = sessionToken,
      client = client,
      logger = logger,
      statsReceiver = statsReceiver.scope("approve_oauth2_auth_code")
    )
    // Exchange Access Token Request
    val accessTokenResponse = OAuth2ApiRunner.runExchangeAccessToken(
      authCode = authCode,
      clientId = clientId,
      redirectUri = redirectUri,
      codeVerifier = codeVerifier,
      client = client,
      logger = logger,
      statsReceiver = statsReceiver.scope("exchange_oauth2_access_token")
    )
    // Verify access token response
    OAuth2ApiRunner.assertAccessTokenResponse(
      accessTokenResponse = accessTokenResponse,
      expectRefreshToken = expectedRefreshTokenSize > 0,
      logger = logger,
      statsReceiver = statsReceiver
    )
    // Verify access token & refresh token counts against FlightAuth
    assertOAuth2Tokens(
      userId = userId,
      clientApplicationId = clientApplicationId,
      expectedAccessTokenSize = expectedAccessTokenSize,
      expectedRefreshTokenSize = expectedRefreshTokenSize,
      logger = logger,
      statsReceiver = statsReceiver
    )
    accessTokenResponse
  }

  def runCreateOAuth2AuthCode(
    clientId: String,
    redirectUri: String,
    scopes: String,
    state: String,
    codeChallenge: String,
    codeChallengeMethod: String,
    csrfToken: String,
    appOnlyToken: String,
    sessionToken: String,
    client: Service[Request, Response],
    logger: Logger,
    statsReceiver: StatsReceiver
  ): String = {
    logger.info(s"Request: OAuth2 Create Auth Code.")
    // Create Auth Code Request
    val request = HttpRequestHelper.createAuthCodeRequest(
      clientId,
      redirectUri,
      scopes,
      state,
      codeChallenge,
      codeChallengeMethod,
      csrfToken,
      appOnlyToken,
      sessionToken)
    val response: Response = Await.result(client(request))
    val status = response.status

    HttpRequestHelper.assertStatusCode(Status.Ok, status, statsReceiver)
    logger.info(s"Response: OAuth2 Create Auth Code HTTP Status: $status.")
    HttpRequestHelper.getAuthCodeFromCreateAuthCodeResponse(response)
  }

  def runApproveOAuth2AuthCode(
    authCode: String,
    csrfToken: String,
    appOnlyToken: String,
    sessionToken: String,
    client: Service[Request, Response],
    logger: Logger,
    statsReceiver: StatsReceiver
  ): String = {
    logger.info(s"Request: OAuth2 Approve Auth Code.")

    val request =
      HttpRequestHelper.approveAuthCodeRequest(authCode, csrfToken, appOnlyToken, sessionToken)
    val response: Response = Await.result(client(request))
    val status = response.status

    HttpRequestHelper.assertStatusCode(Status.Ok, status, statsReceiver)
    logger.info(s"Response: OAuth2 Approve Auth Code HTTP Status: $status.")
    authCode
  }

  def runExchangeAccessToken(
    authCode: String,
    clientId: String,
    redirectUri: String,
    codeVerifier: String,
    client: Service[Request, Response],
    logger: Logger,
    statsReceiver: StatsReceiver
  ): AccessTokenResponse = {
    logger.info(s"Request: OAuth2 Exchange Access Token.")

    val request =
      HttpRequestHelper.accessTokenRequest(authCode, clientId, redirectUri, codeVerifier)
    val response: Response = Await.result(client(request))
    val status = response.status
    val accessToken = HttpRequestHelper.getAccessTokenFromResponse(response)
    val refreshToken = HttpRequestHelper.getRefreshTokenFromResponse(response)

    HttpRequestHelper.assertStatusCode(Status.Ok, status, statsReceiver)
    logger.info(s"Response: OAuth2 Exchange Access Token HTTP Status: ${response.status}.")
    AccessTokenResponse(accessToken, refreshToken)
  }

  def runExchangeRefreshToken(
    refreshToken: String,
    clientId: String,
    client: Service[Request, Response],
    userId: Long,
    clientApplicationId: Long,
    expectedAccessTokenSize: Int,
    expectedRefreshTokenSize: Int,
    logger: Logger,
    statsReceiver: StatsReceiver
  ): AccessTokenResponse = {
    logger.info(s"Request: OAuth2 Exchange Refresh Token.")

    val request = HttpRequestHelper.refreshTokenRequest(refreshToken, clientId)
    val response: Response = Await.result(client(request))
    val status = response.status

    // Refreshed Tokens
    val newAccessToken = HttpRequestHelper.getAccessTokenFromResponse(response)
    val newRefreshToken = HttpRequestHelper.getRefreshTokenFromResponse(response)

    HttpRequestHelper.assertStatusCode(Status.Ok, status, statsReceiver)
    logger.info(s"Response: OAuth2 Exchange Refresh Token HTTP Status: $status.")

    val accessTokenResponse = AccessTokenResponse(newAccessToken, newRefreshToken)

    // Verify access token response
    OAuth2ApiRunner.assertAccessTokenResponse(
      accessTokenResponse = accessTokenResponse,
      expectRefreshToken = expectedRefreshTokenSize > 0,
      logger = logger,
      statsReceiver = statsReceiver
    )
    // Verify access token & refresh token counts against FlightAuth
    assertOAuth2Tokens(
      userId = userId,
      clientApplicationId = clientApplicationId,
      expectedAccessTokenSize = expectedAccessTokenSize,
      expectedRefreshTokenSize = expectedRefreshTokenSize,
      logger = logger,
      statsReceiver = statsReceiver
    )

    accessTokenResponse
  }

  def runRevokeToken(
    tokenToRevoke: String,
    clientId: String,
    client: Service[Request, Response],
    userId: Long,
    clientApplicationId: Long,
    expectedAccessTokenSize: Int,
    expectedRefreshTokenSize: Int,
    logger: Logger,
    statsReceiver: StatsReceiver
  ): Unit = {
    logger.info(s"Request: OAuth2 Revoke Token.")

    val request =
      HttpRequestHelper.revokeTokenRequest(tokenToRevoke = tokenToRevoke, clientId = clientId)
    val response: Response = Await.result(client(request))
    val status = response.status

    HttpRequestHelper.assertStatusCode(Status.Ok, status, statsReceiver)
    logger.info(s"Response: OAuth2 Revoke Token HTTP Status: $status.")

    // Verify access token & refresh token counts against FlightAuth
    assertOAuth2Tokens(
      userId = userId,
      clientApplicationId = clientApplicationId,
      expectedAccessTokenSize = expectedAccessTokenSize,
      expectedRefreshTokenSize = expectedRefreshTokenSize,
      logger = logger,
      statsReceiver = statsReceiver
    )
  }

  def assertOAuth2Tokens(
    userId: Long,
    clientApplicationId: Long,
    expectedAccessTokenSize: Int,
    expectedRefreshTokenSize: Int,
    logger: Logger,
    statsReceiver: StatsReceiver
  ): Unit = {
    // Give it a few seconds due to eventually consistency
    Thread.sleep(10 * 1000)
    // Verify access token & refresh token counts against FlightAuth
    FlightAuthClient.getAndAssertAccessTokens(
      userId = userId,
      clientApplicationId = clientApplicationId,
      expectedSize = expectedAccessTokenSize,
      logger = logger,
      statsReceiver = statsReceiver)
    FlightAuthClient.getAndAssertRefreshTokens(
      userId = userId,
      clientApplicationId = clientApplicationId,
      expectedSize = expectedRefreshTokenSize,
      logger = logger,
      statsReceiver = statsReceiver)
  }

  def assertAccessTokenResponse(
    accessTokenResponse: AccessTokenResponse,
    expectRefreshToken: Boolean,
    logger: Logger,
    statsReceiver: StatsReceiver
  ): Unit = {
    if (expectRefreshToken && accessTokenResponse.refreshToken.isEmpty) {
      logger.error("Expected a refresh token but got none.")
      statsReceiver.counter("unexpected_refresh_token").incr()
    } else if (!expectRefreshToken && accessTokenResponse.refreshToken.isDefined) {
      logger.error("Expected no refresh token but got one.")
      statsReceiver.counter("unexpected_refresh_token").incr()
    }
  }
}
