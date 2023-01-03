package com.twitter.auth.pasetoheaders.encryption;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import dev.paseto.jpaseto.FooterClaims;
import dev.paseto.jpaseto.io.Serializer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FastPasetoTokenBuilderTest {

  @Test
  public void setIssuer() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    String expectedValue = "test-issuer";
    tokenBuilder.setIssuer(expectedValue);
    verify(tokenBuilder).claim("iss", expectedValue);
  }

  @Test
  public void setAudience() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    String expectedValue = "test-audience";
    tokenBuilder.setAudience(expectedValue);
    verify(tokenBuilder).claim("aud", expectedValue);
  }

  @Test
  public void setIssuedAt() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    Instant expectedValue = Instant.now();
    tokenBuilder.setIssuedAt(expectedValue);
    verify(tokenBuilder).claim("iat", expectedValue);
  }

  @Test
  public void setTokenId() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    String expectedValue = "test-tokenId";
    tokenBuilder.setTokenId(expectedValue);
    verify(tokenBuilder).claim("jti", expectedValue);
  }

  @Test
  public void setSubject() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    String expectedValue = "test-subject";
    tokenBuilder.setSubject(expectedValue);
    verify(tokenBuilder).claim("sub", expectedValue);
  }

  @Test
  public void setExpiration() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    Instant expectedValue = Instant.now();
    tokenBuilder.setExpiration(expectedValue);
    verify(tokenBuilder).claim("exp", expectedValue);
  }

  @Test
  public void setNotBefore() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    Instant expectedValue = Instant.now();
    tokenBuilder.setNotBefore(expectedValue);
    verify(tokenBuilder).claim("nbf", expectedValue);
  }

  @Test
  public void setKeyId() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    String expectedValue = "test-keyId";
    tokenBuilder.setKeyId(expectedValue);
    verify(tokenBuilder).footerClaim("kid", expectedValue);
  }

  @Test
  public void setSerializer() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    Serializer<Map<String, Object>> customSerializer = fastPasetoTokenBuilder -> new byte[0];
    tokenBuilder.setSerializer(customSerializer);
    assertEquals(tokenBuilder.getSerializer(), customSerializer);
  }

  @Test
  public void setFooterString() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    String expectedValue = "foo";
    tokenBuilder.setFooter(expectedValue);
    assertEquals(tokenBuilder.getFooterString(), expectedValue);
  }

  @Test
  public void footerAsBytes() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    String expectedValue = "foo";
    tokenBuilder.setFooter(expectedValue);
    assertArrayEquals(tokenBuilder.footerAsBytes(),
        expectedValue.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void footerAsBytesWithEmptyFooter() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    assertArrayEquals(tokenBuilder.footerAsBytes(), new byte[0]);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void footerAsBytesWithObjectFooter() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    String expectedValue = "test-keyId";
    tokenBuilder.setKeyId(expectedValue);
    // mock serializer
    Serializer<Map<String, Object>> serializerMock =
        (Serializer<Map<String, Object>>) Mockito.mock(Serializer.class);
    when(serializerMock.serialize(Collections.singletonMap(FooterClaims.KEY_ID, expectedValue)))
        .thenReturn(expectedValue.getBytes());
    tokenBuilder.setSerializer(serializerMock);
    assertArrayEquals(tokenBuilder.footerAsBytes(),
        expectedValue.getBytes(StandardCharsets.UTF_8));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void payloadAsBytes() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    Map<String, Long> entity = Collections.singletonMap("passportId", 1L);
    tokenBuilder.claim("passport", entity);
    String expectedValue = "passport1";
    // mock serializer
    Serializer<Map<String, Object>> serializerMock =
        (Serializer<Map<String, Object>>) Mockito.mock(Serializer.class);
    when(serializerMock.serialize(Collections.singletonMap("passport", entity)))
        .thenReturn(expectedValue.getBytes(StandardCharsets.UTF_8));
    tokenBuilder.setSerializer(serializerMock);
    assertArrayEquals(tokenBuilder.payloadAsBytes(),
        expectedValue.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void footerToStringWithEmptyString() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    String expectedValue = "";
    assertEquals(tokenBuilder.footerToString(expectedValue.getBytes()),
        expectedValue);
  }

  @Test
  public void footerToString() {
    FastPasetoTokenBuilder tokenBuilder = Mockito.spy(new FastPasetoTokenBuilder());
    String expectedValue = ".eyJraWQiOjEyM30";
    assertEquals(tokenBuilder.footerToString("{\"kid\":123}".getBytes()), expectedValue);
  }
}
