package com.twitter.auth.policykeeper.api.storage

import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.util.Future
import com.twitter.auth.policykeeper.api.storage.common.PolicyId

trait ReadOnlyPolicyStorageInterface {

  /**
   * Returns policies by with specified policy identifiers
   *
   * @param policyIds
   * @return
   */
  def getPoliciesByIds(
    policyIds: Seq[PolicyId]
  ): Future[Seq[Policy]]

  /**
   * Returns policy by identifier
   *
   * @param policyId
   * @return
   */
  def getPolicyById(
    policyId: PolicyId
  ): Future[Option[Policy]] = {
    getPoliciesByIds(Seq(policyId)).map {
      case Seq() => None
      case Seq(p) => Some(p)
    }
  }

  //TODO: add getPolicyByName, getPoliciesByNames
}
