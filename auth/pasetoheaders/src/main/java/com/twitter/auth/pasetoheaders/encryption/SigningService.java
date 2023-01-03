package com.twitter.auth.pasetoheaders.encryption;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SigningService<T> extends TrackableService {
  private String claim;
  private String issuer;
  private String environment;
  private KeyProvider keyProvider;
  private ClaimMapping mapping;
  private Long tokenExpirationTTLSec = 300L;
  private Long maxTokenExpirationTTLSec = 300L;
  /**
   * To avoid class loader overhead we must initiate builder in advance
   * http://go/j/IM-4163
   */
  private FastPasetoV2PublicBuilder pasetoService;

  public SigningService(String claim,
                        Class<T> entityClass,
                        String environment,
                        String issuer,
                        KeyProvider keyProvider,
                        Optional<LoggingInterface> logger,
                        Optional<StatsInterface> stats) {
    this.claim = claim;
    this.issuer = issuer;
    this.environment = environment;
    this.keyProvider = keyProvider;
    this.mapping = new ClaimMapping();
    this.logger = logger;
    this.stats = stats;
    this.serviceName = "signing_service";
    mapping.registerClaimMapping(claim, entityClass);
    pasetoService = (new FastPasetoV2PublicBuilder())
        .setSerializer(mapping.getSerializer());
  }

  /**
   * Creates a signing service with custom token ttl bounded to maxTokenExpirationTTLSec
   *
   * @param claim
   * @param entityClass
   * @param environment
   * @param issuer
   * @param keyProvider
   * @param logger
   * @param stats
   * @param tokenExpirationTTLSec
   */
  public SigningService(String claim,
                        Class<T> entityClass,
                        String environment,
                        String issuer,
                        KeyProvider keyProvider,
                        Optional<LoggingInterface> logger,
                        Optional<StatsInterface> stats,
                        Long tokenExpirationTTLSec) {
    this.claim = claim;
    this.issuer = issuer;
    this.environment = environment;
    this.keyProvider = keyProvider;
    this.mapping = new ClaimMapping();
    this.logger = logger;
    this.stats = stats;
    this.serviceName = "signing_service";
    this.tokenExpirationTTLSec = Math.min(tokenExpirationTTLSec, maxTokenExpirationTTLSec);
    mapping.registerClaimMapping(claim, entityClass);
    pasetoService = (new FastPasetoV2PublicBuilder())
        .setSerializer(mapping.getSerializer());
  }

  public String getIssuer() {
    return this.issuer;
  }

  public String getEnvironment() {
    return this.environment;
  }

  /**
   * Generate token for specific entity using private key with optional key version
   * if optional key version is not set then the last private key will be used
   * @param entity
   * @param keyVersion
   * @return a signed token string or Optional.empty if encryption failed
   */
  public Optional<String> signToken(T entity, Optional<Integer> keyVersion) {
    incrMetric("signing_requested", 1L,
        Optional.of(serviceMetadata()));
    Optional<VersionedKey<PrivateKey>> signingKey;
    KeyIdentifier keyIdentifier;
    /**
     * If key version is present we are attempting to use private key with the specified version
     */
    if (keyVersion.isPresent()) {
      keyIdentifier = new KeyIdentifier(environment, issuer, keyVersion.get());
      Optional<PrivateKey> possibleKey = keyProvider.getPrivateKey(keyIdentifier);
      if (possibleKey.isPresent()) {
        signingKey = Optional.of(new VersionedKey<>(
            keyIdentifier,
            possibleKey.get()
        ));
      } else {
        signingKey = Optional.empty();
      }
    } else {
      /**
       * If key version is not set we are using the last available private key
       */
      signingKey = keyProvider.getLastPrivateKey(environment, issuer);
    }
    if (!signingKey.isPresent()) {
      incrMetric("private_key_not_found", 1L,
          Optional.of(serviceMetadata()));
      return Optional.empty();
    }
    keyIdentifier = signingKey.get().getKeyIdentifier();
    Instant issuedAt = Instant.now();
    String compacted = pasetoService
        .compact(new FastPasetoTokenBuilder()
          .setIssuer(this.issuer)
          //.setSubject("tbd")
          .setIssuedAt(issuedAt)
          .setExpiration(issuedAt.plus(tokenExpirationTTLSec, ChronoUnit.SECONDS))
          //.setExpiration(issuedAt.plus(10*365*24*60*60, ChronoUnit.SECONDS))
          // unencrypted claim for key identification and rotation
          // contains environment, issuer, key_version
          .setKeyId(keyIdentifier.getKeyId())
          //TODO: reserved for future
          .footerClaim("modelVersion", 1)
          //Important! setFooter removes other footer claim
          //.setFooter("")
          .claim(claim, entity),
        signingKey.get().getKey());
    incrMetric("signing_completed", 1L,
        Optional.of(serviceMetadata()));
    incrMetric("token_size", (long) compacted.length(),
        Optional.of(serviceMetadata()));
    trace(compacted,  Optional.of(serviceMetadata()));
    return Optional.of(compacted);
  }

  private Map<String, String> serviceMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("iss", issuer);
    metadata.put("env", environment);
    return metadata;
  }

}
