package com.twitter.auth.authorization

/**
 * NOTE: This implementation of FeaturePermissionLookup should only be used for testing purposes
 */
object FixedFeaturePermissionLookup extends FeaturePermissionLookup {

  private[this] val mapping = Map[Long, Set[String]](
    1L -> Set("can_access_control_tower"),
    2L -> Set("can_access_control_tower", "test_feature_permission_1", "test_feature_permission_2"),
    129032L -> Set("disallow_oauth2_authorization_code"),
    258901L -> Set("disallow_oauth2_authorization_code"),
    1082764L -> Set("disallow_oauth2_authorization_code"),
    191841L -> Set("disallow_oauth2_authorization_code"),
    18755074L -> Set("allow_ads_scopes")
  )

  override def areAllValidFPs(fpNames: Set[String]): Boolean = true

  override def featurePermissions(clientApplicationId: Long): Set[String] =
    mapping.getOrElse(clientApplicationId, Set())
}
