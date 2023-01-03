package com.twitter.auth.pasetoheaders.encryption;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

public interface KeyProvider {
  /**
   * getLastPrivateKey returns the last available private key for the issuer
   *
   * @param issuer
   * @return the last available private key with version or Optional.empty if key is not found
   */
  Optional<VersionedKey<PrivateKey>> getLastPrivateKey(
      String environment,
      String issuer);
  /**
   * getPrivateKey returns the specific private key for the issuer
   * can be used for manual key rotation
   *
   * @param keyIdentifier
   * @return the last available private key with version or Optional.empty if key is not found
   */
  Optional<PrivateKey> getPrivateKey(KeyIdentifier keyIdentifier);
  /**
   * getPublicKey returns public key with specific identifier
   * for key rotation and multiple authority we are obtaining key identifier
   * from unencrypted kid (Key-ID) claim (see https://paseto.io/rfc/)
   *
   * @param keyIdentifier
   * @return public key with specific identifier or Optional.empty if key is not found
   */
  Optional<PublicKey> getPublicKey(KeyIdentifier keyIdentifier);
}
