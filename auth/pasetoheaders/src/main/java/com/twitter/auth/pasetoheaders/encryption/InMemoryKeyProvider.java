package com.twitter.auth.pasetoheaders.encryption;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

/**
 * In-memory implementation of key provider
 */
public class InMemoryKeyProvider implements KeyProvider {

  private final InMemoryKeyStorage<PrivateKey> privateKeyStorage = new InMemoryKeyStorage<>();
  private final InMemoryKeyStorage<PublicKey> publicKeyStorage = new InMemoryKeyStorage<>();

  /**
   * Adds private key with specific identifier to the key storage
   *
   * @param keyIdentifier
   * @param privateKey
   */
  public void addPrivateKey(
      KeyIdentifier keyIdentifier,
      PrivateKey privateKey) {
    privateKeyStorage.addKey(keyIdentifier, privateKey);
  }

  /**
   * Adds public key with specific identifier to the key storage
   *
   * @param keyIdentifier
   * @param publicKey
   */
  public void addPublicKey(
      KeyIdentifier keyIdentifier,
      PublicKey publicKey) {
    publicKeyStorage.addKey(keyIdentifier, publicKey);
  }

  public void clear() {
    privateKeyStorage.clear();
    publicKeyStorage.clear();
  }

  @Override
  public Optional<VersionedKey<PrivateKey>> getLastPrivateKey(
      String environment,
      String issuer) {
    return this.privateKeyStorage.getLastKey(environment, issuer);
  }

  @Override
  public Optional<PrivateKey> getPrivateKey(KeyIdentifier keyIdentifier) {
    return this.privateKeyStorage.getKey(keyIdentifier);
  }

  @Override
  public Optional<PublicKey> getPublicKey(KeyIdentifier keyIdentifier) {
    return this.publicKeyStorage.getKey(keyIdentifier);
  }

}
