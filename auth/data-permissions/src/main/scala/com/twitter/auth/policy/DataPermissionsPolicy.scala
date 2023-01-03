package com.twitter.auth.policy

trait DataPermissionsPolicy {

  /**
   * Retrieve policy mappings from Authorization Scope to Data Permission Ids.
   *
   * @param scope the AuthorizationScopes to retrieve corresponding Data Permissions
   *
   * @return set of DataPermissions as String
   */
  def dataPermissionIds(scope: String): Set[Long]

  /**
   * Check that a Data Permission Ids is valid.
   *
   * @param dpId of Data Permission Id to validate
   *
   * @return whether the dpId is valid
   */
  def isValidDPId(dpId: Long): Boolean
}
