package com.twitter.auth.pasetoheaders.models;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class SubscriptionPermissionDecisions {

  public Optional<Set<String>> getAllowedSubscriptionPermissions() {
    return allowedSubscriptionPermissions;
  }

  public Optional<Set<String>> getRejectedSubscriptionPermissions() {
    return rejectedSubscriptionPermissions;
  }

  @JsonProperty("anm")
  private Optional<Set<String>> allowedSubscriptionPermissions;
  @JsonProperty("rnm")
  private Optional<Set<String>> rejectedSubscriptionPermissions;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public SubscriptionPermissionDecisions() {
    this.allowedSubscriptionPermissions = Optional.empty();
    this.rejectedSubscriptionPermissions = Optional.empty();
  }

  public SubscriptionPermissionDecisions(Optional<Set<String>> allowedSubscriptionPermissions,
                                         Optional<Set<String>> rejectedSubscriptionPermissions) {
    this.allowedSubscriptionPermissions = allowedSubscriptionPermissions;
    this.rejectedSubscriptionPermissions = rejectedSubscriptionPermissions;
  }

  @Override
  public String toString() {
    return "SubscriptionPermissionDecisions ==>"
        + " [allowed: " + (allowedSubscriptionPermissions.isPresent()
          ? allowedSubscriptionPermissions.get().toString() : "None")
        + ", rejected: " + (rejectedSubscriptionPermissions.isPresent()
          ? rejectedSubscriptionPermissions.get().toString() : "None")
        + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionPermissionDecisions that = (SubscriptionPermissionDecisions) o;
    return allowedSubscriptionPermissions.equals(
        that.allowedSubscriptionPermissions) && rejectedSubscriptionPermissions.equals(
        that.rejectedSubscriptionPermissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowedSubscriptionPermissions, rejectedSubscriptionPermissions);
  }
}
