package com.twitter.auth.pasetoheaders.encryption;

import java.security.PrivateKey;
import java.security.PublicKey;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import static org.hamcrest.core.Is.is;

import static org.hamcrest.CoreMatchers.instanceOf;

public class KeyUtilsTest extends Stubs  {

  @Test
  public void testGetPrivateKey() {
    MatcherAssert.assertThat(KeyUtils.getPrivateKey(testPrivateKeyString),
        instanceOf(PrivateKey.class));
  }

  @Test
  public void testGetPublicKey() {
    MatcherAssert.assertThat(KeyUtils.getPublicKey(testPublicKeyString),
        instanceOf(PublicKey.class));
  }

  @Test
  public void hexDecoderTest1() {
    MatcherAssert.assertThat(KeyUtils.decodeHex(
        "9901cc3d1c41d2ef24c7f0054fd107fa8502d863a00673ca93fd059ff21f9adb"),
        is(new byte[]{
            -103, 1, -52, 61, 28, 65, -46, -17, 36, -57, -16,
            5, 79, -47, 7, -6, -123, 2, -40, 99, -96, 6, 115,
            -54, -109, -3, 5, -97, -14, 31, -102, -37}));
  }

  @Test
  public void hexDecoderTest2() {
    MatcherAssert.assertThat(KeyUtils.decodeHex(
        "0856f21329ca27c159e20b7667d77119b342c05b7e5c2c9aec4acdeeb855596c"),
        is(new byte[]{
            8, 86, -14, 19, 41, -54, 39, -63, 89, -30, 11, 118,
            103, -41, 113, 25, -77, 66, -64, 91, 126, 92, 44,
            -102, -20, 74, -51, -18, -72, 85, 89, 108}));
  }

}
