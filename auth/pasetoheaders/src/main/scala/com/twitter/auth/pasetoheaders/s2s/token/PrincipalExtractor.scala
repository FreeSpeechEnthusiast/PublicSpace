package com.twitter.auth.pasetoheaders.s2s.token

import com.twitter.auth.pasetoheaders.encryption.ClaimService
import com.twitter.auth.pasetoheaders.encryption.ExtractedClaims
import com.twitter.auth.pasetoheaders.encryption.{KeyProvider => KeyProviderInterface}
import com.twitter.auth.pasetoheaders.finagle.ConfigBusSource
import com.twitter.auth.pasetoheaders.finagle.ConfigBusSubscriber
import com.twitter.auth.pasetoheaders.finagle.PublicKeyProviderProxy
import com.twitter.auth.pasetoheaders.finagle.PublicKeysConfiguration
import com.twitter.auth.pasetoheaders.finagle.Service
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.auth.pasetoheaders.s2s.models.Principals
import com.twitter.conversions.DurationOps._
import com.twitter.decider.Feature
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.logging.Logger
import com.twitter.util.Duration

/**
 * Builds an extractor service to extract and validate principal tokens
 *
 * By default, PrincipalExtractor will attempt to load public keys from ConfigBus platform-security/principal-tokens/public_keys.json.
 * On production env ConfigBus (ConfigBusSource.remote(...)) must be used.
 * In unit tests and on local machine, ConfigBusSource.resource("principal-tokens") can be used.
 *
 * @param logger
 * @param stats
 * @param loggingEnabledDecider
 * @param publicKeysSourceDir
 * @param publicKeyFileName
 * @param configWaitTimeout
 */
class PrincipalExtractor(
  logger: Option[Logger],
  stats: Option[StatsReceiver],
  loggingEnabledDecider: Option[Feature] = None,
  publicKeysSourceDir: ConfigBusSource =
    ConfigBusSource.remote("platform-security/principal-tokens"),
  publicKeyFileName: String = "/public_keys.json",
  configWaitTimeout: Duration = 10.seconds)
    extends Service(
      serviceName = "PrincipalExtractor",
      logger = logger,
      stats = stats,
      loggingEnabledDecider = loggingEnabledDecider,
      configWaitTimeout = configWaitTimeout)
    with ConfigBusSubscriber {

  override protected val configBusKeysSource = publicKeysSourceDir
  override protected val configBusKeysFileName = publicKeyFileName

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

  private val claimService = new ClaimService[Principals.Principal](
    "principal",
    classOf[Principals.Principal],
    keyProvider,
    loggerConnection,
    statsConnection
  )

  /**
   * Extracts claim from PASETO token
   *
   * @param token PASETO token
   *
   * @return claims content or None if token invalid
   */
  def extractClaims(
    token: String
  ): Option[ExtractedClaims[Principals.Principal]] = {
    claimService.extractClaims(token)
  }

  /**
   * Extracts claim from PASETO token with optional integrity verification
   *
   * @param token PASETO token
   * @param verifyIntegrity Verify PASETO token integrity with costs of performance
   *
   * @return claims content or None if token invalid
   */
  def extractClaims(
    token: String,
    verifyIntegrity: Boolean,
  ): Option[ExtractedClaims[Principals.Principal]] = {
    verifyIntegrity match {
      case true => claimService.extractClaims(token)
      case false => extractUnverifiedClaims(token)
    }
  }

  /**
   * Extract unverified claim from PASETO token.
   *
   * @param token PASETO token
   *
   * @return claims content or None if token invalid
   */
  private def extractUnverifiedClaims(
    token: String
  ): Option[ExtractedClaims[Principals.Principal]] = {
    claimService.extractUnverifiedClaims(token)
  }
}
