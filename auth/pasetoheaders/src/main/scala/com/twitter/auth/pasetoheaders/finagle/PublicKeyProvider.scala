package com.twitter.auth.pasetoheaders.finagle

import com.twitter.auth.pasetoheaders.encryption.{KeyIdentifier, KeyUtils}
import com.twitter.configbus.subscriber.{JsonConfigParser, Subscription}
import java.security.PublicKey

/**
 * Public key provider provides ability to load public keys from auto-updatable config file located in config bus
 *
 * @param pasetoHeadersPublicKeySubscription
 * @param logger
 * @param stats
 */
private case class PublicKeyProvider(
  pasetoHeadersPublicKeySubscription: Subscription[PublicKeysConfiguration],
  private val logger: Option[FinagleLoggerProxy],
  private val stats: Option[FinagleStatsProxy],
  private val publicKeyStorage: UpdatableKeyStorage[PublicKey])
    extends PlatformKeyProvider {

  import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
  import com.twitter.auth.pasetoheaders.javahelpers.MapConv._

  override protected val LOGGING_SCOPE = "publickeyprovider"

  // reload when the keys get updated in configbus in thread-safe mode
  pasetoHeadersPublicKeySubscription.data.changes.respond { newValue =>
    publicKeyStorage.replaceKeysWithNewVersion(
      newValue.keys.map(key =>
        (
          new KeyIdentifier(key.keyEnvironment, key.keyIssuer, key.keyVersion),
          KeyUtils.getPublicKey(key.keyData)))
    )
    doStatementIfOptionSet[FinagleStatsProxy](
      stats,
      s =>
        s.counter(
          scope = LOGGING_SCOPE,
          name = "public_keys_refreshed",
          metadata = Some(commonLogMetadata)
        )
    )
  }

  def getPublicKey(keyIdentifier: KeyIdentifier): Option[PublicKey] = {
    publicKeyStorage.getKey(keyIdentifier)
  }

}

/**
 * Declares JSON file data format
 * {"keys": [{"key_environment", "key_version", "key_issuer", "key_data"}]}
 *
 * Public keys JSON file contains configuration for all issuers and environments
 */
case class KeyWithIdentifier(
  keyEnvironment: String,
  keyIssuer: String,
  keyVersion: Int,
  keyData: String)

case class PublicKeysConfiguration(keys: Set[KeyWithIdentifier]) {}

object PublicKeysConfigParser extends JsonConfigParser[PublicKeysConfiguration]

object PublicKeysConfiguration {
  val EMPTY: PublicKeysConfiguration = PublicKeysConfiguration(Set.empty)
}
