package com.twitter.auth.pasetoheaders.encryption;

import java.security.PrivateKey;
import java.util.Map;

import dev.paseto.jpaseto.impl.crypto.JcaV2PublicCryptoProvider;
import dev.paseto.jpaseto.impl.crypto.V2PublicCryptoProvider;
import dev.paseto.jpaseto.io.Serializer;
import dev.paseto.jpaseto.lang.Services;

/**
 * Class is based on dev.paseto.jpaseto.impl.DefaultPasetoV2PublicBuilder
 * https://github.com/paseto-toolkit/jpaseto/blob/74420edbc1180cfdd14bce36ef920407b55c4828/impl/src/main/java/dev/paseto/jpaseto/impl/DefaultPasetoV2PublicBuilder.java
 * but token builder is detached separated
 */
public class FastPasetoV2PublicBuilder {

  private static final String HEADER = "v2.public.";

  private final V2PublicCryptoProvider cryptoProvider;

  public FastPasetoV2PublicBuilder() {
    this(Services.loadFirst(V2PublicCryptoProvider.class, new JcaV2PublicCryptoProvider()));
  }

  private FastPasetoV2PublicBuilder(V2PublicCryptoProvider cryptoProvider) {
    this.cryptoProvider = cryptoProvider;
  }

  private Serializer<Map<String, Object>> serializer;

  /**
   * Returns serializer
   * @return
   */
  public Serializer<Map<String, Object>> getSerializer() {
    // if null just return the first service
    if (serializer == null) {
      return Services.loadFirst(Serializer.class);
    }
    return serializer;
  }

  /**
   * Sets serializer and returns original instance of object
   * @return
   */
  public FastPasetoV2PublicBuilder setSerializer(Serializer<Map<String, Object>> newSerializer) {
    this.serializer = newSerializer;
    return this;
  }

  /**
   * Returns string form of signed Paseto v2 Public token
   * @return
   */
  public String compact(FastPasetoTokenBuilder t, PrivateKey privateKey) {
    t.setSerializer(this.getSerializer());

    byte[] payload = t.payloadAsBytes();
    byte[] footer = t.footerAsBytes();

    byte[] signature = cryptoProvider.sign(payload, footer, privateKey);

    return HEADER + t.noPadBase64(payload, signature) + t.footerToString(footer);
  }
}
