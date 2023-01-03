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
public class DataPermissionDecisions {

  public Optional<Set<Long>> getAllowedDataPermissionIds() {
    return allowedDataPermissionIds;
  }

  public Optional<Set<Long>> getRejectedDataPermissionIds() {
    return rejectedDataPermissionIds;
  }

  @JsonProperty("aid")
  private Optional<Set<Long>> allowedDataPermissionIds;
  @JsonProperty("rid")
  private Optional<Set<Long>> rejectedDataPermissionIds;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public DataPermissionDecisions() {
    this.allowedDataPermissionIds = Optional.empty();
    this.rejectedDataPermissionIds = Optional.empty();
  }

  public DataPermissionDecisions(Optional<Set<Long>> allowedDataPermissionIds,
                                 Optional<Set<Long>> rejectedDataPermissionIds) {
    this.allowedDataPermissionIds = allowedDataPermissionIds;
    this.rejectedDataPermissionIds = rejectedDataPermissionIds;
  }

  @Override
  public String toString() {
    return "DataPermissionDecisions ==>"
        + " [id allowed: " + (allowedDataPermissionIds.isPresent()
          ? allowedDataPermissionIds.get().toString() : "None")
        + ", id rejected: " + (rejectedDataPermissionIds.isPresent()
          ? rejectedDataPermissionIds.get().toString() : "None")
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
    DataPermissionDecisions that = (DataPermissionDecisions) o;
    return allowedDataPermissionIds.equals(
        that.allowedDataPermissionIds) && rejectedDataPermissionIds.equals(
        that.rejectedDataPermissionIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowedDataPermissionIds, rejectedDataPermissionIds);
  }
}
