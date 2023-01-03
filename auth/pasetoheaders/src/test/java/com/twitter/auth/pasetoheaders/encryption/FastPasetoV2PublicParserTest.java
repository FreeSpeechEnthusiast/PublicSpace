package com.twitter.auth.pasetoheaders.encryption;

import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.PasetoParser;
import dev.paseto.jpaseto.PasetoSignatureException;
import dev.paseto.jpaseto.Pasetos;
import dev.paseto.jpaseto.Purpose;
import dev.paseto.jpaseto.Version;
import dev.paseto.jpaseto.impl.DefaultClaims;

import static org.junit.Assert.assertThat;

public class FastPasetoV2PublicParserTest extends Stubs {

  private PublicKey publicKey = KeyUtils
      .getPublicKey("1eb9dbbbbc047c03fd70604e0071f0987e16b28b757225c11f00415d0e20b1a2");

  static Clock clockForVectors() {
    return Clock.fixed(Instant.ofEpochMilli(1544490000000L),
        ZoneOffset.UTC); // December 11, 2018 01:00:00
  }

  @Test(expected = PasetoSignatureException.class)
  public void invalidPublicKeyDecode() {

    PublicKey wrongPublicKey = KeyUtils
        .getPublicKey("1111111111111111111111111111111111111111111111111111111111111111");

    String token = "v2.public.eyJkYXRhIjoidGhpcyBpcyBhIHNpZ25lZCBtZXNzYWdlIiwiZXhwIjoiMjA"
        + "xOS0wMS0wMVQwMDowMDowMCswMDowMCJ9HQr8URrGntTu7Dz9J2IF23d1M7"
        + "-9lH9xiqdGyJNvzp4angPW5Esc7C5huy_M8I8_DjJK2ZXC2SUYuOFM-Q_5Cw";

    PasetoParser parser = Pasetos.parserBuilder()
        .setClock(clockForVectors()) // December 11, 2018 01:00:00
        .setPublicKey(wrongPublicKey) // this was signed with a different key
        .build();

    // an incorrect key will generate a different auth key, and will fail before the cipher
    parser.parse(token);
  }

  /**
   * Test is based on official JPASETO test vector #1
   * <a href="https://github.com/paseto-toolkit/jpaseto/blob/7cb92c05d8457822b0e34927c97a8d7ed6e52edd/integration-tests/src/main/groovy/dev/paseto/jpaseto/its/V2PublicIT.groovy#L60">...</a>
   * and designed to proof compatibility with the original library
   */
  @Test
  public void parse1() {
    PasetoParser pasetoParser = new FastPasetoV2PublicParserBuilder()
        .setClock(clockForVectors())
        .setPublicKey(publicKey)
        .build();

    String token =
        "v2.public.eyJkYXRhIjoidGhpcyBpcyBhIHNpZ25lZCBtZXNzYWdlIiwiZXhwIjoiMjAx"
            + "OS0wMS0wMVQwMDowMDowMCswMDowMCJ9HQr8URrGntTu7Dz9J2IF23d1M7-9lH9xiqdGyJ"
            + "Nvzp4angPW5Esc7C5huy_M8I8_DjJK2ZXC2SUYuOFM-Q_5Cw";

    Map<String, Object> claims = new HashMap<>();
    claims.put("data", "this is a signed message");
    claims.put("exp", "2019-01-01T00:00:00+00:00");
    Paseto expectedToken = new FastPaseto(Version.V2,
        Purpose.PUBLIC,
        new DefaultClaims(claims),
        new FastPasetoFooterClaims(""));

    assertThat(pasetoParser.parse(token), PasetoMatcher.paseto(expectedToken));
  }

  /**
   * Test is based on official JPASETO test vector #2
   * <a href="https://github.com/paseto-toolkit/jpaseto/blob/7cb92c05d8457822b0e34927c97a8d7ed6e52edd/integration-tests/src/main/groovy/dev/paseto/jpaseto/its/V2PublicIT.groovy#L60">...</a>
   * and designed to proof compatibility with the original library
   */
  @Test
  public void parse2() {
    PasetoParser pasetoParser = new FastPasetoV2PublicParserBuilder()
        .setClock(clockForVectors())
        .setPublicKey(publicKey)
        .build();

    String token =
        "v2.public.eyJkYXRhIjoidGhpcyBpcyBhIHNpZ25lZCBtZXNzYWdlIiwiZXhwIjoiMjAxOS0wMS0wMVQwMD"
            + "owMDowMCswMDowMCJ9flsZsx_gYCR0N_Ec2QxJFFpvQAs7h9HtKwbVK2n1MJ3Rz-hwe8KUqjnd8FAnIJZ601"
            + "tp7lGkguU63oGbomhoBw.eyJraWQiOiJ6VmhNaVBCUDlmUmYyc25FY1Q3Z0ZUaW9lQTlDT2NOeTlEZmdMMVc"
            + "2MGhhTiJ9";

    Map<String, Object> claims = new HashMap<>();
    claims.put("data", "this is a signed message");
    claims.put("exp", "2019-01-01T00:00:00+00:00");

    Map<String, Object> footerClaims = new HashMap<>();
    footerClaims.put("kid", "zVhMiPBP9fRf2snEcT7gFTioeA9COcNy9DfgL1W60haN");

    Paseto expectedToken = new FastPaseto(Version.V2,
        Purpose.PUBLIC,
        new DefaultClaims(claims),
        new FastPasetoFooterClaims(footerClaims));

    assertThat(pasetoParser.parse(token), PasetoMatcher.paseto(expectedToken));
  }

  /**
   * Test is based on official JPASETO test vector #3
   */
  @Test(expected = PasetoSignatureException.class)
  public void parse3() {

    PublicKey wrongPublicKey = KeyUtils
        .getPublicKey("707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f");

    PasetoParser pasetoParser = new FastPasetoV2PublicParserBuilder()
        .setClock(clockForVectors())
        .setPublicKey(wrongPublicKey)
        .build();

    String token =
        "v2.public.eyJpbnZhbGlkIjoidGhpcyBzaG91bGQgbmV2ZXIgZGVjb2RlIn1kgrdAMxcO3wFKXJrLa1cq"
            + "-DB6V_b25KQ1hV_jpOS-uYBmsg8EMS4j6kl2g83iRsh73knLGr7Ik1AEOvUgyw0P.eyJraWQiOiJ6V"
            + "mhNaVBCUDlmUmYyc25FY1Q3Z0ZUaW9lQTlDT2NOeTlEZmdMMVc2MGhhTiJ9";

    Map<String, Object> claims = new HashMap<>();
    claims.put("data", "this is a signed message");
    claims.put("exp", "2019-01-01T00:00:00+00:00");

    Map<String, Object> footerClaims = new HashMap<>();

    Paseto expectedToken = new FastPaseto(
        Version.V2,
        Purpose.PUBLIC,
        new DefaultClaims(claims),
        new FastPasetoFooterClaims(footerClaims));

    assertThat(pasetoParser.parse(token), PasetoMatcher.paseto(expectedToken));
  }

}
