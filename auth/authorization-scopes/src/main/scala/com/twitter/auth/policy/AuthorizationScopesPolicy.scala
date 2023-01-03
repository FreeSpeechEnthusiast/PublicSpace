package com.twitter.auth.policy

import com.twitter.auth.authorizationscope.AuthorizationScope

trait AuthorizationScopesPolicy {

  /***
   * Retrieve policy mappings based on Token Privileges
   * @param tokenPrivileges Set of Token Privileges (as Strings)
   */
  def authorizationScopes(tokenPrivileges: Set[String]): Set[String]

  /**
   * Retrieve authorization scope based on Token Privilege
   * @param tokenPrivilege Token Privilege (as String)
   */
  def authorizationScope(tokenPrivilege: String): Option[AuthorizationScope]
}
