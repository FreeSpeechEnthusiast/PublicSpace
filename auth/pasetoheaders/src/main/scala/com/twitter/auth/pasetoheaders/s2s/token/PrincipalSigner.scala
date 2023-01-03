package com.twitter.auth.pasetoheaders.s2s.token

import com.twitter.auth.pasetoheaders.encryption.SigningService
import com.twitter.auth.pasetoheaders.encryption.{KeyProvider => KeyProviderInterface}
import com.twitter.auth.pasetoheaders.finagle.ConfigBusSource
import com.twitter.auth.pasetoheaders.finagle.ConfigBusSubscriber
import com.twitter.auth.pasetoheaders.finagle.PrivateKeyProviderProxy
import com.twitter.auth.pasetoheaders.finagle.PrivateKeysConfiguration
import com.twitter.auth.pasetoheaders.finagle.Service
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.auth.pasetoheaders.s2s.models.Principals
import com.twitter.decider.Feature
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.logging.Logger
import java.util.concurrent.atomic.AtomicInteger

/**
 * Builds a signer service to mint principal tokens
 *
 * By default, PrincipalSigner will attempt to load private keys from TSS principal-tokens/unknownEnv/unknownIssuer/private_keys.json.
 * On production env TSS (ConfigBusSource.tss(...)) must be used.
 * In unit tests and on local machine, ConfigBusSource.resource("principal-tokens") can be used.
 *
 * @param logger
 * @param stats
 * @param loggingEnabledDecider
 * @param brokerServiceEnv Sets token's issuer environment. Required for key identification
 * @param brokerServiceName Sets token's issuer name. Required for key identification
 * @param privateKeysSourceDir
 * @param privateKeyFileName
 * @param tokenTtlSec Token TTL in seconds
 */
class PrincipalSigner(
  logger: Option[Logger],
  stats: Option[StatsReceiver],
  loggingEnabledDecider: Option[Feature] = None,
  brokerServiceEnv: String = "unknownEnv",
  brokerServiceName: String = "unknownIssuer",
  privateKeysSourceDir: ConfigBusSource =
    ConfigBusSource.tss("principal-tokens/unknownEnv/unknownService"),
  privateKeyFileName: String = "/private_keys.json",
  tokenTtlSec: Long = 300L)
    extends Service(
      serviceName = "PrincipalSigner",
      logger = logger,
      stats = stats,
      loggingEnabledDecider = loggingEnabledDecider)
    with ConfigBusSubscriber {

  override protected val configBusKeysSource = privateKeysSourceDir
  override protected val configBusKeysFileName = privateKeyFileName

  scopedStats.foreach {
    _.provideGauge("selected_private_key_version") {
      selectedPrivateKeyVersion.get().toFloat
    }
  }

  private val privateKeySubscription =
    configBusSubscription(stats = stats, initialValue = PrivateKeysConfiguration.EMPTY)

  private val selectedPrivateKeyVersion: AtomicInteger = new AtomicInteger(0)

  private val keyProvider: KeyProviderInterface = {
    awaitForConfigFileFromSubscription(privateKeySubscription)
    PrivateKeyProviderProxy(
      environment = brokerServiceEnv,
      issuer = brokerServiceName,
      logger = loggerConnection,
      stats = statsConnection,
      pasetoHeadersPrivateKeySubscription = privateKeySubscription
    )
  }

  private val entitySigner = new SigningService[Principals.Principal](
    "principal",
    classOf[Principals.Principal],
    brokerServiceEnv,
    brokerServiceName,
    keyProvider,
    loggerConnection,
    statsConnection,
    tokenTtlSec
  )

  private[pasetoheaders] def getIssuer: String = {
    entitySigner.getIssuer
  }
  private[pasetoheaders] def getEnvironment: String = {
    entitySigner.getEnvironment
  }

  /**
   * Track selected private key version in the private keys config file
   */
  privateKeySubscription.data.changes.respond { newValue =>
    // validate if private key config contains specified selected key version
    if (newValue.keys.exists(p => p.keyVersion == newValue.selectedKeyVersion)) {
      selectedPrivateKeyVersion.set(newValue.selectedKeyVersion)
    }
  }

  /**
   * Encrypt using a private key with specific version
   *
   * @param token
   * @param keyVersion
   * @return
   */
  def signToken(principal: Principals.Principal, keyVersion: Integer): Option[String] = {
    entitySigner.signToken(principal, Some(keyVersion))
  }

  /**
   * Encrypt using the last private key
   *
   * @param token
   * @return
   */
  def signToken(principal: Principals.Principal): Option[String] = {
    entitySigner.signToken(principal, None)
  }

  /**
   * Encrypt using a private key with version key specified in config file as selected_key_version
   *
   * @param token
   * @return
   */
  def signTokenUsingConfiguredKeyVersion(principal: Principals.Principal): Option[String] = {
    entitySigner.signToken(principal, Some(int2Integer(selectedPrivateKeyVersion.get())))
  }
}
