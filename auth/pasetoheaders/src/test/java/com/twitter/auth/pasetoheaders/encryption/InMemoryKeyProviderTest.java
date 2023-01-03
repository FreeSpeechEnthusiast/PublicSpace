package com.twitter.auth.pasetoheaders.encryption;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InMemoryKeyProviderTest extends Stubs {

  protected InMemoryKeyProvider keyProvider = new InMemoryKeyProvider();

  /*
  PRIVATE KEY
  */

  @Test
  public void testAddPrivateKey() {
    keyProvider.clear();
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, testLastKeyVersion),
        KeyUtils.getPrivateKey(testPrivateKeyString));
    assertEquals(
        KeyUtils.getPrivateKey(testPrivateKeyString),
        keyProvider.getLastPrivateKey(testEnv, testIssuer)
            .get()
            .getKey());
  }

  @Test
  public void testThatWeGetPrivateKeyByVersion() {
    keyProvider.clear();
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, 1),
        KeyUtils.getPrivateKey(testOtherPrivateKeyString));
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, 2),
        KeyUtils.getPrivateKey(testPrivateKeyString));
    assertEquals(
        KeyUtils.getPrivateKey(testOtherPrivateKeyString),
        keyProvider.getPrivateKey(
            new KeyIdentifier(testEnv, testIssuer, 1)).get());
    assertEquals(
        KeyUtils.getPrivateKey(testPrivateKeyString),
        keyProvider.getPrivateKey(
            new KeyIdentifier(testEnv, testIssuer, 2)).get());
  }

  @Test
  public void testThatWeCantGetNonExistingPrivateKeyByVersion() {
    keyProvider.clear();
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, 1),
        KeyUtils.getPrivateKey(testOtherPrivateKeyString));
    assertEquals(
        false,
        keyProvider.getPrivateKey(
            new KeyIdentifier(testEnv, testIssuer, 2)).isPresent());
  }

  @Test
  public void testThatWeGetTheLastPrivateKey() {
    keyProvider.clear();
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, 1),
        KeyUtils.getPrivateKey(testOtherPrivateKeyString));
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, 3),
        KeyUtils.getPrivateKey(testPrivateKeyString));
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, 2),
        KeyUtils.getPrivateKey(testOtherPrivateKeyString));
    assertEquals(
        KeyUtils.getPrivateKey(testPrivateKeyString),
        keyProvider.getLastPrivateKey(testEnv, testIssuer).get()
            .getKey());
    assertEquals((Integer) 3,
        keyProvider.getLastPrivateKey(testEnv, testIssuer).get()
            .getKeyIdentifier()
            .getVersion());
  }

  public void testThatWeGetNotGetPrivateKeyUsingWrongEnv() {
    keyProvider.clear();
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, testLastKeyVersion),
        KeyUtils.getPrivateKey(testPrivateKeyString));
    assertEquals(false, keyProvider
        .getLastPrivateKey(testOtherEnv, testIssuer).isPresent());
  }

  public void testThatWeGetNotGetPrivateKeyUsingWrongIssuer() {
    keyProvider.clear();
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, testLastKeyVersion),
        KeyUtils.getPrivateKey(testPrivateKeyString));
    assertEquals(false, keyProvider
        .getLastPrivateKey(testEnv, testOtherIssuer).isPresent());
  }

  /*
  PUBLIC KEY
  */

  @Test
  public void testAddPublicKey() {
    keyProvider.clear();
    KeyIdentifier keyIdentifier = new KeyIdentifier(testEnv, testIssuer, testLastKeyVersion);
    keyProvider.addPublicKey(keyIdentifier,
        KeyUtils.getPublicKey(testPublicKeyString));
    assertEquals(KeyUtils.getPublicKey(testPublicKeyString),
        keyProvider.getPublicKey(keyIdentifier).get());
  }

  public void testThatWeGetNotGetPublicKeyUsingWrongEnv() {
    keyProvider.clear();
    KeyIdentifier keyIdentifier = new KeyIdentifier(testEnv, testIssuer, testLastKeyVersion);
    keyProvider.addPublicKey(keyIdentifier,
        KeyUtils.getPublicKey(testPublicKeyString));
    assertEquals(false, keyProvider.getPublicKey(
        new KeyIdentifier(testOtherEnv, testIssuer, testLastKeyVersion)).isPresent());
  }

  public void testThatWeGetNotGetPublicKeyUsingWrongIssuer() {
    keyProvider.clear();
    keyProvider.addPublicKey(
        new KeyIdentifier(testEnv, testIssuer, testLastKeyVersion),
        KeyUtils.getPublicKey(testPublicKeyString));
    keyProvider.getPublicKey(new KeyIdentifier(testEnv, testOtherIssuer, testLastKeyVersion));
    assertEquals(false, keyProvider.getPublicKey(
        new KeyIdentifier(testEnv, testOtherIssuer, testLastKeyVersion)).isPresent());
  }

  public void testThatWeGetNotGetPublicKeyUsingWrongVersion() {
    keyProvider.clear();
    keyProvider.addPublicKey(
        new KeyIdentifier(testEnv, testIssuer, testLastKeyVersion),
        KeyUtils.getPublicKey(testPublicKeyString));
    keyProvider.getPublicKey(new KeyIdentifier(testEnv, testIssuer, testOtherKeyVersion));
    assertEquals(false, keyProvider.getPublicKey(
        new KeyIdentifier(testEnv, testIssuer, testOtherKeyVersion)).isPresent());
  }

}
