package com.twitter.auth.apiverification

import com.google.inject.Module
import com.twitter.auth.apiverification.models.AccessTokenResponse
import com.twitter.finagle.http.Status
import com.twitter.finatra.mtls.app.Mtls
import com.twitter.util.security.Credentials
import com.twitter.inject.annotations.Flags
import com.twitter.inject.app.App
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.server.AdminHttpServer
import com.twitter.server.Stats
import java.io.File

object ApiVerificationAppMain extends ApiVerificationApp

class ApiVerificationApp extends App with Mtls with Stats with AdminHttpServer {
  import CommonFixtures._

  override val modules: Seq[Module] = Seq(
    StatsReceiverModule
  )

  flag[String]("redirect_uri", "https://twitter.com/", "Redirect URI for OAuth2 requests.")
  flag[String]("state", "state", "State for OAuth2 requests.")
  flag[String]("code_challenge", "challenge", "Code challenge for OAuth2 requests.")
  flag[String]("code_verifier", "challenge", "Code verifier for OAuth2 requests.")
  flag[String]("code_challenge_method", "plain", "Code challenge method for OAuth2 requests.")
  // This is app client id, which is NOT credential
  flag[String](
    "client_id",
    "TWMwYThvMU1jMmg0Z2RPZGZjUHE6MTpjaQ",
    "Client ID to run the test requests.")
  flag[Long]("user_id", 1176196242566574085L, "User ID")
  flag[Long]("client_application_id", 18755074L, "Client Application ID")

  // How to update it in tss?
  // => tss material edit /var/lib/tss/keys/flightauth/oauth2_adhoc_test_creds.yml
  val credentials = Credentials(
    new File("/var/lib/tss/keys/flightauth/oauth2_adhoc_test_creds.yml"))
  // Local Config
//    new File("auth/api-verification/src/main/resources/oauth2_adhoc_test_creds.yml"))

  private[this] val scoped = statsReceiver.scope("api_verification_app")
  private[this] val oauth2Scope = scoped.scope("oauth2")
  private[this] val v2ApiScope = scoped.scope("v2")

