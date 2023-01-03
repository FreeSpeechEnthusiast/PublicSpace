package com.twitter.auth.pasetoheaders.passport

import com.twitter.auth.pasetoheaders.encryption.SigningService
import com.twitter.auth.pasetoheaders.encryption.{KeyProvider => KeyProviderInterface}
import com.twitter.auth.pasetoheaders.finagle.PrivateKeyProviderProxy
import com.twitter.auth.pasetoheaders.finagle.Service
import com.twitter.auth.pasetoheaders.finagle.TssSubscriber
import com.twitter.auth.pasetoheaders.models.Passports
import com.twitter.logging.Logger
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.decider.Feature
import java.util.concurrent.atomic.AtomicInteger

case class PassportSigner(
  logger: Option[Logger],
  stats: Option[StatsReceiver],
  loggingEnabledDecider: Option[Feature] = None,
  keyFileFullPath: String)
    extends Service(
      serviceName = "PassportSigner",
      logger = logger,
      stats = stats,
      loggingEnabledDecider = loggingEnabledDecider)
    with TssSubscriber {

  scopedStats.foreach {
    _.provideGauge("selected_private_key_version") {
      selectedPrivateKeyVersion.get().toFloat
    }
  }

  // TODO change to logger
  println(
    "passportSigner stars with"
      + ", keyFileFullPath=" + keyFileFullPath
  )

  // unfortunately the polling config source builder hard-codes the default
  // path to /usr/local/config/
  // therefore, if we want to use the polling functionality, we have to override the base
  // path in production to be /var/lib/tss/keys
  // therefore, we ALWAYS have to use base path
  // sometimes it is local-resources path, and sometimes it is /var/lib/tss/keys

  // our path will always contain the following:
  // /this/is/full/path/pasetoheaders/$ENVIRONMENT/$ISSUER/private_keys.json
  //                  |.... this path format is mandatory with filename
  private val defaultFilename = "private_keys.json"
  private val privateKeySubPath = "/pasetoheaders/"

  // use a base path for TSS always
  private val basePath =
    keyFileFullPath.substring(0, keyFileFullPath.length - defaultFilename.length)

  /***
   * Parse the base path (as shown below) and return env and issuer
   * @param basePath /this/is/some/full/path/pasetoheaders/$ENV/$ISSUER/ (note trailing slash)
   * @return (environment, issuer) combo
   */
  private[this] def getEnvAndIssuer(basePath: String): (String, String) = {
    val envAndIssuer: (String, String) = {
      val noTrailingSlash = basePath.stripSuffix("/")
      noTrailingSlash.indexOf(privateKeySubPath) match {
        case -1 =>
          throw new RuntimeException("cannot find pasetoheaders in path=" + noTrailingSlash)
        case idx => {
          val joinedBySlash = noTrailingSlash.substring(idx + privateKeySubPath.length)
          println("joined by slash=" + joinedBySlash)
          joinedBySlash.indexOf("/") match {
            case -1 => throw new RuntimeException("cannot find / in path=" + joinedBySlash)
            case slashIdx =>
              (joinedBySlash.substring(0, slashIdx), joinedBySlash.substring(slashIdx + 1))
          }
        }
      }
    }
    envAndIssuer
  }

  // parse this out, and we'll use the variables to set the key signing properties
  private val (pathEnv, pathIssuer) = getEnvAndIssuer(basePath)

  println(
    "passportSigner continues with"
      + " pathEnv=" + pathEnv
      + ", pathIssuer=" + pathIssuer)

  private val privateKeySubscription =
    tssSubscription(
      basePath = basePath,
      filename = defaultFilename,
      stats = stats
    )

  private val selectedPrivateKeyVersion: AtomicInteger = new AtomicInteger(0)

  private val keyProvider: KeyProviderInterface = {
    awaitForConfigFileFromSubscription(privateKeySubscription)
    PrivateKeyProviderProxy(
      environment = pathEnv,
      issuer = pathIssuer,
      logger = loggerConnection,
      stats = statsConnection,
      pasetoHeadersPrivateKeySubscription = privateKeySubscription
    )
  }

  private val entitySigner = new SigningService[Passports.Passport](
    "passport",
    classOf[Passports.Passport],
    pathEnv,
    pathIssuer,
    keyProvider,
    loggerConnection,
    statsConnection
  )

  private[pasetoheaders] def getIssuer(): String = {
    entitySigner.getIssuer
  }
  private[pasetoheaders] def getEnvironment(): String = {
    entitySigner.getEnvironment
  }

  /**
   * Track selected private key version in the tss config file
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
   * @param passport
   * @param keyVersion
   * @return
   */
  def signToken(passport: Passports.Passport, keyVersion: Integer): Option[String] = {
    entitySigner.signToken(passport, Some(keyVersion))
  }

  /**
   * Encrypt using the last private key
   *
   * @param passport
   * @return
   */
  def signToken(passport: Passports.Passport): Option[String] = {
    entitySigner.signToken(passport, None)
  }

  /**
   * Encrypt using a private key with version key specified in config file as selected_key_version
   *
   * @param passport
   * @return
   */
  def signTokenUsingConfiguredKeyVersion(passport: Passports.Passport): Option[String] = {
    entitySigner.signToken(passport, Some(int2Integer(selectedPrivateKeyVersion.get())))
  }
}
