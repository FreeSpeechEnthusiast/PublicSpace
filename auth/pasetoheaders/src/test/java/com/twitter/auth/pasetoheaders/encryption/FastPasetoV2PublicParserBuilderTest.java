package com.twitter.auth.pasetoheaders.encryption;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.Mockito;

import dev.paseto.jpaseto.ExpiredPasetoException;
import dev.paseto.jpaseto.IncorrectClaimException;
import dev.paseto.jpaseto.KeyResolver;
import dev.paseto.jpaseto.MissingClaimException;
import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.PasetoParser;
import dev.paseto.jpaseto.Pasetos;
import dev.paseto.jpaseto.PrematurePasetoException;
import dev.paseto.jpaseto.Purpose;
import dev.paseto.jpaseto.UnsupportedPasetoException;
import dev.paseto.jpaseto.Version;
import dev.paseto.jpaseto.io.Deserializer;
import dev.paseto.jpaseto.lang.DescribedPredicate;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;

public class FastPasetoV2PublicParserBuilderTest extends Stubs {

  private PublicKey publicKey = KeyUtils
      .getPublicKey("1eb9dbbbbc047c03fd70604e0071f0987e16b28b757225c11f00415d0e20b1a2");
  private PrivateKey privateKey =
      KeyUtils
          .getPrivateKey("b4cbfb43df4ce210727d953e4a713307fa19bb7d9f85041438d9e11b942a3774");
  @Test
  public void setDeserializer() {
    FastPasetoV2PublicParserBuilder pasetoV2PublicParserBuilder =
        Mockito.spy(new FastPasetoV2PublicParserBuilder());
    Deserializer<Map<String, Object>> customDeserializer = bytes -> Collections.singletonMap(
        "something", 0L);
    pasetoV2PublicParserBuilder.setDeserializer(customDeserializer);
    assertEquals(pasetoV2PublicParserBuilder.getDeserializer(), customDeserializer);
  }

  @Test(expected = UnsupportedPasetoException.class)
  public void unsupportedVersionTest() {
    String input = "v5.local.something";

    PasetoParser parser = new FastPasetoV2PublicParserBuilder()
        .setPublicKey(publicKey)
        .build();

      parser.parse(input);
  }

  @Test(expected = UnsupportedPasetoException.class)
  public void unsupportedPurposeTest() {
    String input = "v1.other.something";

      new FastPasetoV2PublicParserBuilder()
          .setPublicKey(publicKey)
          .build()
          .parse(input);
  }

  @Test
  public void basicUsageTest() {

    PublicKey testPublicKey = mock(PublicKey.class);
    byte[] secret = "a-secret".getBytes(UTF_8);
    Clock clock = mock(Clock.class);
    Duration skew = Duration.ofSeconds(101);
    Deserializer deserializer = mock(Deserializer.class);

    PasetoParser parser =  new FastPasetoV2PublicParserBuilder()
        .setSharedSecret(secret)
        .setPublicKey(testPublicKey)
        .setClock(clock)
        .setAllowedClockSkew(skew)
        .setDeserializer(deserializer)
        .build();

    assertEquals(((FastPasetoV2PublicParser) parser).getClock(), clock);
    assertEquals(((FastPasetoV2PublicParser) parser).getAllowedClockSkew(), skew);
    assertEquals(((FastPasetoV2PublicParser) parser).getDeserializer(), deserializer);
    assertEquals(
        ((FastPasetoV2PublicParserBuilder.SimpleKeyResolver) ((FastPasetoV2PublicParser) parser)
        .getKeyResolver()).getPublicKey(), testPublicKey);
    assertArrayEquals(
        ((FastPasetoV2PublicParserBuilder.SimpleKeyResolver) ((FastPasetoV2PublicParser) parser)
        .getKeyResolver()).getSharedSecret().getEncoded(), secret);
  }

