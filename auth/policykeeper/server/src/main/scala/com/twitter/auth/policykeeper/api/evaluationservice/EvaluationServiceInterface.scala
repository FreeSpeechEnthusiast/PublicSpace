package com.twitter.auth.policykeeper.api.evaluationservice

import com.twitter.auth.policykeeper.api.dataproviders.DataProviderTimeoutException
import com.twitter.auth.policykeeper.api.dataproviderservice.DataProviderService
import com.twitter.auth.policykeeper.api.dataproviderservice.DataProviderServiceTimeoutException
import com.twitter.auth.policykeeper.api.evaluationengine.EvaluationEngineInterface
import com.twitter.auth.policykeeper.api.evaluationengine.Expression
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.PerPolicyMetrics
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.finagle.stats.Stat
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Future
import com.twitter.decider.Decider

final case class DataProviderWaitDeadlineExceededException() extends Exception()

final case class EvaluatedActionWithInput(
  ruleAction: Option[RuleAction],
  expressionInput: ExpressionInput)

trait EvaluationServiceInterface[T <: Expression] extends PerPolicyMetrics {
  protected val evaluationEngine: EvaluationEngineInterface[T]
  protected val dataProviderService: DataProviderService[T]
  protected val statsReceiver: StatsReceiver
  protected val logger: JsonLogger
  protected val decider: Decider

  private[evaluationservice] val PolicyDataProviderTimeout = "policyDataProviderTimeout"
  private[evaluationservice] val PolicyDataProviderFailed = "policyDataProviderFailed"
  private[evaluationservice] val DataProvidersWaitTime = "dataProvidersWaitTime"
  private[evaluationservice] val PolicyPassed = "policyPassed"
  private[evaluationservice] val PolicyFailed = "policyFailed"
  private[evaluationservice] val PolicyDisabled = "policyDisabled"

  private val loggerScope = logger.withScope(Scope)
  private val statsScope = statsReceiver.scope(Scope)
  private[evaluationservice] val policyDataProviderTimeoutCounter =
    statsScope.counter(PolicyDataProviderTimeout)
  private[evaluationservice] val policyDataProviderFailedCounter =
    statsScope.counter(PolicyDataProviderFailed)
  private[evaluationservice] val policyPassedCounter =
    statsScope.counter(PolicyPassed)
  private[evaluationservice] val policyFailedCounter =
    statsScope.counter(PolicyFailed)
  private[evaluationservice] val policyDisabledCounter =
    statsScope.counter(PolicyDisabled)
  private[evaluationservice] val dataProvidersWaitTime = statsScope.stat(DataProvidersWaitTime)

  private def policyMustBeApplied(policy: Policy): Boolean = {
    policy.decider match {
      case Some(feature) => decider.isAvailable(feature)
      case None => true
    }
  }

  def execute(
    policy: Policy,
    routeInformation: Option[RouteInformation],
    customInput: Option[ExpressionInput],
    authMetadata: Option[AuthMetadata]
  ): Future[EvaluatedActionWithInput] = {
    policyMustBeApplied(policy) match {
      case true =>
        Stat
          .timeFuture(policyStatsScope(policy).stat(DataProvidersWaitTime)) {
            Stat.timeFuture(dataProvidersWaitTime) {
              dataProviderService
                .getData(
                  policies = Seq(policy),
                  routeInformation = routeInformation,
                  authMetadata = authMetadata)
            }
          }
          .rescue {
            case DataProviderTimeoutException(p, duration) =>
              loggerScope.warning(
                message = "policy execution failed due to timeout in a specific data provider",
                metadata = Some(
                  Map(
                    "policyId" -> policy.policyId,
                    "dataProvider" -> p.getClass.getSimpleName,
                    "expectedDurationMs" -> duration.inMilliseconds))
              )
              policyDataProviderTimeoutCounter.incr()
              policyStatsScope(policy).counter(PolicyDataProviderTimeout).incr()
              Future.exception(DataProviderWaitDeadlineExceededException())
            case DataProviderServiceTimeoutException(duration) =>
              loggerScope.warning(
                message = "policy execution failed due to data provider service wait timeout",
                metadata = Some(
                  Map(
                    "policyId" -> policy.policyId,
                    "expectedDurationMs" -> duration.inMilliseconds))
              )
              policyDataProviderTimeoutCounter.incr()
              policyStatsScope(policy).counter(PolicyDataProviderTimeout).incr()
              Future.exception(DataProviderWaitDeadlineExceededException())
            case e: Exception =>
              loggerScope.error(
                message = "policy execution failed due to unknown data providers exception",
                metadata = Some(Map("policyId" -> policy.policyId, "exception" -> e.getMessage))
              )
              policyDataProviderFailedCounter.incr()
              policyStatsScope(policy).counter(PolicyDataProviderFailed).incr()
              Future.exception(e)
          }
          .flatMap { i =>
            val input = ExpressionInput(customInput match {
              case Some(ci) => i ++ ci
              case _ => i
            })
            evaluationEngine
              .execute(
                policy = policy,
                input = input
              ).map(EvaluatedActionWithInput(_, input))
          }.onSuccess {
            case EvaluatedActionWithInput(Some(_), _) =>
              policyPassedCounter.incr()
              policyStatsScope(policy).counter(PolicyPassed).incr()
            case EvaluatedActionWithInput(None, _) =>
              policyFailedCounter.incr()
              policyStatsScope(policy).counter(PolicyFailed).incr()
          }
      case false =>
        policyStatsScope(policy).counter(PolicyDisabled).incr()
        policyDisabledCounter.incr()
        Future.value(
          EvaluatedActionWithInput(
            None,
            ExpressionInput(customInput match {
              case Some(ci) => ci
              case _ => Map()
            })))
    }
  }
}
