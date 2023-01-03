package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.api.evaluationengine.PerPolicyMetrics
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.thriftscala.Code
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.Result
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.servo.util.MemoizingStatsReceiver

trait FailOpenPolicies extends PerPolicyMetrics {

  protected val statsReceiver: StatsReceiver
  protected val logger: JsonLogger
  protected val advancedStatsReceiver: MemoizingStatsReceiver

  /**
   * Connect advancedStatsReceiver with PerPolicyMetrics
   */
  protected val perPolicyStatsReceiver: MemoizingStatsReceiver = advancedStatsReceiver

  private val loggerScope = logger.withScope(Scope)
  private val statsScope = statsReceiver.scope(Scope)

  private[internal] val FailOpenPolicyEvaluated = "failOpenPolicyEvaluated"
  private[internal] val FailClosedPolicyEvaluated = "failClosedPolicyEvaluated"
  private[internal] val failOpenPolicyEvaluatedCounter = statsScope.counter(FailOpenPolicyEvaluated)
  private[internal] val failClosedPolicyEvaluatedCounter =
    statsScope.counter(FailClosedPolicyEvaluated)

  def handleFailOpenPolicyResult(
    policy: Policy,
    result: Result
  ): Result = {
    policy.failClosed match {
      case Some(failClosed) if failClosed =>
        failClosedPolicyEvaluatedCounter.incr()
        policyStatsScope(policy).counter(FailClosedPolicyEvaluated).incr()
        result
      case _ =>
        // stats
        failOpenPolicyEvaluatedCounter.incr()
        policyStatsScope(policy).counter(FailOpenPolicyEvaluated).incr()
        // log original result
        loggerScope.info(
          message = "Fail-open policy evaluated",
          metadata = Some(
            Map(
              "policyId" -> policy.policyId,
              "policyExecutionCode" -> result.policyExecutionCode.toString,
              "apiErrorCode" -> result.apiErrorCode.getOrElse("n/a"),
              "bouncerRequest" -> result.bouncerRequest.getOrElse("n/a")
            ))
        )
        // overwrite execution result in fail open mode
        Result(policyExecutionCode = Code.False, apiErrorCode = None, bouncerRequest = None)
    }
  }
}
