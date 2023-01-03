package com.twitter.auth.pasetoheaders.models;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import org.junit.Test;
import org.hamcrest.core.StringContains;
import org.hamcrest.MatcherAssert;
import static org.junit.Assert.*;

public class PassportSerializerTest {

  private String testPassportId = "1";

  private Long testUserId = 1L;

  private Long testAuthUserId = 0L;
  private Long testGuestToken = 0L;
  private Long testClientApplicationId = 0L;
  private String testSessionHash = "";
  private String testLdap = "employee";

  private String testCustomerPassport =
      "{\"ptp\":\"cus\","
          + "\"pid\":\"" + testPassportId + "\","
          + "\"uid\":" + testUserId + ","
          + "\"aid\":" + testAuthUserId + ","
          + "\"gid\":" + testGuestToken + ","
          + "\"cid\":" + testClientApplicationId + ","
          + "\"sid\":\"" + testSessionHash + "\""
          + "}";
  private String testStringifiedCustomerPassport =
      "CustomerPassport ==> "
          + "[pid= 1, "
          + "userId=1, "
          + "authenticatedUserId=0, "
          + "guestId=0, "
          + "clientApplicationId=0, "
          + "sessionHash=, "
          + "DPD=None, "
          + "FPS=None, "
          + "SPS=None]";

  private ObjectMapper jsonMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    return mapper;
  }

  @Test
  public void testCustomerPassportToStringMethod() {
    Passports.CustomerPassport cp = new Passports.CustomerPassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.of(testClientApplicationId),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    assertEquals(testStringifiedCustomerPassport, cp.toString());
  }

  @Test
  public void customerPassportToJsonTest() throws JsonProcessingException {
    Passports.CustomerPassport cp = new Passports.CustomerPassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.of(testClientApplicationId),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    String result = jsonMapper().writeValueAsString(cp);
    MatcherAssert.assertThat(result, StringContains.containsString("cus"));
    assertEquals(testCustomerPassport, result);
  }

  @Test
  public void customerPassportFromJsonTest() throws JsonProcessingException {
    Passports.CustomerPassport cp = jsonMapper()
        .readerFor(Passports.CustomerPassport.class)
        .readValue(testCustomerPassport);
    assertEquals(Passports.CustomerPassport.class, cp.getClass());
    assertEquals(testPassportId, cp.getPassportId());
    assertEquals(testUserId, cp.getUserId().get());
  }

  @Test
  public void getGenericPassportFromJsonTest() throws JsonProcessingException {
    Passports.Passport cp = jsonMapper()
        .readerFor(Passports.Passport.class)
        .readValue(testCustomerPassport);
    assertEquals(Passports.CustomerPassport.class, cp.getClass());
    assertEquals(testPassportId, cp.getPassportId());
    /**
     * using MATCH we can identify passport type and get specific principal information
     */
  }

  @Test
  public void getGenericPassportEncodeDecodeTest() throws JsonProcessingException {
    Passports.CustomerPassport cp = new Passports.CustomerPassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.empty(),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    Passports.Passport decodedCp = jsonMapper()
        .readerFor(Passports.Passport.class)
        .readValue(jsonMapper().writeValueAsString(cp));
    assertEquals(cp.toString(), decodedCp.toString());
  }

  @Test
  public void testEqualityOfCustomerPassports() {
    Passports.CustomerPassport cp = new Passports.CustomerPassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.empty(),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    Passports.CustomerPassport cp2 = new Passports.CustomerPassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.empty(),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    assertEquals(cp, cp2);
  }

  @Test
  public void testInEqualityOfCustomerPassportsWithAnotherId() {
    Passports.CustomerPassport cp = new Passports.CustomerPassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.empty(),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    Passports.CustomerPassport cp2 = new Passports.CustomerPassport(
        testPassportId + 1,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.empty(),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    assertNotEquals(cp, cp2);
  }

  @Test
  public void testInEqualityOfCustomerPassports() {
    Passports.CustomerPassport cp = new Passports.CustomerPassport(
        testPassportId,
        Optional.of(testUserId + 1),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.empty(),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    Passports.CustomerPassport cp2 = new Passports.CustomerPassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.empty(),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    assertNotEquals(cp, cp2);
  }

  @Test
  public void testInEqualityWithEmptyInCustomerPassports() {
    Passports.CustomerPassport cp = new Passports.CustomerPassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.empty(),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    Passports.CustomerPassport cp2 = new Passports.CustomerPassport(
        testPassportId,
        Optional.empty(),
        Optional.of(testAuthUserId),
        Optional.of(testGuestToken),
        Optional.empty(),
        Optional.of(testSessionHash),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    assertNotEquals(cp, cp2);
  }

  @Test
  public void testInEqualityOfEmployeePassports() {
    Passports.EmployeePassport ep = new Passports.EmployeePassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.empty(),
        Optional.of(testSessionHash),
        testLdap,
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    Passports.EmployeePassport ep2 = new Passports.EmployeePassport(
        testPassportId,
        Optional.empty(),
        Optional.of(testAuthUserId),
        Optional.empty(),
        Optional.of(testSessionHash),
        testLdap,
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    assertNotEquals(ep, ep2);
  }

  @Test
  public void testEqualityOfEmployeePassports() {
    Passports.EmployeePassport ep = new Passports.EmployeePassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.empty(),
        Optional.of(testSessionHash),
        testLdap,
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    Passports.EmployeePassport ep2 = new Passports.EmployeePassport(
        testPassportId,
        Optional.of(testUserId),
        Optional.of(testAuthUserId),
        Optional.empty(),
        Optional.of(testSessionHash),
        testLdap,
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    assertEquals(ep, ep2);
  }
}
