package com.twitter.auth.pasetoheaders.encryption;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import com.twitter.auth.pasetoheaders.models.DataPermissionDecisions;
import com.twitter.auth.pasetoheaders.models.FeaturePermissionDecisions;
import com.twitter.auth.pasetoheaders.models.Passports;
import com.twitter.auth.pasetoheaders.models.SubscriptionPermissionDecisions;

public class Stubs {
  protected String testPrivateKeyString =
      "976e9e84221efcd8cbb4083948371fdcb833df0ad5ffd816fb07d25639c28bfe";
  protected String testPublicKeyString =
      "31c273c193af94a96d3a8db254cbc5aac81e881e098746d549caf2dea819ed81";
  protected String testOtherPrivateKeyString =
      "6054C48F759C6C7D73EF839FBF4C5D4CA39986AD18C8C3A49B3796C7E660A3F6";
  protected String testEnv = "devel";
  protected String testOtherEnv = "devel2";
  protected String testClaim = "passport";
  protected String testIssuer = "test";
  protected String testOtherIssuer = "test2";
  protected InMemoryKeyProvider keyProvider = new InMemoryKeyProvider();

  protected String testPassportId = "1";
  protected Long testUserId = 1L;
  private Long testAuthUserId = 0L;
  private Long testGuestToken = 0L;
  private Long testClientApplicationId = 0L;
  private String testSessionHash = "";

  protected Integer testLastKeyVersion = 1;
  protected Integer testOtherKeyVersion = 2;

  protected String testExpiredToken = "v2.public.eyJzdWIiOiJ0YmQiLCJwYXNz"
      + "cG9ydCI6eyJwdHAiOiJjdXMiLCJwaWQiOiIxIiwidWlkIjoxLCJhaWQiOjAsImdpZC"
      + "I6MCwiY2lkIjowLCJzaWQiOiIifSwiaXNzIjoidGVzdCIsImV4cCI6IjIwMjEtMDct"
      + "MjdUMjA6NTk6NTEuNjM5NCswMDowMCIsImlhdCI6IjIwMjEtMDctMjdUMjA6NTk6NT"
      + "AuNjM5NCswMDowMCJ952bUjpqnkhSS7dgyyccdpq6nI_B8mgBwOo7MtubauIhdbb8D"
      + "YnkiceweudOvE9cQbFVhspI7dYdpfAuh81kSDA.eyJtb2RlbFZlcnNpb24iOjEsImt"
      + "pZCI6ImNpOnRlc3Q6MSJ9";

  protected Passports.CustomerPassport customerPassport = new Passports.CustomerPassport(
      testPassportId,
      Optional.of(testUserId),
      Optional.of(testAuthUserId),
      Optional.of(testGuestToken),
      Optional.of(testClientApplicationId),
      Optional.of(testSessionHash),
      Optional.empty(),
      Optional.empty(),
      Optional.empty());

  protected Passports.EmployeePassport employeePassport = new Passports.EmployeePassport(
      testPassportId,
      Optional.of(testUserId),
      Optional.of(testAuthUserId),
      Optional.of(testClientApplicationId),
      Optional.of(testSessionHash),
      "employeeLdap",
      Optional.of(new DataPermissionDecisions(
          Optional.of(new HashSet<>(Arrays.asList(1L, 2L, 3L))),
          Optional.of(new HashSet<>())
      )),
      Optional.of(new FeaturePermissionDecisions()),
      Optional.of(new SubscriptionPermissionDecisions())
  );

  protected SigningService<Passports.Passport> signingService = new SigningService<>(
      testClaim,
      Passports.Passport.class,
      testEnv,
      testIssuer,
      keyProvider,
      Optional.empty(),
      Optional.empty());

  protected ClaimService<Passports.Passport> claimService = new ClaimService<>(
      testClaim,
      Passports.Passport.class,
      keyProvider,
      Optional.empty(),
      Optional.empty()
  );

  public Stubs() {
    keyProvider.addPrivateKey(
        new KeyIdentifier(testEnv, testIssuer, testLastKeyVersion),
        KeyUtils.getPrivateKey(testPrivateKeyString));
    keyProvider.addPublicKey(
        new KeyIdentifier(testEnv, testIssuer, testLastKeyVersion),
        KeyUtils.getPublicKey(testPublicKeyString));
  }
}
