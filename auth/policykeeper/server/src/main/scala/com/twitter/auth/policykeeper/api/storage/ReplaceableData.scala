package com.twitter.auth.policykeeper.api.storage

trait ReplaceableData[T] {

  /**
   * Replaces entire data with new version of data
   *
   * @param data
   */
  def replaceDataWithNewVersion(data: Set[T]): Unit
}
