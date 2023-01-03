package com.twitter.auth.pasetoheaders.encryption;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import dev.paseto.jpaseto.io.Serializer;

import static org.junit.Assert.assertEquals;

public class FastPasetoV2PublicBuilderTest extends Stubs {

  @Test
  public void setSerializer() {
    FastPasetoV2PublicBuilder pasetoServiceBuilder = Mockito.spy(new FastPasetoV2PublicBuilder());
    Serializer<Map<String, Object>> customSerializer = fastPasetoTokenBuilder -> new byte[0];
    pasetoServiceBuilder.setSerializer(customSerializer);
    assertEquals(pasetoServiceBuilder.getSerializer(), customSerializer);
  }

  /**
   * Test is based on official JPASETO test vector #1
   * <a href="https://github.com/paseto-toolkit/jpaseto/blob/7cb92c05d8457822b0e34927c97a8d7ed6e52edd/integration-tests/src/main/groovy/dev/paseto/jpaseto/its/V2PublicIT.groovy#L60">...</a>
   * and designed to proof compatibility with the original library
   */
  @Test
  public void compact1() {
    FastPasetoV2PublicBuilder pasetoServiceBuilder = new FastPasetoV2PublicBuilder();
    String expectedToken =
        "v2.public.eyJkYXRhIjoidGhpcyBpcyBhIHNpZ25lZCBtZXNzYWdlIiwiZXhwIjoiMjAx"
        + "OS0wMS0wMVQwMDowMDowMCswMDowMCJ9HQr8URrGntTu7Dz9J2IF23d1M7-9lH9xiqdGyJ"
        + "Nvzp4angPW5Esc7C5huy_M8I8_DjJK2ZXC2SUYuOFM-Q_5Cw";
    PrivateKey privateKey =
        KeyUtils.getPrivateKey("b4cbfb43df4ce210727d953e4a713307fa19bb7d9f85041438d9e11b942a3774");
    assertEquals(pasetoServiceBuilder
        .compact(
            (new FastPasetoTokenBuilder())
                .setExpiration(
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        .parse("2019-01-01T00:00:00+00:00", Instant::from)
                )
                .claim("data", "this is a signed message"), privateKey), expectedToken);
  }

  /**
   * Test is based on official JPASETO test vector #2
   * <a href="https://github.com/paseto-toolkit/jpaseto/blob/7cb92c05d8457822b0e34927c97a8d7ed6e52edd/integration-tests/src/main/groovy/dev/paseto/jpaseto/its/V2PublicIT.groovy#L60">...</a>
   * and designed to proof compatibility with the original library
   */
  @Test
  public void compact2() {
    FastPasetoV2PublicBuilder pasetoServiceBuilder = new FastPasetoV2PublicBuilder();
    String expectedToken =
        "v2.public.eyJkYXRhIjoidGhpcyBpcyBhIHNpZ25lZCBtZXNzYWdlIiwiZXhwIjoiMjAxOS0wMS0wMVQwMD"
        + "owMDowMCswMDowMCJ9flsZsx_gYCR0N_Ec2QxJFFpvQAs7h9HtKwbVK2n1MJ3Rz-hwe8KUqjnd8FAnIJZ601t"
        + "p7lGkguU63oGbomhoBw.eyJraWQiOiJ6VmhNaVBCUDlmUmYyc25FY1Q3Z0ZUaW9lQTlDT2NOeTlEZmdMMVc2M"
        + "GhhTiJ9";
    PrivateKey privateKey =
        KeyUtils.getPrivateKey("b4cbfb43df4ce210727d953e4a713307fa19bb7d9f85041438d9e11b942a3774");
    assertEquals(pasetoServiceBuilder
        .compact(
            (new FastPasetoTokenBuilder())
                .setExpiration(
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        .parse("2019-01-01T00:00:00+00:00", Instant::from)
                )
                .setKeyId("zVhMiPBP9fRf2snEcT7gFTioeA9COcNy9DfgL1W60haN")
                .claim("data", "this is a signed message"), privateKey), expectedToken);
  }


}
