package com.twitter.auth.authorization

import com.twitter.auth.authorizationscope.AuthorizationScope
import com.twitter.auth.policy.DynamicAuthorizationScopesPolicy

trait AuthorizationScopeLookup {

  val dynamicAuthorizationScopesPolicy: DynamicAuthorizationScopesPolicy
  val TestScopeState = "test"

  /**
   * Verify and retrieve scope names by Token Privileges
   * @param tokenPrivileges Set of Token Privileges (as Strings)
   */
  def authorizationScopes(tokenPrivileges: Set[String]): Set[String]

  /**
   * Verify and retrieve scope names by Token Privileges
   * @param tokenPrivilege Token Privilege (as String)
   */
  def authorizationScope(
    tokenPrivilege: String,
    internalGroup: Option[String],
    applicableAuthTypes: Option[String] = None,
    includeTestStateScope: Boolean
  ): Option[AuthorizationScope] = {
    val scopeOpt = dynamicAuthorizationScopesPolicy.authorizationScope(tokenPrivilege)
    (internalGroup, applicableAuthTypes) match {
      case (Some(group), Some(authTypes)) if includeTestStateScope =>
        scopeOpt
          .filter(_.internalGroup == group.toLowerCase()).filter(
            _.applicableAuthTypes.contains(authTypes))
      case (Some(group), Some(authTypes)) =>
        scopeOpt
          .filter(_.internalGroup == group.toLowerCase()).filter(
            _.applicableAuthTypes.contains(authTypes)).filter(_.state != TestScopeState)
      case (Some(group), None) if includeTestStateScope =>
        scopeOpt.filter(_.internalGroup == group.toLowerCase())
      case (Some(group), None) =>
        scopeOpt.filter(_.internalGroup == group.toLowerCase()).filter(_.state != TestScopeState)
      case (None, Some(authTypes)) if includeTestStateScope =>
        scopeOpt.filter(_.applicableAuthTypes.contains(authTypes))
      case (None, Some(authTypes)) =>
        scopeOpt.filter(_.applicableAuthTypes.contains(authTypes)).filter(_.state != TestScopeState)
      case (None, None) if includeTestStateScope =>
        scopeOpt
      case (None, None) =>
        scopeOpt.filter(_.state != TestScopeState)
    }
  }
}
