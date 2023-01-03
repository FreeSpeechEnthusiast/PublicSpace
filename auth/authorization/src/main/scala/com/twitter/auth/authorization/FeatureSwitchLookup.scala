package com.twitter.auth.authorization

import com.twitter.auth.policy.FeatureSwitchPolicy

object FeatureSwitchLookup {
  private val defaultConfigRepoPath = "/usr/local/config"
  def apply(configRepoPath: String = defaultConfigRepoPath): FeatureSwitchLookup = {
    FeatureSwitchPolicy(configRepoPath)
    new FeatureSwitchLookup()
  }
}

class FeatureSwitchLookup extends FeaturePermissionLookup {
  override def featurePermissions(clientApplicationId: Long): Set[String] = {
    FeatureSwitchPolicy.featurePermissions(clientApplicationId)
  }

  override def areAllValidFPs(fpNames: Set[String]): Boolean = {
    val validFPs = filterValidFPs(fpNames)
    validFPs.size == fpNames.size
  }

  def isValidFP(fpName: String): Boolean = {
    FeatureSwitchPolicy.isValidFP(fpName)
  }

  def filterValidFPs(fpNames: Set[String]): Set[String] = {
    fpNames.filter(FeatureSwitchPolicy.isValidFP)
  }
}
