package com.twitter.auth.pasetoheaders.encryption;

import java.util.Objects;

import dev.paseto.jpaseto.Claims;
import dev.paseto.jpaseto.FooterClaims;
import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.Purpose;
import dev.paseto.jpaseto.Version;

/**
 * Class is based on dev.paseto.jpaseto.impl.DefaultPaseto
 * https://github.com/paseto-toolkit/jpaseto/blob/74420edbc1180cfdd14bce36ef920407b55c4828/impl/src/main/java/dev/paseto/jpaseto/impl/DefaultPaseto.java
 * The copy is required due the fact that the original class is not public and required for FastPasetoParser
 */
public class FastPaseto implements Paseto {
  /*
  CONTENT BELOW REMAINS AS IS
  */
  private final Version version;
  private final Purpose purpose;
  private final Claims payload;
  private final FooterClaims footer;

  FastPaseto(Version version, Purpose purpose, Claims payload, FooterClaims footer) {
    this.version = version;
    this.purpose = purpose;
    this.payload = payload;
    this.footer = footer;
  }

  @Override
  public Version getVersion() {
    return version;
  }

  @Override
  public Purpose getPurpose() {
    return purpose;
  }

  @Override
  public Claims getClaims() {
    return payload;
  }

  @Override
  public FooterClaims getFooter() {
    return footer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FastPaseto that = (FastPaseto) o;
    return version == that.version
        && purpose == that.purpose
        && Objects.equals(payload, that.payload)
        && Objects.equals(footer, that.footer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, purpose, payload, footer);
  }
}
