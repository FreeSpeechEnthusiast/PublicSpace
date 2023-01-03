package com.twitter.auth.policykeeper.api.storage

import com.twitter.util.Future
import com.twitter.auth.policykeeper.api.storage.common.PolicyId

trait EndpointAssociationStorageInterface[M, T <: EndpointAssociationData[M, T]]
    extends ReadOnlyEndpointAssociationStorageInterface[M, T] {

  /**
   * Attaches specific policy identifiers to associated endpoint identifiers
   *
   * @param associatedData
   * @param policyIds
   * @return
   */
  def attachAssociatedPolicies(
    associatedData: Set[T],
    policyIds: Set[PolicyId]
  ): Future[Boolean]

  /**
   * If [[policyIds]] is present then detaches specific policies from associated endpoint identifiers
   * If [[policyIds]] is omitted then detaches all policies from associated endpoint identifiers
   *
   * @param associatedData
   * @return
   */
  def detachAssociatedPolicies(
    associatedData: Set[T],
    policyIds: Option[Set[PolicyId]]
  ): Future[Boolean]

  /**
   * Syncs policy identifiers with associated endpoint identifiers
   *
   * @param associatedData
   * @param policyIds
   * @return
   */
  def syncAssociatedPolicies(
    associatedData: Set[T],
    policyIds: Set[PolicyId]
  ): Future[Boolean]
}
