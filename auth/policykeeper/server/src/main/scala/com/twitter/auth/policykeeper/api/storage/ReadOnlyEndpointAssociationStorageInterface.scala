package com.twitter.auth.policykeeper.api.storage

import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.util.Future

trait ReadOnlyEndpointAssociationStorageInterface[M, T <: EndpointAssociationData[M, T]] {

  def policyStorage: ReadOnlyPolicyStorageInterface

  protected def getAssociatedPoliciesIds(
    associatedData: Seq[T]
  ): Future[Seq[PolicyId]]

  /**
   * Returns policies associated with endpoint identifiers
   *
   * @param associatedData
   * @return
   */
  def getAssociatedPolicies(
    associatedData: Seq[T]
  ): Future[Seq[Policy]] = {
    getAssociatedPoliciesIds(associatedData = associatedData).flatMap { ids =>
      policyStorage.getPoliciesByIds(ids)
    }
  }

}
