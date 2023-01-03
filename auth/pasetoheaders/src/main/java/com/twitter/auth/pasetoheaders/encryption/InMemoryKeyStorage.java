package com.twitter.auth.pasetoheaders.encryption;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class InMemoryKeyStorage<T extends Key> {
  /*
   Key Storage:
    HashMap(environment -> HashMap(issuer -> TreeMap(version -> key))
   */
  private final HashMap<String,
      HashMap<String,
          TreeMap<Integer, T>>> keyStorage = new HashMap<>();

  /**
   * Adds key with specific identifier to the key storage
   *
   * @param keyIdentifier
   * @param key
   */
  public void addKey(
      KeyIdentifier keyIdentifier,
      T key) {
    keyStorage
        .putIfAbsent(keyIdentifier.getEnvironment(), new HashMap<>());
    keyStorage.get(keyIdentifier.getEnvironment())
        .putIfAbsent(keyIdentifier.getIssuer(), new TreeMap<>());
    keyStorage.get(keyIdentifier.getEnvironment())
        .get(keyIdentifier.getIssuer())
        .put(keyIdentifier.getVersion(), key);
  }

  public void clear() {
    keyStorage.clear();
  }

  /**
   * Returns a key with maximum version number
   *
   * @param environment
   * @param issuer
   * @return the last key if present or optional.empty if key is not found
   */
  public Optional<VersionedKey<T>> getLastKey(
      String environment,
      String issuer) {
    if (!keyStorage.containsKey(environment)
        || !keyStorage.get(environment).containsKey(issuer)) {
      return Optional.empty();
    }
    Map.Entry<Integer, T> lastKey = keyStorage
        .get(environment)
        .get(issuer)
        .lastEntry();
    return Optional.of(new VersionedKey<T>(
        new KeyIdentifier(environment, issuer, lastKey.getKey()),
        lastKey.getValue()));
  }

  /**
   * Returns a key with specific identifier
   *
   * @param keyIdentifier
   * @return the requested key if present or optional.empty if key is not found
   */
  public Optional<T> getKey(KeyIdentifier keyIdentifier) {
    if (!keyStorage.containsKey(keyIdentifier.getEnvironment())
        || !keyStorage.get(keyIdentifier.getEnvironment())
        .containsKey(keyIdentifier.getIssuer())
        || !keyStorage.get(keyIdentifier.getEnvironment())
        .get(keyIdentifier.getIssuer())
        .containsKey(keyIdentifier.getVersion())) {
      return Optional.empty();
    }
    return Optional.of(keyStorage.get(keyIdentifier.getEnvironment())
        .get(keyIdentifier.getIssuer())
        .get(keyIdentifier.getVersion()));
  }
}
