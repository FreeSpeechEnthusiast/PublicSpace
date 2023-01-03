package com.twitter.auth.pasetoheaders.encryption;

import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import javax.crypto.SecretKey;

import dev.paseto.jpaseto.FooterClaims;
import dev.paseto.jpaseto.KeyResolver;
import dev.paseto.jpaseto.PasetoParser;
import dev.paseto.jpaseto.PasetoParserBuilder;
import dev.paseto.jpaseto.Purpose;
import dev.paseto.jpaseto.Version;
import dev.paseto.jpaseto.impl.crypto.JcaV2PublicCryptoProvider;
import dev.paseto.jpaseto.impl.crypto.V2PublicCryptoProvider;
import dev.paseto.jpaseto.io.Deserializer;
import dev.paseto.jpaseto.lang.Assert;
import dev.paseto.jpaseto.lang.Services;

/**
 * Class is based on dev.paseto.jpaseto.impl.DefaultPasetoParserBuilder
 * https://github.com/paseto-toolkit/jpaseto/blob/74420edbc1180cfdd14bce36ef920407b55c4828/impl/src/main/java/dev/paseto/jpaseto/impl/DefaultPasetoParserBuilder.java
 * the only difference is that cryptoProvider is added to a constructor and then reused in the parser
 *
 * Warning! This implementation supports V2 Public tokens only
 */
public class FastPasetoV2PublicParserBuilder implements PasetoParserBuilder {

  private final V2PublicCryptoProvider v2PublicCryptoProvider;

  public FastPasetoV2PublicParserBuilder() {
    this(Services.loadFirst(V2PublicCryptoProvider.class, new JcaV2PublicCryptoProvider()));
  }

  private FastPasetoV2PublicParserBuilder(V2PublicCryptoProvider v2PublicCryptoProvider) {
    this.v2PublicCryptoProvider = v2PublicCryptoProvider;
  }

  public Deserializer<Map<String, Object>> getDeserializer() {
    return this.deserializer;
  }

  /*
  CONTENT BELOW REMAINS AS IS
  */

  private PublicKey publicKey = null;
  private SecretKey sharedSecret = null;
  private KeyResolver keyResolver = null;
  private Deserializer<Map<String, Object>> deserializer;
  private Clock clock = Clock.systemUTC();
  private Duration allowedClockSkew = Duration.ofMillis(0);

  private final Map<String, Predicate<Object>> expectedClaimsMap = new HashMap<>();
  private final Map<String, Predicate<Object>> expectedFooterClaimsMap = new HashMap<>();

  @Override
  public PasetoParserBuilder setKeyResolver(KeyResolver newKeyResolver) {
    this.keyResolver = newKeyResolver;
    return this;
  }

  @Override
  public PasetoParserBuilder setSharedSecret(SecretKey newSharedSecret) {
    this.sharedSecret = newSharedSecret;
    return this;
  }

  @Override
  public PasetoParserBuilder setPublicKey(PublicKey newPublicKey) {
    this.publicKey = newPublicKey;
    return this;
  }

  @Override
  public PasetoParserBuilder setDeserializer(Deserializer<Map<String, Object>> newDeserializer) {
    this.deserializer = newDeserializer;
    return this;
  }

  @Override
  public PasetoParser build() {

    Assert.isTrue(keyResolver != null
        || publicKey != null
        || sharedSecret != null,
        "PasetoParser must be configure with a public key"
    + "(for public tokens) and/or a sharedSecret (for local tokens).");

    @SuppressWarnings("unchecked")
    Deserializer<Map<String, Object>> tmpDeserializer = (this.deserializer != null)
        ? this.deserializer
        : Services.loadFirst(Deserializer.class);

    // validate we have either a private key or shared key OR the KeyResolver set, NOT both
    boolean hasDirectKeys = publicKey != null || sharedSecret != null;
    if (hasDirectKeys && keyResolver != null) {
      throw new IllegalStateException(
          "Both a KeyResolver and a publicKey/sharedSecret cannot "
          + "be used together, use one or the other");
    }

    KeyResolver tmpKeyResolver = keyResolver != null
        ? keyResolver
        : new FastPasetoV2PublicParserBuilder.SimpleKeyResolver(publicKey, sharedSecret);

    return new FastPasetoV2PublicParser(
        v2PublicCryptoProvider,
        tmpKeyResolver,
        tmpDeserializer,
        clock,
        allowedClockSkew,
        expectedClaimsMap,
        expectedFooterClaimsMap);
  }


  @Override
  public PasetoParserBuilder require(String claimName, Predicate<Object> value) {
    Assert.hasText(claimName, "claim name cannot be null or empty.");
    Assert.notNull(value, "The value cannot be null for claim name: " + claimName);
    expectedClaimsMap.put(claimName, value);
    return this;
  }

  @Override
  public PasetoParserBuilder requireFooter(String claimName, Predicate<Object> value) {
    Assert.hasText(claimName, "claim name cannot be null or empty.");
    Assert.notNull(value, "The value cannot be null for claim name: " + claimName);
    expectedFooterClaimsMap.put(claimName, value);
    return this;
  }

  @Override
  public PasetoParserBuilder setClock(Clock newClock) {
    Assert.notNull(newClock, "Clock instance cannot be null.");
    this.clock = newClock;
    return this;
  }

  @Override
  public PasetoParserBuilder setAllowedClockSkew(Duration newAllowedClockSkewMillis) {
    this.allowedClockSkew = newAllowedClockSkewMillis;
    return this;
  }

  public static final class SimpleKeyResolver implements KeyResolver {

    private final PublicKey publicKey;
    private final SecretKey sharedSecret;

    private SimpleKeyResolver(PublicKey publicKey, SecretKey sharedSecret) {
      this.publicKey = publicKey;
      this.sharedSecret = sharedSecret;
    }

    @Override
    public PublicKey resolvePublicKey(Version version, Purpose purpose, FooterClaims footer) {
      Assert.notNull(publicKey,
          "A public key has not been configured.  A public key must be configured in "
          + "'Pasetos.parserBuilder().setPublicKey(...)' or "
          + "Pasetos.parserBuilder().setKeyResolver(...)");
      return publicKey;
    }

    @Override
    public SecretKey resolveSharedKey(Version version, Purpose purpose, FooterClaims footer) {
      Assert.notNull(sharedSecret,
          "A shared secret has not been configured.  A shared secret must be configured in "
          + "'Pasetos.parserBuilder().setSharedSecret(...)' or "
          + "Pasetos.parserBuilder().setKeyResolver(...)");
      return sharedSecret;
    }

    public PublicKey getPublicKey() {
      return publicKey;
    }

    public SecretKey getSharedSecret() {
      return sharedSecret;
    }
  }
}
