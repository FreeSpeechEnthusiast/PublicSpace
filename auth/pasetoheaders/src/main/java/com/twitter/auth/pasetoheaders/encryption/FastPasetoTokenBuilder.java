package com.twitter.auth.pasetoheaders.encryption;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import dev.paseto.jpaseto.Claims;
import dev.paseto.jpaseto.FooterClaims;
import dev.paseto.jpaseto.impl.lang.Bytes;
import dev.paseto.jpaseto.io.Serializer;
import dev.paseto.jpaseto.lang.Collections;
import dev.paseto.jpaseto.lang.Strings;

/**
 * Class is based on dev.paseto.jpaseto.impl.DefaultPasetoV2PublicBuilder
 * https://github.com/paseto-toolkit/jpaseto/blob/74420edbc1180cfdd14bce36ef920407b55c4828/impl/src/main/java/dev/paseto/jpaseto/impl/DefaultPasetoV2PublicBuilder.java
 * but class loader overhead is detached
 */
public class FastPasetoTokenBuilder {

  private final Map<String, Object> payload = new HashMap<>();
  private final Map<String, Object> footer = new HashMap<>();
  private String footerString = null;

  private Serializer<Map<String, Object>> serializer;

  public FastPasetoTokenBuilder claim(String key, Object value) {
    payload.put(key, value);
    return this;
  }

  public FastPasetoTokenBuilder footerClaim(String key, Object value) {
    footer.put(key, value);
    return this;
  }

  /**
   * Sets plain text footer and returns original instance of object
   * @return
   */
  public FastPasetoTokenBuilder setFooter(String newFooter) {
    this.footerString = newFooter;
    return this;
  }

  /**
   * Returns serializer
   * @return
   */
  protected Serializer<Map<String, Object>> getSerializer() {
    return serializer;
  }

  /**
   * Sets serializer and returns original instance of object
   * @return
   */
  public FastPasetoTokenBuilder setSerializer(Serializer<Map<String, Object>> newSerializer) {
    this.serializer = newSerializer;
    return this;
  }

  protected String footerToString(byte[] footerData) {

    if (footerData == null || footerData.length == 0) {
      return "";
    }

    return "." + noPadBase64(footerData);
  }

  protected String noPadBase64(byte[]... inputs) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(Bytes.concat(inputs));
  }

  protected byte[] payloadAsBytes() {
    return getSerializer().serialize(getPayload());
  }

  protected byte[] footerAsBytes() {

    if (Strings.hasText(getFooterString())) {
      return getFooterString().getBytes(StandardCharsets.UTF_8);
    }

    Map<String, Object> tmpFooter = getFooter();
    if (!Collections.isEmpty(tmpFooter)) {
      return getSerializer().serialize(tmpFooter);
    }

    return new byte[0];
  }

  protected Map<String, Object> getPayload() {
    return payload;
  }

  protected Map<String, Object> getFooter() {
    return footer;
  }

  protected String getFooterString() {
    return footerString;
  }

  /**
   * Sets Issuer and returns original instance of object
   * @return
   */
  public FastPasetoTokenBuilder setIssuer(String newIss) {
    claim(Claims.ISSUER, newIss);
    return this;
  }

  public FastPasetoTokenBuilder setSubject(String newSub) {
    claim(Claims.SUBJECT, newSub);
    return this;
  }

  /**
   * Sets Audience and returns original instance of object
   * @return
   */
  public FastPasetoTokenBuilder setAudience(String newAud) {
    claim(Claims.AUDIENCE, newAud);
    return this;
  }

  /**
   * Sets Expiration and returns original instance of object
   * @return
   */
  public FastPasetoTokenBuilder setExpiration(Instant newExp) {
    claim(Claims.EXPIRATION, newExp);
    return this;
  }

  /**
   * Sets Not Before and returns original instance of object
   * @return
   */
  public FastPasetoTokenBuilder setNotBefore(Instant newNbf) {
    claim(Claims.NOT_BEFORE, newNbf);
    return this;
  }

  /**
   * Sets Issued At and returns original instance of object
   * @return
   */
  public FastPasetoTokenBuilder setIssuedAt(Instant newIat) {
    claim(Claims.ISSUED_AT, newIat);
    return this;
  }

  /**
   * Sets Token Id and returns original instance of object
   * @return
   */
  public FastPasetoTokenBuilder setTokenId(String newJti) {
    claim(Claims.TOKEN_ID, newJti);
    return this;
  }

  /**
   * Sets Key Id and returns original instance of object
   * @return
   */
  public FastPasetoTokenBuilder setKeyId(String newKid) {
    footerClaim(FooterClaims.KEY_ID, newKid);
    return this;
  }

}
