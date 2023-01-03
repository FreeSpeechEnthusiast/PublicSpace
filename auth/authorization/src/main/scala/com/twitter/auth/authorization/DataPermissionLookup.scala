package com.twitter.auth.authorization

trait DataPermissionLookup {

  /**
   * Retrieve policy mappings from Authorization Scope to Data Permission Ids.
   *
   * @param scopes set of AuthorizationScopes to retrieve corresponding Data Permissions Ids
   *
   * @return set of DataPermissionsm as String
   */
  def dataPermissionIds(scopes: Set[String]): Set[Long]

  /**
   * Filter the list of given DPs to only the valid ones.
   *
   * @param dpIds set of Data Permission Ids to validate
   *
   * @return a list of the ones that are valid
   */
  def filterValidDPIds(dpIds: Set[Long]): Set[Long]

  /**
   * Check validation of a set of Data Permission Ids.
   *
   * @param dpIds set of Data Permission Ids to validate
   *
   * @return whether all dpIds are valid
   */
  def areAllValidDPIds(dpIds: Set[Long]): Boolean
}
