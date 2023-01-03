package com.twitter.auth.sso.service

import com.twitter.auth.sso.client.IdTokenIntegrityClient
import com.twitter.auth.sso.models.{IdToken, SsoProvider, SsoProviderInfo}
import com.twitter.finagle.stats.{Counter, NullStatsReceiver, StatsReceiver}
import com.twitter.servo.util.ExceptionCounter
import com.twitter.util.{Future, Try}

/**
 * This class is meant to be embedded as a library into an arbitrary service. It's responsible for:
 *
 * 1. Verifying and deserializing ID Tokens ( see https://openid.net/specs/openid-connect-core-1_0.html#IDToken)
 *    received from TOO apps during SSO Login.
 * 2. Communicating with SSO Providers to receive profile information from the token.
 */
class IdTokenIntegrityService(
  idTokenIntegrityClient: IdTokenIntegrityClient,
  statsReceiver: StatsReceiver = NullStatsReceiver) {
  import IdTokenIntegrityService._

  private val scopedStatsReceiver: StatsReceiver = statsReceiver.scope("id_token_service")
  private val getIdTokenScope: StatsReceiver = scopedStatsReceiver.scope("get_from_id_token")
  private val isIdTokenValidScope: StatsReceiver = scopedStatsReceiver.scope("is_id_token_valid")

  private val getIdTokenAttemptCounter: Counter = getIdTokenScope.counter(Attempt)
  private val getIdTokenSuccessCounter: Counter = getIdTokenScope.counter(Success)
  private val getIdTokenExceptionCounter: ExceptionCounter = new ExceptionCounter(getIdTokenScope)

  private val isIdTokenValidAttemptCounter: Counter = isIdTokenValidScope.counter(Attempt)
  private val isIdTokenValidTrueCounter: Counter = isIdTokenValidScope.counter(true.toString)
  private val isIdTokenValidFalseCounter: Counter = isIdTokenValidScope.counter(false.toString)
  private val isIdTokenValidExceptionCounter: ExceptionCounter = new ExceptionCounter(
    isIdTokenValidScope)

  /**
   * Returns true if the IdToken is valid for the given provider.
   */
  def isIdTokenValid(idToken: IdToken, ssoProvider: SsoProvider): Future[Boolean] = {
    isIdTokenValidAttemptCounter.incr()
    idTokenIntegrityClient
      .isIdTokenValid(idToken, ssoProvider).onSuccess { result =>
        if (result) {
          isIdTokenValidTrueCounter.incr()
        } else {
          isIdTokenValidFalseCounter.incr()
        }
      }
      .onFailure(isIdTokenValidExceptionCounter(_))
      .handle { case _: Throwable => false }
  }

  /**
   * Given the [[IdToken]] and the [[SsoProvider]], do the following:
   *  1. Validate & decrypt the given token.
   *  2. Extract and return any relevant info from the valid token.
   * */
  def getFromIdToken(idTokenProvider: SsoProvider, idToken: IdToken): Try[SsoProviderInfo] = {
    getIdTokenAttemptCounter.incr()
    Try(
      idTokenIntegrityClient.extractSsoProviderInfo(
        ssoProvider = idTokenProvider,
        idToken = idToken
      )
    ).onSuccess { _ =>
        getIdTokenSuccessCounter.incr()
      }.onFailure(getIdTokenExceptionCounter(_))
  }
}

object IdTokenIntegrityService {
  final val Attempt = "attempt"
  final val Success = "success"
}
