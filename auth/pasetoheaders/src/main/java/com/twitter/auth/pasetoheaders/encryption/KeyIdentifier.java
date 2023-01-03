package com.twitter.auth.pasetoheaders.encryption;

import java.util.Optional;

public class KeyIdentifier {
  private final String issuer;
  private final Integer version;
  private final String environment;
  private static final String DELIMITER = ":";

  public String getEnvironment() {
    return environment;
  }

  public String getIssuer() {
    return issuer;
  }

  public Integer getVersion() {
    return version;
  }

  public KeyIdentifier(String environment, String issuer, Integer version) {
    this.environment = environment;
    this.issuer = issuer;
    this.version = version;
  }

  /**
   * Extracts key identifier from PASETO Key-Id footer claim
   * @param keyId
   * @return
   */
  public static Optional<KeyIdentifier> parseFromKeyIdClaim(String keyId) {
    String[] chunks = keyId.split(DELIMITER, 3);
    if (chunks.length != 3) {
      return Optional.empty();
    }
    return Optional.of(new KeyIdentifier(chunks[0], chunks[1], Integer.valueOf(chunks[2])));
  }

  public String getKeyId() {
    return String.join(DELIMITER,
        this.getEnvironment(),
        this.getIssuer(),
        this.getVersion().toString());
  }
}
