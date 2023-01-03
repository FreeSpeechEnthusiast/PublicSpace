package com.twitter.auth.policykeeper.api.storage

import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.util.Future

case class InMemoryPolicyStorage()
    extends ReadOnlyPolicyStorageInterface
    with ReplaceableData[Policy] {
  private[storage] var storage: Map[PolicyId, Policy] = Map()

  def clear(): Unit = {
    storage = Map()
  }

  def getPoliciesByIds(
    policyIds: Seq[PolicyId]
  ): Future[Seq[Policy]] = {
    Future.value(storage.filterKeys(policyIds.contains(_)).values.toSeq)
  }

  /**
   * Replaces entire data with new version of data
   *
   * @param policies
   */
  override def replaceDataWithNewVersion(data: Set[Policy]): Unit = {
    storage = data.zipWithIndex.map {
      case (v, i) => (PolicyId(v.policyId), v)
    }.toMap
  }
}
