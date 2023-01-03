package com.twitter.auth.pasetoheaders.encryption;

public final class VersionedKey<T> {

  private final KeyIdentifier keyIdentifier;
  private final T key;

  public KeyIdentifier getKeyIdentifier() {
    return keyIdentifier;
  }

  public T getKey() {
    return key;
  }

  public VersionedKey(KeyIdentifier keyIdentifier, T key) {
    this.keyIdentifier = keyIdentifier;
    this.key = key;
  }

}
