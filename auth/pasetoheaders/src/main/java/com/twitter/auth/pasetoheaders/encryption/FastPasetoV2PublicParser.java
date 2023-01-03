package com.twitter.auth.pasetoheaders.encryption;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import dev.paseto.jpaseto.ClaimPasetoException;
import dev.paseto.jpaseto.ExpiredPasetoException;
import dev.paseto.jpaseto.FooterClaims;
import dev.paseto.jpaseto.IncorrectClaimException;
import dev.paseto.jpaseto.InvalidClaimException;
import dev.paseto.jpaseto.KeyResolver;
import dev.paseto.jpaseto.MissingClaimException;
import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.PasetoParser;
import dev.paseto.jpaseto.PasetoSignatureException;
import dev.paseto.jpaseto.PrematurePasetoException;
import dev.paseto.jpaseto.Purpose;
import dev.paseto.jpaseto.UnsupportedPasetoException;
import dev.paseto.jpaseto.Version;
import dev.paseto.jpaseto.impl.DefaultClaims;
import dev.paseto.jpaseto.impl.crypto.V2PublicCryptoProvider;
import dev.paseto.jpaseto.io.Deserializer;
import dev.paseto.jpaseto.lang.Assert;
import dev.paseto.jpaseto.lang.DateFormats;
import dev.paseto.jpaseto.lang.DescribedPredicate;

/**
 * Class is based on dev.paseto.jpaseto.impl.DefaultPasetoParser
 * https://github.com/paseto-toolkit/jpaseto/blob/74420edbc1180cfdd14bce36ef920407b55c4828/impl/src/main/java/dev/paseto/jpaseto/impl/DefaultPasetoParser.java
 * but it uses cryptoProvider passed from FastPasetoParserBuilder instead of loading it each time using Services.loadFirst
 *
 * Warning! This implementation currently supports V2 Public tokens only
 */
public class FastPasetoV2PublicParser implements PasetoParser {

  public Clock getClock() {
    return clock;
  }

  public Duration getAllowedClockSkew() {
    return allowedClockSkew;
  }

  public KeyResolver getKeyResolver() {
    return keyResolver;
  }

  public Map<String, Predicate<Object>> getUserExpectedClaimsMap() {
    return userExpectedClaimsMap;
  }

  public Deserializer<Map<String, Object>> getDeserializer() {
    return deserializer;
  }

  public Map<String, Predicate<Object>> getUserExpectedFooterClaimsMap() {
    return userExpectedFooterClaimsMap;
  }

  private final V2PublicCryptoProvider v2PublicCryptoProvider;
  private final KeyResolver keyResolver;
  private final Deserializer<Map<String, Object>> deserializer;
  private final Clock clock;
  private final Duration allowedClockSkew;
  private final Map<String, Predicate<Object>> userExpectedClaimsMap;
  private final Map<String, Predicate<Object>> userExpectedFooterClaimsMap;

  FastPasetoV2PublicParser(V2PublicCryptoProvider v2PublicCryptoProvider,
                           KeyResolver keyResolver,
                           Deserializer<Map<String, Object>> deserializer,
                           Clock clock,
                           Duration allowedClockSkew,
                           Map<String, Predicate<Object>> expectedClaimsMap,
                           Map<String, Predicate<Object>> expectedFooterClaimsMap) {
    this.v2PublicCryptoProvider = v2PublicCryptoProvider;
    this.keyResolver = keyResolver;
    this.deserializer = deserializer;
    this.clock = clock;
    this.allowedClockSkew = allowedClockSkew;
    this.userExpectedClaimsMap = Collections.unmodifiableMap(expectedClaimsMap);
    this.userExpectedFooterClaimsMap = Collections.unmodifiableMap(expectedFooterClaimsMap);
  }

  @Override
  public Paseto parse(String token) {
    Assert.hasText(token, "Paseto token cannot be null or empty");

    String[] parts = token.split("\\.");
    Assert.isTrue(parts.length == 3 || parts.length == 4,
        "Paseto token expected to have 3 or 4 parts."); // header is optional
    // format is <version>.<purpose>.<payload>[.<footer>]

    Version version = Version.from(parts[0]);
    Purpose purpose = Purpose.from(parts[1]);
    byte[] payloadBytes =
        Base64.getUrlDecoder().decode(parts[2].getBytes(StandardCharsets.UTF_8));
    byte[] footerBytes = parts.length == 4
        ? Base64.getUrlDecoder().decode(parts[3].getBytes(StandardCharsets.UTF_8)) : new byte[0];

    Paseto paseto;
    if (version == Version.V2 && purpose == Purpose.PUBLIC) {
      paseto = v2Public(payloadBytes, footerBytes);
    } else {
      // Cannot reach this point unless the Version and/or Purpose enum have been changed
      // parsing those enums will fail before this point
      throw new UnsupportedPasetoException("Paseto token with header: '" + version.toString()
          + "." + purpose.toString()
          + ".' is not supported.");
    }

    verifyExpiration(paseto);
    verifyNotBefore(paseto);
    validateExpectedClaims(paseto);
    validateExpectedFooterClaims(paseto);

    return paseto;
  }

