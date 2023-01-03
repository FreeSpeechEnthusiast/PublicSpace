package com.twitter.auth.policy

import com.twitter.featureswitches.Recipient
import com.twitter.featureswitches.v2.FeatureSwitches
import com.twitter.featureswitches.v2.builder.FeatureSwitchesBuilder

object FeatureSwitchPolicy {
  private val featurePermissionsPath = "/features/customer-auth"
  private var fs: FeatureSwitches = _

  def apply(configRepoPath: String) = {
    fs = FeatureSwitchesBuilder
      .createWithNoExperiments(featurePermissionsPath)
      .configRepoAbsPath(configRepoPath)
      .build()
  }

  /**
   * Retrieve policy mappings from client application id to Feature Permissions.
   *
   * @param clientApplicationId id of com.twitter.passbird.clientapplication.thriftscala.ClientApplication
   *
   * @return set of FeaturePermissions in String
   *
   * TODO: The current Recipient is based on client app id only.
   * This will need to change to support a broader list of fields once its available via the auth principal
   */

  def featurePermissions(clientApplicationId: Long): Set[String] = {

    /** Load all FPs that match this Recipient */
    val matchedResults = fs.matchRecipient(
      Recipient(clientApplicationId = Some(clientApplicationId))
    )

    /** Filter Feature Switch keys:
     * 1. That are only boolean types
     * 2. That only evaluate to true
     * 3. And finally, collect only the FPs that are defined in the FeaturePermissions.thrift enum
     */
    matchedResults.getMatchedFeatureKeys
      .filter(matchedResults.getBoolean(_).getOrElse(false))
      .map(fk => (fsKeyToFeaturePermission(fk)))
  }

  def isValidFP(fpName: String): Boolean = {
    // TODO: change this to extract valid FPs from config file: AUTHPLT-1036
    true
  }

  /**
   * Convert Feature Permission name from Feature Switch config file, and naming format is with lower case
   * e.g.
   * feature_permissions_Can_Access_Control_Tower => can_access_control_tower // valid
   * feature_permissions_can_access_control_tower => can_access_control_tower // valid
   * feature_permissions_CanAccessControlTower => canaccesscontroltower       // invalid
   */
  private[this] def fsKeyToFeaturePermission(fsKey: String): String = {
    fsKey.split("feature_permissions_")(1).toLowerCase()
  }
}
