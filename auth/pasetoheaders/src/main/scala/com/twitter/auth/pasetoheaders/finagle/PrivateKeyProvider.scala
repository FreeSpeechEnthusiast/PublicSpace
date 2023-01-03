package com.twitter.auth.pasetoheaders.finagle

import com.twitter.auth.pasetoheaders.encryption.{KeyIdentifier, KeyUtils, VersionedKey}
import com.twitter.configbus.subscriber.{JsonConfigParser, Subscription}
import java.security.PrivateKey

/**
 * Private key provider provides ability to load keys from auto-updatable config file located in a mounted tss secret
 *
 * @param environment
 * @param issuer
 * @param pasetoHeadersPrivateKeySubscription
 * @param logger
 * @param stats
 */
private case class PrivateKeyProvider(
  environment: String,
  issuer: String,
  pasetoHeadersPrivateKeySubscription: Subscription[PrivateKeysConfiguration],
  private val logger: Option[FinagleLoggerProxy],
  private val stats: Option[FinagleStatsProxy],
  private val privateKeyStorage: UpdatableKeyStorage[PrivateKey])
    extends PlatformKeyProvider {

  import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
  import com.twitter.auth.pasetoheaders.javahelpers.MapConv._

  override protected val LOGGING_SCOPE = "privatekeyprovider"

  pasetoHeadersPrivateKeySubscription.data.changes.respond { newValue =>
    // validate if private key config contains specified selected key version
    if (newValue.keys.exists(p => p.keyVersion == newValue.selectedKeyVersion)) {
      privateKeyStorage.replaceKeysWithNewVersion(
        newValue.keys.map(key =>
          (
            new KeyIdentifier(environment, issuer, key.keyVersion),
            KeyUtils.getPrivateKey(key.keyData))))
      doStatementIfOptionSet[FinagleStatsProxy](
        stats,
        s =>
          s.counter(
            scope = LOGGING_SCOPE,
            name = "private_keys_refreshed",
            metadata = Some(commonLogMetadata)
          )
      )
    } else {
      doStatementIfOptionSet[FinagleLoggerProxy](
        logger,
        l =>
          l.error(
            scope = LOGGING_SCOPE,
            message = "unable to find a private key with selected key version",
            metadata = Some(
              commonLogMetadata ++ Map(
                "selected_key_version" -> newValue.selectedKeyVersion.toString
              ))
          )
      )
      doStatementIfOptionSet[FinagleStatsProxy](
        stats,
        s =>
          s.counter(
            scope = LOGGING_SCOPE,
            name = "wrong_selected_private_key",
            metadata = Some(commonLogMetadata)
          )
      )
    }
  }

  override protected def commonLogMetadata: Map[String, String] = {
    Map(
      "environment" -> environment,
      "issuer" -> issuer,
    )
  }

  def getLastPrivateKey(
    environment: String,
    issuer: String
  ): Option[VersionedKey[PrivateKey]] = {
    privateKeyStorage.getLastKey(environment, issuer)
  }

  def getPrivateKey(keyIdentifier: KeyIdentifier): Option[PrivateKey] = {
    privateKeyStorage.getKey(keyIdentifier)
  }

}

/**
 * Declares JSON file data format
 * {"selected_key_version": XX, "keys": [{"key_version", "key_data"}]}
 *
 * Private keys JSON file contains 1 file per env/issuer, so only one key could be selected
 */
case class KeyWithVersionIdentifier(
  keyVersion: Int,
  keyData: String)

case class PrivateKeysConfiguration(selectedKeyVersion: Int, keys: Set[KeyWithVersionIdentifier]) {}

object PrivateKeysConfigParser extends JsonConfigParser[PublicKeysConfiguration]

object PrivateKeysConfiguration {
  val EMPTY: PrivateKeysConfiguration = PrivateKeysConfiguration(0, Set.empty)
}