  private Paseto v2Public(byte[] payload, byte[] footerBytes) {
    // parse footer to map (if available)
    FooterClaims footer = toFooter(footerBytes);

    byte[] message = Arrays.copyOf(payload, payload.length - 64);
    byte[] signature = Arrays.copyOfRange(payload, payload.length - 64, payload.length);

    PublicKey publicKey = keyResolver.resolvePublicKey(Version.V2, Purpose.PUBLIC, footer);
    Assert.notNull(publicKey,
        "A public key could not be resolved.  A public key must be configured in "
            + "'Pasetos.parserBuilder().setPublicKey(...)' "
            + "or Pasetos.parserBuilder().setKeyResolver(...)");
    boolean valid = v2PublicCryptoProvider.verify(message, footerBytes, signature, publicKey);
    if (!valid) {
      throw new PasetoSignatureException("Signature could not be validated in paseto token.");
    }

    Map<String, Object> claims = deserializer.deserialize(message);
    return new FastPaseto(Version.V2, Purpose.PUBLIC, new DefaultClaims(claims), footer);
  }

  private FooterClaims toFooter(byte[] footerBytes) {
    if (footerBytes.length != 0) {
      if (footerBytes[0] == '{' && footerBytes[footerBytes.length - 1] == '}') { // assume JSON
        return new FastPasetoFooterClaims(deserializer.deserialize(footerBytes));
      } else {
        return  new FastPasetoFooterClaims(new String(footerBytes, StandardCharsets.UTF_8));
      }
    } else {
      return  new FastPasetoFooterClaims("");
    }
  }

  /**
   * The current paseto spec registers the 'nbf' claim but does NOT provide validation information.  This library
   * uses the JWT spec for expiration:
   * https://tools.ietf.org/html/draft-ietf-oauth-json-web-token-30#section-4.1.5
   *
   * Token MUST NOT be accepted on or after any specified exp time
   *
   * @param paseto
   */
  private void verifyNotBefore(Paseto paseto) {
    Instant now = clock.instant();
    Instant nbf = paseto.getClaims().getNotBefore();
    if (nbf != null) {

      Instant min = now.plus(allowedClockSkew);
      if (min.isBefore(nbf)) {
        String nbfVal = DateFormats.formatIso8601(nbf);
        String nowVal = DateFormats.formatIso8601(now);

        Duration diff = Duration.between(nbf, min);

        String msg = "JWT must not be accepted before "
            + nbfVal
            + ". Current time: "
            + nowVal
            + ", a difference of "
            + diff
            + ".  Allowed clock skew: "
            + this.allowedClockSkew + ".";
        throw new PrematurePasetoException(paseto, msg);
      }
    }
  }

  /**
   * The current paseto spec registers the 'exp' claim but does NOT provide validation information.  This library
   * uses the JWT spec for expiration:
   * https://tools.ietf.org/html/draft-ietf-oauth-json-web-token-30#section-4.1.4
   *
   * Token MUST NOT be accepted on or after any specified exp time
   *
   * @param paseto
   */
  private void verifyExpiration(Paseto paseto) {
    Instant now = clock.instant();
    Instant exp = paseto.getClaims().getExpiration();

    if (exp != null) {

      Instant max = now.minus(allowedClockSkew);
      if (max.isAfter(exp)) {
        String expVal = DateFormats.formatIso8601(exp);
        String nowVal = DateFormats.formatIso8601(now);

        Duration diff = Duration.between(max, exp);

        String msg = "Paseto expired at "
            + expVal
            + ". Current time: "
            + nowVal
            + ", a difference of "
            + diff
            + ".  Allowed clock skew: "
            + allowedClockSkew
            + ".";
        throw new ExpiredPasetoException(paseto, msg);
      }
    }
  }

  private void validateExpectedClaims(Paseto paseto) {
    validateExpected(paseto, paseto.getClaims(), userExpectedClaimsMap);
  }

  private void validateExpectedFooterClaims(Paseto paseto) {
    validateExpected(paseto, paseto.getFooter(), userExpectedFooterClaimsMap);
  }

  private void validateExpected(Paseto paseto,
                                Map<String, Object> claims,
                                Map<String, Predicate<Object>> expectedClaims) {
    expectedClaims.forEach((claimName, predicate) -> {

      Object actualClaimValue = normalize(claims.get(claimName));

      InvalidClaimException invalidClaimException = null;

      String description = "<unnamed predicate>";
      if (predicate instanceof DescribedPredicate) {
        description = ((DescribedPredicate<Object>) predicate).getDescription();
      }

      if (actualClaimValue == null) {

        String msg = String.format(ClaimPasetoException.MISSING_EXPECTED_CLAIM_MESSAGE_TEMPLATE,
            claimName, description);
        invalidClaimException = new MissingClaimException(paseto, claimName, description, msg);

      } else if (!predicate.test(actualClaimValue)) {

        String msg = String.format(ClaimPasetoException.INCORRECT_EXPECTED_CLAIM_MESSAGE_TEMPLATE,
            claimName, description, actualClaimValue);

        invalidClaimException = new IncorrectClaimException(paseto, claimName, description, msg);
      }

      if (invalidClaimException != null) {
        throw invalidClaimException;
      }
    });
  }

  private static Object normalize(Object o) {
    if (o instanceof Integer) {
      return ((Integer) o).longValue();
    }
    return o;
  }
}
