package com.twitter.auth.authorizationscope

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Authorization Scopes defined as Strings (PDP Customer Auth)
 * Represent a set of Data Permissions (e.g. Write Profile, Read DM).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
case class AuthorizationScope(
  id: Int,
  name: String,
  internalGroup: String,
  appCanViewDescription: Option[String],
  appCanDoDescription: Option[String],
  state: String,
  ldapOwners: Set[String] = Set(),
  userRoles: Set[String] = Set(),
  accessControl: Set[String] = Set(),
  applicableAuthTypes: Set[String] = Set(),
  clientPrivilegesAccessControl: Set[String] = Set(),
  rank: Int = 0) {

  def copy(
    id: Int = this.id,
    name: String = this.name,
    internalGroup: String = this.internalGroup,
    appCanViewDescription: Option[String] = this.appCanViewDescription,
    appCanDoDescription: Option[String] = this.appCanDoDescription,
    state: String = this.state,
    ldapOwners: Set[String] = this.ldapOwners,
    userRoles: Set[String] = this.userRoles,
    accessControl: Set[String] = this.accessControl,
    applicableAuthTypes: Set[String] = this.applicableAuthTypes,
    clientPrivilegesAccessControl: Set[String] = this.clientPrivilegesAccessControl,
    rank: Int = this.rank
  ) = AuthorizationScope(
    id = id,
    name = name,
    internalGroup = internalGroup,
    appCanViewDescription = appCanViewDescription,
    appCanDoDescription = appCanDoDescription,
    state = state,
    ldapOwners = ldapOwners,
    userRoles = userRoles,
    accessControl = accessControl,
    applicableAuthTypes = applicableAuthTypes,
    clientPrivilegesAccessControl = clientPrivilegesAccessControl,
    rank = rank
  )
}
