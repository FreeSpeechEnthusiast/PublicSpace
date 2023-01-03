package com.twitter.auth.pasetoheaders.encryption;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import dev.paseto.jpaseto.Claims;
import dev.paseto.jpaseto.FooterClaims;
import dev.paseto.jpaseto.Purpose;
import dev.paseto.jpaseto.Version;
import dev.paseto.jpaseto.impl.DefaultClaims;
import dev.paseto.jpaseto.io.Deserializer;

/**
 * Warning! This class is a part of adoption strategy go/passports-adoption-strategy.
 * It will be removed eventually.
 */
class UnsafeExtraction<T> extends TrackableService {
  /**
   * Extracts unverified claim from PASETO token.
   * Warning! This method is a part of adoption strategy go/passports-adoption-strategy.
   * It will be removed eventually.
   *
   * @param token PASETO token
   *
   * @return claims content or Optional.empty if token invalid
   */
  protected Optional<ExtractedClaims<T>> extractClaimsUsingUnsafeExtraction(
      String claimName,
      ClaimMapping mapping,
      String token
  ) {
    incrMetric("unverified_extraction_requested", 1L, Optional.empty());
    if (token.length() > 0) {
      String[] parts = token.split("\\.", 5);
      if (parts.length == 4) {
        Version version = Version.from(parts[0]);
        Purpose purpose = Purpose.from(parts[1]);
        byte[] payloadBytes = Base64.getUrlDecoder()
            .decode(parts[2].getBytes(StandardCharsets.UTF_8));
        byte[] footerBytes =
            Base64.getUrlDecoder()
                .decode(parts[3].getBytes(StandardCharsets.UTF_8));
        if ((version == Version.V1 || version == Version.V2) && purpose == Purpose.PUBLIC) {
          int messageSize = getTokenMessageSize(payloadBytes, version);
          byte[] message = Arrays.copyOf(payloadBytes, messageSize);
          Deserializer<Map<String, Object>> deserializer = mapping.getDeserializer();
          Claims bodyClaims = new DefaultClaims(deserializer.deserialize(message));
          if (verifyExpiration(bodyClaims)) {
            incrMetric("unverified_extraction_succeeded", 1L, Optional.empty());
            return Optional.of(new ExtractedClaims(
                claimName,
                mapping,
                bodyClaims,
                buildFooterClaim(deserializer, footerBytes)
            ));
          }
        }
      }
    }
    incrMetric("unverified_extraction_failed", 1L, Optional.empty());
    return Optional.empty();
  }

  /**
   * Warning! This class is a part of adoption strategy go/passports-adoption-strategy.
   * It will be removed eventually.
   */
  private static class UnverifiedFooterClaims extends DefaultClaims implements FooterClaims {
    public UnverifiedFooterClaims(Map<String, Object> claims) {
      super(claims);
    }
    @Override
    public String value() {
      /* Non-json unverified footers are not supported */
      return "";
    }
  }

  /**
   * Verifies expiration from unverified body claims.
   * Warning! This method is a part of adoption strategy go/passports-adoption-strategy.
   * It will be removed eventually.
   *
   * @param bodyClaims
   */
  private boolean verifyExpiration(Claims bodyClaims) {
    Clock clock = Clock.systemDefaultZone();
    Instant now = clock.instant();
    Instant exp = bodyClaims.getExpiration();
    if (exp != null) {
      Duration allowedClockSkew = Duration.ofMillis(0);
      Instant max = now.minus(allowedClockSkew); //default value
      return !max.isAfter(exp);
    }
    return true;
  }

  /**
   * Builds footer from unverified byte array.
   * Warning! This method is a part of adoption strategy go/passports-adoption-strategy.
   * It will be removed eventually.
   *
   * @param deserializer
   * @param footerBytes
   * @return
   */
  private FooterClaims buildFooterClaim(
      Deserializer<Map<String, Object>> deserializer,
      byte[] footerBytes
  ) {
    if (footerBytes.length != 0) {
      if (footerBytes[0] == '{' && footerBytes[footerBytes.length - 1] == '}') {
        return new UnverifiedFooterClaims(deserializer.deserialize(footerBytes));
      } else {
        /* Non-json unverified footers are not supported */
        return new UnverifiedFooterClaims(Collections.emptyMap());
      }
    } else {
      return new UnverifiedFooterClaims(Collections.emptyMap());
    }
  }

  /**
   * Returns message size based on token type.
   * Warning! This method is a part of adoption strategy go/passports-adoption-strategy.
   * It will be removed eventually.
   *
   * @param payloadBytes
   * @param v
   * @return
   */
  private int getTokenMessageSize(byte[] payloadBytes, Version v) {
    if (v == Version.V1) {
      return payloadBytes.length - 256;
    } else {
      return payloadBytes.length - 64;
    }
  }

}
