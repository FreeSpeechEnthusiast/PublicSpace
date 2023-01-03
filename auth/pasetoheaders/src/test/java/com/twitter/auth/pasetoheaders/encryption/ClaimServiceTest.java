package com.twitter.auth.pasetoheaders.encryption;

import java.util.Optional;

import org.junit.Test;

import com.twitter.auth.pasetoheaders.models.Passports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClaimServiceTest extends Stubs {

  @Test
  public void testClaimServiceExtractingGenericType() {
    Optional<String> signedToken = signingService.signToken(customerPassport, Optional.empty());
    Passports.Passport cp = claimService.extractClaims(
        signedToken.get()).get().getEnclosedEntity();
    assertEquals(Passports.CustomerPassport.class, cp.getClass());
    assertEquals(testPassportId, cp.getPassportId());
  }

  @Test
  public void testClaimServiceUnverifiedExtractingGenericType() {
    Optional<String> signedToken = signingService.signToken(customerPassport, Optional.empty());
    Passports.Passport cp = claimService.extractUnverifiedClaims(
        signedToken.get()).get().getEnclosedEntity();
    assertEquals(Passports.CustomerPassport.class, cp.getClass());
    assertEquals(testPassportId, cp.getPassportId());
  }

  @Test
  public void testClaimServiceExtractingOtherClaimsFromGenericType() {
    Optional<String> signedToken = signingService.signToken(customerPassport, Optional.empty());
    ExtractedClaims<Passports.Passport>
        claims = claimService.extractClaims(
        signedToken.get()).get();
    assertEquals("devel:test:1", claims.getKeyId());
    assertEquals("test", claims.getIssuer());
    assertEquals(Integer.valueOf(1), claims.getModelVersion());
  }

  @Test
  public void testClaimServiceUnverifiedExtractingOtherClaimsFromGenericType() {
    Optional<String> signedToken = signingService.signToken(customerPassport, Optional.empty());
    ExtractedClaims<Passports.Passport>
        claims = claimService.extractUnverifiedClaims(
        signedToken.get()).get();
    assertEquals("devel:test:1", claims.getKeyId());
    assertEquals("test", claims.getIssuer());
    assertEquals(Integer.valueOf(1), claims.getModelVersion());
  }

  @Test
  public void testClaimServiceExtractingWithExpiredTokenGenericType() {
    Optional<ExtractedClaims<Passports.Passport>> claim =
        claimService.extractClaims(testExpiredToken);
    assertEquals(false, claim.isPresent());
  }

  @Test
  public void testClaimServiceUnverifiedExtractingWithExpiredTokenGenericType() {
    Optional<ExtractedClaims<Passports.Passport>> claim =
        claimService.extractUnverifiedClaims(testExpiredToken);
    assertEquals(false, claim.isPresent());
  }

  @Test
  public void testClaimServiceExtractingSpecificInformationFromGenericType() {
    Optional<String> signedToken = signingService.signToken(customerPassport, Optional.empty());
    Passports.Passport cp = claimService.extractClaims(
        signedToken.get()).get().getEnclosedEntity();
    if (cp instanceof Passports.CustomerPassport) {
      assertEquals(testUserId, ((Passports.CustomerPassport) cp).getUserId().get());
    } else {
      fail();
    }
  }

  @Test
  public void testClaimServiceExtractingEmployeePassportFromGenericType() {
    Optional<String> signedToken = signingService.signToken(employeePassport, Optional.empty());
    Passports.Passport passport = claimService.extractClaims(
        signedToken.get()).get().getEnclosedEntity();
    if (passport instanceof Passports.EmployeePassport) {
      Passports.EmployeePassport receivedEmployeePassport = (Passports.EmployeePassport) passport;
      //TODO: find better way to compare, risk of bad implementation of toString and unordered sets
      assertEquals(employeePassport.toString(), receivedEmployeePassport.toString());
    } else {
      fail();
    }
  }

  @Test
  public void testClaimServiceWhenPublicKeyIsMissing() {
    Optional<String> signedToken = signingService.signToken(customerPassport, Optional.empty());
    keyProvider.clear();
    assertEquals(true, signedToken.isPresent());
    assertEquals(claimService.extractClaims(signedToken.get()), Optional.empty());
  }

  @Test
  public void testClaimServiceWithUnverifiedExtractionWhenPublicKeyIsMissing() {
    Optional<String> signedToken = signingService.signToken(customerPassport, Optional.empty());
    keyProvider.clear();
    assertEquals(true, signedToken.isPresent());
    assertEquals(claimService.extractUnverifiedClaims(signedToken.get()).isPresent(), true);
  }

}
