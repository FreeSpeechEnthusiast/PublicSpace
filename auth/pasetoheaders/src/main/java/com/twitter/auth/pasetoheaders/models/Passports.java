package com.twitter.auth.pasetoheaders.models;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

/**
 * Based on:
 * "com/twitter/auth/PassportType.thrift"
 * "com/twitter/auth/paseto/TamperProofing.thrift"
 * "com/twitter/auth/authenforcement.Thrift"
 */
public final class Passports {
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.PROPERTY,
      property = "ptp")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = CustomerPassport.class, name = "cus"),
      @JsonSubTypes.Type(value = EmployeePassport.class, name = "emp")
  })
  // TODO - if need be, here we can specify the order of parameter serialization
  public
  static class Passport {
    /**
     * Now that we are not using thrift, keep in mind that nesting objects deeper will just make
     * serialization more "verbose"
     * However, we can mitigate that by using JsonProperty annotation and use a different name
     *
     * Using JSON also mitigates the difficulty in updating the data structure later (vs Thrift)
     * Therefore, come up with a good first version of the Passport, and expand as necessary
     *
     * Serialization, deserialization, and tamper-proof signature generation should work regardless
     * of having new elements present (i.e. unknown elements)
     * One key feature to enable the "regardless" is to run signature validation on raw JSON
     * Post-verification, do NOT attempt to deserialize unknown objects
     */

    public String getPassportId() {
      return passportId;
    }

    public Optional<DataPermissionDecisions> getDpd() {
      return dpd;
    }

    public Optional<FeaturePermissionDecisions> getFps() {
      return fps;
    }

    public Optional<SubscriptionPermissionDecisions> getSps() {
      return sps;
    }

    /**
     * Passport Id
     */
    @JsonProperty("pid")
    protected String passportId;

    /**
     * Data permission decisions
     */
    @JsonProperty("dpd")
    protected Optional<DataPermissionDecisions> dpd;

    /**
     * Feature permission decisions
     */
    @JsonProperty("fps")
    protected Optional<FeaturePermissionDecisions> fps;

    /**
     * Subscription permission decisions
     */
    @JsonProperty("sps")
    protected Optional<SubscriptionPermissionDecisions> sps;

    protected String serializedPrincipal() {
      return "";
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName()
          + " ==>"
          + " [pid= " + this.passportId
          + serializedPrincipal()
          + ", DPD=" + (dpd.isPresent() ? this.dpd.get() : "None")
          + ", FPS=" + (fps.isPresent() ? this.fps.get() : "None")
          + ", SPS=" + (sps.isPresent() ? this.sps.get() : "None")
          + "]";
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    private Passport() {
      /**
       * Attention! Optional values should be predefined
       * to replace null values during deserialization
       */
      this.dpd = Optional.empty();
      this.fps = Optional.empty();
      this.sps = Optional.empty();
    }

    private Passport(String passportId, Optional<DataPermissionDecisions> dpd,
                     Optional<FeaturePermissionDecisions> fps,
                     Optional<SubscriptionPermissionDecisions> sps) {
      this.passportId = passportId;
      this.dpd = dpd;
      this.fps = fps;
      this.sps = sps;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Passport passport = (Passport) o;
      return passportId.equals(passport.passportId)
         && dpd.equals(passport.dpd)
         && fps.equals(passport.fps)
         && sps.equals(passport.sps);
    }

    @Override
    public int hashCode() {
      return Objects.hash(passportId, dpd, fps, sps);
    }
  }

  @JsonTypeName("cus")
  public
  static class CustomerPassport extends Passport {
    public Optional<Long> getUserId() {
      return userId;
    }

    public Optional<Long> getAuthenticatedUserId() {
      return authenticatedUserId;
    }

    public Optional<Long> getGuestToken() {
      return guestToken;
    }

    public Optional<Long> getClientApplicationId() {
      return clientApplicationId;
    }

    public Optional<String> getSessionHash() {
      return sessionHash;
    }

    @JsonProperty("uid")
    protected Optional<Long> userId;

    @JsonProperty("aid")
    protected Optional<Long> authenticatedUserId;

    @JsonProperty("gid")
    protected Optional<Long> guestToken;

    @JsonProperty("cid")
    protected Optional<Long> clientApplicationId;

    @JsonProperty("sid")
    protected Optional<String> sessionHash;

    @Override
    protected String serializedPrincipal() {
      return super.serializedPrincipal() + ", userId=" + (this.userId.isPresent()
          ? this.userId.get() : "None")
          + ", authenticatedUserId=" + (this.authenticatedUserId.isPresent()
          ? this.authenticatedUserId.get() : "None")
          + ", guestId=" + (this.guestToken.isPresent()
          ? this.guestToken.get() : "None")
          + ", clientApplicationId=" + (this.clientApplicationId.isPresent()
          ? this.clientApplicationId.get() : "None")
          + ", sessionHash=" + (this.sessionHash.orElse("None"));
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    private CustomerPassport() {
      /**
       * Attention! Optional values should be predefined
       * to replace null values during deserialization
       */
      this.userId = Optional.empty();
      this.authenticatedUserId = Optional.empty();
      this.guestToken = Optional.empty();
      this.clientApplicationId = Optional.empty();
      this.sessionHash = Optional.empty();
    }

    public CustomerPassport(String passportId,
                            Optional<Long> userId,
                            Optional<Long> authenticatedUserId,
                            Optional<Long> guestToken,
                            Optional<Long> clientApplicationId,
                            Optional<String> sessionHash,
                            Optional<DataPermissionDecisions> dpd,
                            Optional<FeaturePermissionDecisions> fps,
                            Optional<SubscriptionPermissionDecisions> sps) {
      super(passportId, dpd, fps, sps);
      this.userId = userId;
      this.authenticatedUserId = authenticatedUserId;
      this.guestToken = guestToken;
      this.clientApplicationId = clientApplicationId;
      this.sessionHash = sessionHash;
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
      CustomerPassport that = (CustomerPassport) o;
      return userId.equals(that.userId)
          && authenticatedUserId.equals(that.authenticatedUserId)
          && guestToken.equals(that.guestToken)
          && clientApplicationId.equals(that.clientApplicationId)
          && sessionHash.equals(that.sessionHash);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(),
          userId,
          authenticatedUserId,
          guestToken,
          clientApplicationId,
          sessionHash);
    }
  }

  @JsonTypeName("emp")
  public
  static final class EmployeePassport extends CustomerPassport {
    public String getEmployeeLdap() {
      return employeeLdap;
    }

    @JsonProperty("eid")
    protected String employeeLdap;

    @Override
    protected String serializedPrincipal() {
      return super.serializedPrincipal() + ", employeeId=" + this.employeeLdap;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    private EmployeePassport() {
    }

    public EmployeePassport(String passportId,
                            Optional<Long> userId,
                            Optional<Long> authenticatedUserId,
                            Optional<Long> clientApplicationId,
                            Optional<String> sessionHash,
                            String employeeLdap,
                            Optional<DataPermissionDecisions> dpd,
                            Optional<FeaturePermissionDecisions> fps,
                            Optional<SubscriptionPermissionDecisions> sps) {
      super(passportId,
          userId,
          authenticatedUserId,
          // a guest token field is always empty for employees
          Optional.empty(),
          clientApplicationId,
          sessionHash,
          dpd,
          fps,
          sps);
      this.employeeLdap = employeeLdap;
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
      EmployeePassport that = (EmployeePassport) o;
      return employeeLdap.equals(that.employeeLdap);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), employeeLdap);
    }
  }

}
