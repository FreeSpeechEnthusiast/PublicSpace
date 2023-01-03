package com.twitter.auth.pasetoheaders.passport

import com.twitter.auth.pasetoheaders.encryption.ClaimService
import com.twitter.auth.pasetoheaders.encryption.{KeyProvider => KeyProviderInterface}
import com.twitter.auth.pasetoheaders.encryption.ExtractedClaims
import com.twitter.auth.pasetoheaders.finagle.ConfigBusSource
import com.twitter.auth.pasetoheaders.finagle.ConfigBusSubscriber
import com.twitter.auth.pasetoheaders.finagle.PublicKeyProviderProxy
import com.twitter.auth.pasetoheaders.finagle.PublicKeysConfiguration
import com.twitter.auth.pasetoheaders.finagle.Service
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.auth.pasetoheaders.models.Passports
import com.twitter.decider.Feature
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.logging.Logger
import com.twitter.util.Duration
import com.twitter.conversions.DurationOps._

case class PassportExtractor(
  logger: Option[Logger],
  stats: Option[StatsReceiver],
  loggingEnabledDecider: Option[Feature] = None,
  override protected val configBusKeysSource: ConfigBusSource =
    ConfigBusSource.remote("auth/pasetoheaders"),
  override protected val configBusKeysFileName: String = "/public_keys.json",
  configWaitTimeout: Duration = 10.seconds)
    extends Service(
      serviceName = "PassportExtractor",
      logger = logger,
      stats = stats,
      loggingEnabledDecider = loggingEnabledDecider,
      configWaitTimeout = configWaitTimeout)
    with ConfigBusSubscriber {

  private val publicKeySubscription =
    configBusSubscription(stats = stats, initialValue = PublicKeysConfiguration.EMPTY)

  private val keyProvider: KeyProviderInterface = {
    awaitForConfigFileFromSubscription(publicKeySubscription)
    PublicKeyProviderProxy(
      logger = loggerConnection,
      stats = statsConnection,
      pasetoHeadersPublicKeySubscription = publicKeySubscription
    )
  }

  private val claimService = new ClaimService[Passports.Passport](
    "passport",
    classOf[Passports.Passport],
    keyProvider,
    loggerConnection,
    statsConnection
  )

  /**
   * Extracts claim from PASETO token
   *
   * @param token PASETO token
   *
   * @return claims content or Optional.empty if token invalid
   */
  def extractClaims(
    token: String
  ): Option[ExtractedClaims[Passports.Passport]] = {
    claimService.extractClaims(token)
  }

  /**
   * Extracts claim from PASETO token with optional integrity verification
   *
   * @param token PASETO token
   * @param verifyIntegrity Verify PASETO token integrity with costs of performance
   *
   * @return claims content or Optional.empty if token invalid
   */
  def extractClaims(
    token: String,
    verifyIntegrity: Boolean,
  ): Option[ExtractedClaims[Passports.Passport]] = {
    verifyIntegrity match {
      case true => claimService.extractClaims(token)
      case false => extractUnverifiedClaims(token)
    }
  }

  /**
   * Extract unverified claim from PASETO token.
   * Warning! This method is a part of adoption strategy go/passports-adoption-strategy.
   * It will be removed eventually.
   *
   * @param token PASETO token
   *
   * @return claims content or Optional.empty if token invalid
   */
  private def extractUnverifiedClaims(
    token: String
  ): Option[ExtractedClaims[Passports.Passport]] = {
    claimService.extractUnverifiedClaims(token)
  }
}
