package com.twitter.auth.pasetoheaders.encryption;

import java.util.Optional;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SigningServiceTest extends Stubs {

  @Test
  public void testSignToken() {
    MatcherAssert.assertThat(signingService.signToken(customerPassport, Optional.empty()).get(),
        StringContains.containsString("v2.public."));
  }

  @Test
  public void testSignTokenWithSpecificVersion() {
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, 2),
        KeyUtils.getPrivateKey(testOtherPrivateKeyString));
    MatcherAssert.assertThat(signingService.signToken(customerPassport, Optional.of(2)).get(),
        StringContains.containsString("v2.public."));
  }

  @Test
  public void testSignTokenWithSpecificNonExistingVersion() {
    assertEquals(false, signingService.signToken(customerPassport, Optional.of(2)).isPresent());
  }
}
