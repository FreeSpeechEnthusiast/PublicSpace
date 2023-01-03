package com.twitter.auth.pasetoheaders.encryption;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import dev.paseto.jpaseto.FooterClaims;

public class FastPasetoFooterClaims extends FastPasetoClaimsMap implements FooterClaims {
  /*
  CONTENT BELOW REMAINS AS IS
  */
  private final String value;

  FastPasetoFooterClaims(Map<String, Object> claims) {
    this(claims, dev.paseto.jpaseto.lang.Collections.isEmpty(claims) ? "" : null);
  }

  FastPasetoFooterClaims(String value) {
    this(null, value);
  }

  private FastPasetoFooterClaims(Map<String, Object> claims, String value) {
    super(claims != null
        ? Collections.unmodifiableMap(claims)
        : Collections.emptyMap());
    this.value = value;
  }

  @Override
  public <T> T get(String claimName, Class<T> requiredType) {
    return super.get(claimName, requiredType);
  }

  @Override
  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    FastPasetoFooterClaims that = (FastPasetoFooterClaims) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }
}
