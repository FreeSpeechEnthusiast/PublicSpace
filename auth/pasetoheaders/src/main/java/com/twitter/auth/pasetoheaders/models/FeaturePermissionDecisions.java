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
public class FeaturePermissionDecisions {

  public Optional<Set<String>> getAllowedFeaturePermissions() {
    return allowedFeaturePermissions;
  }

  public Optional<Set<String>> getRejectedFeaturePermissions() {
    return rejectedFeaturePermissions;
  }

  @JsonProperty("anm")
  private Optional<Set<String>> allowedFeaturePermissions;
  @JsonProperty("rnm")
  private Optional<Set<String>> rejectedFeaturePermissions;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public FeaturePermissionDecisions() {
    this.allowedFeaturePermissions = Optional.empty();
    this.rejectedFeaturePermissions = Optional.empty();
  }

  public FeaturePermissionDecisions(Optional<Set<String>> allowedFeaturePermissions,
                                    Optional<Set<String>> rejectedFeaturePermissions) {
    this.allowedFeaturePermissions = allowedFeaturePermissions;
    this.rejectedFeaturePermissions = rejectedFeaturePermissions;
  }

  @Override
  public String toString() {
    return "FeaturePermissionDecisions ==>"
        + " [allowed: " + (allowedFeaturePermissions.isPresent()
          ? allowedFeaturePermissions.get().toString() : "None")
        + ", rejected: " + (rejectedFeaturePermissions.isPresent()
          ? rejectedFeaturePermissions.get().toString() : "None")
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
    FeaturePermissionDecisions that = (FeaturePermissionDecisions) o;
    return allowedFeaturePermissions.equals(
        that.allowedFeaturePermissions) && rejectedFeaturePermissions.equals(
        that.rejectedFeaturePermissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowedFeaturePermissions, rejectedFeaturePermissions);
  }
}
