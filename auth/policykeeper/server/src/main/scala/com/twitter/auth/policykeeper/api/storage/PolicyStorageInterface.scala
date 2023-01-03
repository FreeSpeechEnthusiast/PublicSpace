package com.twitter.auth.policykeeper.api.storage

import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.util.Future

final case class InvalidPolicyIdentifier() extends Exception

trait PolicyStorageInterface extends ReadOnlyPolicyStorageInterface {

  private val policyIdPattern = PolicyId.Pattern.r

  /**
   * Adds new policy
   *
   * @param policy
   * @return
   * @throws InvalidPolicyIdentifier
   */
  def storePolicy(
    policy: Policy
  ): Future[PolicyId] = {
    if (validatePolicyId(policy.policyId)) {
      storeValidatedPolicy(policy)
    } else throw InvalidPolicyIdentifier()
  }

  /**
   * Updates existing policy
   *
   * @param policyId
   * @param policy
   * @return
   * @throws InvalidPolicyIdentifier
   */
  def updatePolicy(
    policyId: PolicyId,
    policy: Policy
  ): Future[Boolean] = {
    if (validatePolicyId(policy.policyId)) {
      updateValidatedPolicy(policyId = policyId, policy = policy)
    } else throw InvalidPolicyIdentifier()
  }

  /**
   * Deletes existing policy
   *
   * @param policyId
   * @return
   */
  def deletePolicy(
    policyId: PolicyId
  ): Future[Boolean] = {
    deletePolicies(Seq(policyId))
  }

  /**
   * Deletes many policies
   *
   * @param policyIds
   * @return
   */
  def deletePolicies(
    policyIds: Seq[PolicyId]
  ): Future[Boolean]

  protected def storeValidatedPolicy(
    policy: Policy
  ): Future[PolicyId]

  protected def updateValidatedPolicy(
    policyId: PolicyId,
    policy: Policy
  ): Future[Boolean]

  private def validatePolicyId(policyId: String): Boolean = {
    policyId match {
      case policyIdPattern(_) => true
      case _ => false
    }
  }
}