  override protected def run(): Unit = {
    // Flag Params
    val userId: Long = injector.instance[Long](Flags.named("user_id"))
    val clientApplicationId: Long = injector.instance[Long](Flags.named("client_application_id"))
    val clientId: String = injector.instance[String](Flags.named("client_id"))
    val redirectUri: String = injector.instance[String](Flags.named("redirect_uri"))
    val state: String = injector.instance[String](Flags.named("state"))
    val codeChallenge: String = injector.instance[String](Flags.named("code_challenge"))
    val codeVerifier: String = injector.instance[String](Flags.named("code_verifier"))
    val codeChallengeMethod: String =
      injector.instance[String](Flags.named("code_challenge_method"))
    // From TSS
    val csrfToken: String = credentials("csrf_token")
    val appOnlyToken: String = credentials("app_token")
    val sessionToken: String = credentials("session_token")
    // HTTP Client
    val client = HttpClientFactory.newApiClient(logger)

    while (true) {
      try {
        // OAuth2 Auth Code PKCE Flow WITH offline.access (Refresh Token)
        var accessTokenResponse: AccessTokenResponse = OAuth2ApiRunner.runOAuth2AuthCodePKCE(
          clientId = clientId,
          redirectUri = redirectUri,
          scopes = FULL_SCOPE_WITH_OFFLINE_ACCESS,
          state = state,
          codeChallenge = codeChallenge,
          codeVerifier = codeVerifier,
          codeChallengeMethod = codeChallengeMethod,
          csrfToken = csrfToken,
          appOnlyToken = appOnlyToken,
          sessionToken = sessionToken,
          client = client,
          userId = userId,
          clientApplicationId = clientApplicationId,
          expectedAccessTokenSize = 1,
          expectedRefreshTokenSize = 1,
          logger = logger,
          statsReceiver = oauth2Scope
        )
        // run all v2 APIs
        VNextApiRunner.runAllV2APIs(
          accessToken = accessTokenResponse.accessToken,
          client = client,
          logger = logger,
          expectedStatus = Status.Ok,
          statsReceiver = v2ApiScope
        )
        // OAuth2 Refresh Token Flow
        if (accessTokenResponse.refreshToken.isDefined) {
          accessTokenResponse = OAuth2ApiRunner.runExchangeRefreshToken(
            refreshToken = accessTokenResponse.refreshToken.get,
            clientId = clientId,
            client = client,
            userId = userId,
            clientApplicationId = clientApplicationId,
            expectedAccessTokenSize = 1,
            expectedRefreshTokenSize = 1,
            logger = logger,
            statsReceiver = oauth2Scope.scope("exchange_oauth2_refresh_token")
          )
        }
        // OAuth2 Revoke Token Flow
        OAuth2ApiRunner.runRevokeToken(
          tokenToRevoke = accessTokenResponse.accessToken,
          clientId = clientId,
          client = client,
          userId = userId,
          clientApplicationId = clientApplicationId,
          expectedAccessTokenSize = 0,
          expectedRefreshTokenSize = 0,
          logger = logger,
          statsReceiver = oauth2Scope.scope("revoke_oauth2_token")
        )
        // OAuth2 Auth Code PKCE Flow with limited scope
        val limitedScopeAccessTokenResponse: AccessTokenResponse =
          OAuth2ApiRunner.runOAuth2AuthCodePKCE(
            clientId = clientId,
            redirectUri = redirectUri,
            scopes = LIMITED_SCOPE,
            state = state,
            codeChallenge = codeChallenge,
            codeVerifier = codeVerifier,
            codeChallengeMethod = codeChallengeMethod,
            csrfToken = csrfToken,
            appOnlyToken = appOnlyToken,
            sessionToken = sessionToken,
            client = client,
            userId = userId,
            clientApplicationId = clientApplicationId,
            expectedAccessTokenSize = 1,
            expectedRefreshTokenSize = 1,
            logger = logger,
            statsReceiver = oauth2Scope
          )
        // run all v2 APIs with limited scopes
        VNextApiRunner.runAllV2APIs(
          accessToken = limitedScopeAccessTokenResponse.accessToken,
          client = client,
          logger = logger,
          expectedStatus = Status.Forbidden,
          statsReceiver = v2ApiScope
        )
        // OAuth2 Revoke Token Flow
        OAuth2ApiRunner.runRevokeToken(
          tokenToRevoke = limitedScopeAccessTokenResponse.accessToken,
          clientId = clientId,
          client = client,
          userId = userId,
          clientApplicationId = clientApplicationId,
          expectedAccessTokenSize = 0,
          expectedRefreshTokenSize = 0,
          logger = logger,
          statsReceiver = oauth2Scope.scope("revoke_oauth2_token")
        )
        // OAuth2 Auth Code PKCE Flow WITHOUT offline.access (Refresh Token)
        OAuth2ApiRunner.runOAuth2AuthCodePKCE(
          clientId = clientId,
          redirectUri = redirectUri,
          scopes = FULL_SCOPE_WITHOUT_OFFLINE_ACCESS,
          state = state,
          codeChallenge = codeChallenge,
          codeVerifier = codeVerifier,
          codeChallengeMethod = codeChallengeMethod,
          csrfToken = csrfToken,
          appOnlyToken = appOnlyToken,
          sessionToken = sessionToken,
          client = client,
          userId = userId,
          clientApplicationId = clientApplicationId,
          expectedAccessTokenSize = 1,
          expectedRefreshTokenSize = 0,
          logger = logger,
          statsReceiver = oauth2Scope
        )
      } catch {
        case e: Throwable =>
          logger.error(s"Exception: $e \n", e)
          statsReceiver.counter("exceptions").incr()
      }
      // one iteration every 15 min to avoid being rate limited
      Thread.sleep(15 * 60 * 1000)
    }
  }
}