  @Test
  public void requiredClaimsTest() {

    PublicKey testPublicKey = mock(PublicKey.class);
    Instant iat = Instant.now();
    Deserializer deserializer = mock(Deserializer.class);
    Instant exp = iat.plus(1, ChronoUnit.DAYS);
    Instant nbf = iat.minus(1, ChronoUnit.MINUTES);

    PasetoParser parser =  new FastPasetoV2PublicParserBuilder()
        .setPublicKey(testPublicKey)
        .setDeserializer(deserializer)
        .requireIssuedAt(iat)
        .requireExpiration(exp)
        .requireNotBefore(nbf)
        .requireIssuer("issuer1")
        .requireAudience("audience1")
        .requireSubject("subject1")
        .requireTokenId("tokenId1")
        .requireKeyId("keyId1")
        .require("foobar", DescribedPredicate.equalTo("FOO"))
        .build();

    assertThat(((FastPasetoV2PublicParser) parser)
        .getUserExpectedClaimsMap(), allOf(
            hasPredicateEntry("iat", "equal to: '" + iat + "'"),
            hasPredicateEntry("exp", "equal to: '" + exp + "'"),
            hasPredicateEntry("nbf", "equal to: '" + nbf + "'"),
            hasPredicateEntry("iss", "equal to: 'issuer1'"),
            hasPredicateEntry("aud", "equal to: 'audience1'"),
            hasPredicateEntry("sub", "equal to: 'subject1'"),
            hasPredicateEntry("jti", "equal to: 'tokenId1'"),
            hasPredicateEntry("foobar", "equal to: 'FOO'")));
  }

  Matcher<Map<? extends String, ?>> hasPredicateEntry(String key, String description) {
    return hasEntry(is(key), hasProperty("description", is(description)));
  }

  @Test(expected = PrematurePasetoException.class)
  public void invalidNotBeforeTest() {
    String token = Pasetos.V2.PUBLIC.builder()
        .setPrivateKey(privateKey)
        .setNotBefore(Instant.now().plus(1, ChronoUnit.HOURS))
        .compact();

    new FastPasetoV2PublicParserBuilder()
        .setPublicKey(publicKey)
        .build()
        .parse(token);
  }

  @Test(expected = ExpiredPasetoException.class)
  public void invalidExpireTest() {
    String token = Pasetos.V2.PUBLIC.builder()
        .setPrivateKey(privateKey)
        .setExpiration(Instant.now().minus(1, ChronoUnit.HOURS))
        .compact();

    new FastPasetoV2PublicParserBuilder()
        .setPublicKey(publicKey)
        .build()
        .parse(token);
  }

  @Test(expected = IncorrectClaimException.class)
  public void requireKeyIdTest() {
    String token = Pasetos.V2.PUBLIC.builder()
        .setPrivateKey(privateKey)
        .setKeyId("invalid")
        .compact();

    PasetoParser parser = new FastPasetoV2PublicParserBuilder()
        .setPublicKey(publicKey)
        .requireKeyId("Valid")
        .build();

    parser.parse(token);
  }

  /**
   * https://github.com/paseto-toolkit/jpaseto/issues/4
   */
  @Test
  public void justKeyPublicResolverTest() {
    // start with a token
    String token = Pasetos.V2.PUBLIC.builder()
        .setPrivateKey(privateKey)
        .setExpiration(Instant.now().plus(1, ChronoUnit.HOURS))
        .setSubject("test-sub")
        .setKeyId("test-kid")
        .compact();

    // setup a mock keyResolver
    KeyResolver keyResolver = mock(KeyResolver.class);
    FastPasetoFooterClaims expectedFooter =
        new FastPasetoFooterClaims(Collections.singletonMap("kid", "test-kid"));
    when(keyResolver.resolvePublicKey(eq(Version.V2), eq(Purpose.PUBLIC), eq(expectedFooter)))
        .thenReturn(publicKey);
    // parse with the key resolver
    PasetoParser parser = new FastPasetoV2PublicParserBuilder()
        .setKeyResolver(keyResolver)
        .build();

    assertThat(((FastPasetoV2PublicParser) parser).getKeyResolver(), sameInstance(keyResolver));
    Paseto result = parser.parse(token);
    assertThat(result.getFooter(), is(expectedFooter));
    assertThat(result.getClaims().getSubject(), is("test-sub"));
  }

  @Test(expected = MissingClaimException.class)
  public void missingKeyIdTest() {
    String token = Pasetos.V2.PUBLIC.builder()
        .setPrivateKey(privateKey)
        .compact();

    PasetoParser parser = new FastPasetoV2PublicParserBuilder()
        .setPublicKey(publicKey)
        .requireKeyId("Valid")
        .build();

    parser.parse(token);
  }

}
