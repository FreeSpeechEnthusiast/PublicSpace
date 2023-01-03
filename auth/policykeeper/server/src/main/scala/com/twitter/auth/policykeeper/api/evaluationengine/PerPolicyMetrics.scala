package com.twitter.auth.policykeeper.api.evaluationengine

import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.servo.util.MemoizingStatsReceiver

trait PerPolicyMetrics {

  protected val perPolicyStatsReceiver: MemoizingStatsReceiver

  protected[api] val Scope: String = this.getClass.getSimpleName
  // bootstrap scope after setting the perPolicyStatsReceiver
  protected lazy val perPolicyStatsScope = perPolicyStatsReceiver.scope(Scope)

  protected[api] def policyScopeName(policy: Policy) =
    "policy_" + policy.policyId

  protected def policyStatsScope(policy: Policy) =
    perPolicyStatsScope.scope(policyScopeName(policy))

}
