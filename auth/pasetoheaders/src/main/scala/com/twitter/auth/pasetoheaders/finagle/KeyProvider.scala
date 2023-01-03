package com.twitter.auth.pasetoheaders.finagle

import com.twitter.auth.pasetoheaders.encryption.{
  KeyIdentifier,
  VersionedKey,
  KeyProvider => IKeyProvider
}
import com.twitter.configbus.subscriber.Subscription
import java.security.{PrivateKey, PublicKey}
import java.util.Optional
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._

/**
 * Key provider with private keys only
 */
case class PrivateKeyProviderProxy(
  environment: String,
  issuer: String,
  pasetoHeadersPrivateKeySubscription: Subscription[PrivateKeysConfiguration],
  logger: Option[FinagleLoggerProxy],
  stats: Option[FinagleStatsProxy],
  privateKeyStorage: UpdatableKeyStorage[PrivateKey] = SimpleUpdatableKeyStorage[PrivateKey]())
    extends IKeyProvider {

  private val keyProvider = PrivateKeyProvider(
    environment = environment,
    issuer = issuer,
    pasetoHeadersPrivateKeySubscription = pasetoHeadersPrivateKeySubscription,
    logger = logger,
    stats = stats,
    privateKeyStorage = privateKeyStorage,
  )

  override def getLastPrivateKey(
    environment: String,
    issuer: String
  ): Optional[VersionedKey[PrivateKey]] = {
    keyProvider.getLastPrivateKey(environment, issuer)
  }

  override def getPrivateKey(keyIdentifier: KeyIdentifier): Optional[PrivateKey] = {
    keyProvider.getPrivateKey(keyIdentifier)
  }

  override def getPublicKey(keyIdentifier: KeyIdentifier): Optional[PublicKey] = {
    None
  }

}

/**
 * Key provider with public keys only
 */
case class PublicKeyProviderProxy(
  pasetoHeadersPublicKeySubscription: Subscription[PublicKeysConfiguration],
  logger: Option[FinagleLoggerProxy],
  stats: Option[FinagleStatsProxy],
  publicKeyStorage: UpdatableKeyStorage[PublicKey] = SimpleUpdatableKeyStorage[PublicKey]())
    extends IKeyProvider {

  private val keyProvider = PublicKeyProvider(
    pasetoHeadersPublicKeySubscription = pasetoHeadersPublicKeySubscription,
    logger = logger,
    stats = stats,
    publicKeyStorage = publicKeyStorage,
  )

  override def getLastPrivateKey(
    environment: String,
    issuer: String
  ): Optional[VersionedKey[PrivateKey]] = {
    None
  }

  override def getPrivateKey(keyIdentifier: KeyIdentifier): Optional[PrivateKey] = {
    None
  }

  override def getPublicKey(keyIdentifier: KeyIdentifier): Optional[PublicKey] = {
    keyProvider.getPublicKey(keyIdentifier)
  }

}
