package com.twitter.auth.authorization

trait FeaturePermissionLookup {

  /**
   * Retrieve policy mappings from client application id to Feature Permissions.
   *
   * @param clientApplicationId id of com.twitter.passbird.clientapplication.thriftscala.ClientApplication
   *
   * @return set of FeaturePermissions as String
   */
  def featurePermissions(clientApplicationId: Long): Set[String]

  /**
   * Check the validity of a set of FPs
   *
   * @param fpNames the set of FP we want to check validity for
   *
   * @return true if all FPs in the set are valid
   */
  def areAllValidFPs(fpNames: Set[String]): Boolean
}
