package com.twitter.auth.pasetoheaders.encryption;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import dev.paseto.jpaseto.RequiredTypeException;
import dev.paseto.jpaseto.lang.DateFormats;

/**
 * Class is based on dev.paseto.jpaseto.impl.ClaimsMap
 * https://github.com/paseto-toolkit/jpaseto/blob/74420edbc1180cfdd14bce36ef920407b55c4828/impl/src/main/java/dev/paseto/jpaseto/impl/ClaimsMap.java
 * The copy is required due the fact that the original class is not public and required for FastPasetoParser
 */
abstract class FastPasetoClaimsMap implements Map<String, Object> {
  /*
  CONTENT BELOW REMAINS AS IS
  */
  private static final String CONVERSION_ERROR_MSG =
      "Cannot convert existing claim value of type '%s' to desired type "
      + "'%s'. JPaseto only converts simple String, Instant, Date,"
      + "Long, Integer, Short and Byte types automatically. "
      + "Anything more complex is expected to be already converted to"
      + "your desired type by the JSON Deserializer "
      + "implementation. You may specify a custom Deserializer for "
      + "a JwtParser with the desired conversion "
      + "configuration via the PasetoParserBuilder.setDeserializer() method. "
      + "See https://github.com/paseto-toolkit/jpaseto#custom-json "
      + "for more information. If using Jackson, you can "
      + "specify custom claim POJO types as described in "
      + "https://github.com/paseto-toolkit/jpaseto#json-jackson-custom-types";

  private final Map<String, Object> claims;

  FastPasetoClaimsMap() {
    this(new HashMap<>());
  }

  public FastPasetoClaimsMap(Map<String, Object> claims) {
    this.claims = claims;
  }

  @Override
  public int size() {
    return claims.size();
  }

  @Override
  public boolean isEmpty() {
    return claims.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return claims.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return claims.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return claims.get(key);
  }

  @Override
  public Object put(String key, Object value) {
    if (value == null) {
      return claims.remove(key);
    } else {
      return claims.put(key, value);
    }
  }

  @Override
  public Object remove(Object key) {
    return claims.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    if (m == null) {
      return;
    }
    m.forEach(this::put);
  }

  @Override
  public void clear() {
    claims.clear();
  }

  @Override
  public Set<String> keySet() {
    return claims.keySet();
  }

  @Override
  public Collection<Object> values() {
    return claims.values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return claims.entrySet();
  }

  protected <T> T get(String claimName, Class<T> requiredType) {
    Object value = get(claimName);
    if (value == null) {
      return null;
    }

    if (Instant.class.equals(requiredType)) {
      if (isSpecDate(claimName)) {
        value = toSpecDate(value, claimName);
      } else {
        value = toInstant(value, claimName);
      }
    }

    if (Date.class.equals(requiredType)) {
      if (isSpecDate(claimName)) {
        value = Date.from(toSpecDate(value, claimName));
      } else {
        value = Date.from(toInstant(value, claimName));
      }
    }

    return castClaimValue(value, requiredType);
  }

  <T> T castClaimValue(Object value, Class<T> requiredType) {

    Object newValue = value;

    if (value instanceof Integer) {
      int intValue = (Integer) value;
      if (requiredType == Long.class) {
        newValue = (long) intValue;
      } else if (requiredType == Short.class
          && Short.MIN_VALUE <= intValue && intValue <= Short.MAX_VALUE) {
        newValue = (short) intValue;
      } else if (requiredType == Byte.class
          && Byte.MIN_VALUE <= intValue && intValue <= Byte.MAX_VALUE) {
        newValue = (byte) intValue;
      }
    }

    if (!requiredType.isInstance(newValue)) {
      throw new RequiredTypeException(
          String.format(CONVERSION_ERROR_MSG, newValue.getClass(), requiredType));
    }

    return requiredType.cast(newValue);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FastPasetoClaimsMap that = (FastPasetoClaimsMap) o;
    return Objects.equals(claims, that.claims);
  }

  @Override
  public int hashCode() {
    return Objects.hash(claims);
  }

  public String toString() {
    return claims.toString();
  }

  protected static Instant toSpecDate(Object v, String name) {
    if (v == null) {
      return null;
    }
    return toInstant(v, name);
  }

  protected static Instant toInstant(Object v, String name) {
    if (v == null) {
      return null;
    } else if (v instanceof Instant) {
      return (Instant) v;
    } else if (v instanceof Date) {
      return ((Date) v).toInstant();
    } else if (v instanceof Calendar) {
      return ((Calendar) v).toInstant();
    } else if (v instanceof Number) {
      //assume millis:
      long millis = ((Number) v).longValue();
      return Instant.ofEpochMilli(millis);
    } else if (v instanceof String) {
      return parseIso8601Date((String) v, name); //ISO-8601 parsing since 0.10.0
    } else {
      throw new IllegalStateException("Cannot create Date from '"
          + name
          + "' value '"
          + v
          + "'.");
    }
  }

  private static Instant parseIso8601Date(String s, String name) throws IllegalArgumentException {
    try {
      return DateFormats.parseIso8601Date(s);
    } catch (DateTimeException e) {
      String msg = "'" + name + "' value does not appear to be ISO-8601-formatted: " + s;
      throw new IllegalArgumentException(msg, e);
    }
  }

  protected boolean isSpecDate(String claimName) {
    return false;
  }
}

