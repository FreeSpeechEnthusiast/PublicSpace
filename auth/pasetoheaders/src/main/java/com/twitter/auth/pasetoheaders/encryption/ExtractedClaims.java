package com.twitter.auth.pasetoheaders.encryption;

import java.time.Instant;

import dev.paseto.jpaseto.Claims;
import dev.paseto.jpaseto.FooterClaims;

/**
 * Wrapper for Paseto Claims
 * @param <T>
 */
public class ExtractedClaims<T> {
  private Claims claims;
  private FooterClaims footerClaims;
  private String claimName;
  private ClaimMapping mapping;

  public ExtractedClaims(
      String claimName, ClaimMapping mapping,
      Claims claims, FooterClaims footerClaims
  ) {
    this.claimName = claimName;
    this.mapping = mapping;
    this.claims = claims;
    this.footerClaims = footerClaims;
  }
  public T getEnclosedEntity() {
    return (T) claims.get(claimName, mapping.getClaimType(claimName));
  }
  public String getIssuer() {
    return claims.getIssuer();
  }
  public String getSubject() {
    return claims.getSubject();
  }
  public String getAudience() {
    return claims.getAudience();
  }
  public Instant getExpiration() {
    return claims.getExpiration();
  }
  public Instant getNotBefore() {
    return claims.getNotBefore();
  }
  public Instant getIssuedAt() {
    return claims.getIssuedAt();
  }
  public String getTokenId() {
    return claims.getTokenId();
  }
  public String getKeyId() {
    return footerClaims.getKeyId();
  }
  public Integer getModelVersion() {
    return footerClaims.get("modelVersion", Integer.class);
  }
}
