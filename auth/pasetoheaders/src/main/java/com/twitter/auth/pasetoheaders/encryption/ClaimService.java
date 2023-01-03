package com.twitter.auth.pasetoheaders.encryption;

import java.security.PublicKey;
import java.util.Collections;
import java.util.Optional;

import dev.paseto.jpaseto.Claims;
import dev.paseto.jpaseto.FooterClaims;
import dev.paseto.jpaseto.KeyResolverAdapter;
import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.PasetoParser;
import dev.paseto.jpaseto.Purpose;
import dev.paseto.jpaseto.Version;

public final class ClaimService<T> extends UnsafeExtraction {
  private String claimName;
  private KeyProvider keyProvider;
  private ClaimMapping mapping;
  private PasetoParser parser;

  public ClaimService(String claimName,
                      Class<T> entityClass,
                      KeyProvider keyProvider,
                      Optional<LoggingInterface> logger,
                      Optional<StatsInterface> stats) {
    this.claimName = claimName;
    this.keyProvider = keyProvider;
    this.mapping = new ClaimMapping();
    this.logger = logger;
    this.stats = stats;
    this.serviceName = "claim_service";
    mapping.registerClaimMapping(claimName, entityClass);
    /*
     * Prepare PASETO parser using keyIdentifier received from unencrypted token's footer
     */
    parser = new FastPasetoV2PublicParserBuilder()
        .setDeserializer(mapping.getDeserializer())
        .setKeyResolver(new KidBasedKeyResolver())
        .build();
  }

  /**
   * Extract claim from PASETO token
   *
   * @param token PASETO token
   * @return claims content or Optional.empty if token invalid
   */
  public Optional<ExtractedClaims<T>> extractClaims(String token) {
    incrMetric("extraction_requested", 1L, Optional.empty());
    /*
     * Read encrypted data
     */
    try {
      Paseto parsedToken = parser.parse(token);
      Claims claims = parsedToken.getClaims();
      FooterClaims footerClaims = parsedToken.getFooter();
      incrMetric("extraction_succeeded", 1L, Optional.empty());
      return Optional.of(new ExtractedClaims<T>(claimName, mapping, claims, footerClaims));
    } catch (Exception e) {
      error(e.getMessage(),
          Optional.empty());
      incrMetric("extraction_failed", 1L, Optional.empty());
      return Optional.empty();
    }
  }

  /**
   * Extracts unverified claim from PASETO token.
   * Warning! This method is a part of adoption strategy go/passports-adoption-strategy.
   * It will be removed eventually.
   *
   * @param token PASETO token
   *
   * @return claims content or Optional.empty if token invalid
   */
  public Optional<ExtractedClaims<T>> extractUnverifiedClaims(
      String token
  ) {
    return this.extractClaimsUsingUnsafeExtraction(claimName, mapping, token);
  }

  /**
   * Connects unencrypted footer claim with KeyProvider
   */
  private class KidBasedKeyResolver extends KeyResolverAdapter {

    @Override
    public PublicKey resolvePublicKey(Version pasetoTokenVersion,
                                      Purpose purpose,
                                      FooterClaims footer) {
      /*
       Extract key information from token's unencrypted kid (Key-ID) claim
       such as environment, issuer, version

       see https://paseto.io/rfc/ for more information
      */
      Optional<KeyIdentifier> keyIdentifier = KeyIdentifier.parseFromKeyIdClaim(footer.getKeyId());
      if (!keyIdentifier.isPresent()) {
        /*
        Throws the exception if ClaimService is not able parse
        information stored kid claim
        */
        String errorMessage = "Unable to parse key identifier from the token footer";
        error(errorMessage,
            Optional.of(Collections.singletonMap("ftr", footer.getKeyId())));
        incrMetric("wrong_key_identifier", 1L, Optional.empty());
        throw new RuntimeException(errorMessage);
      }
      Optional<PublicKey> key = keyProvider.getPublicKey(keyIdentifier.get());
      if (!key.isPresent()) {
        /*
        Throws the exception if KeyProvider is not able to find a public key
        based on kid claim
        */
        String errorMessage = "Unable to find public key for requested token";
        error(errorMessage,
            Optional.of(Collections.singletonMap("ftr", footer.getKeyId())));
        incrMetric("public_key_not_found", 1L, Optional.empty());
        throw new RuntimeException(errorMessage);
      }
      return key.get();
    }
  }



}
