package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine.ScalaStaticExpression
import com.twitter.auth.policykeeper.api.evaluationservice.EvaluationServiceInterface
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.RouteTag
import com.twitter.auth.policykeeper.api.storage.StorageInterface
import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService.VerifyPolicies
import com.twitter.auth.policykeeper.thriftscala.VerifyPoliciesResponse
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Future
import javax.inject.Inject
import javax.inject.Singleton
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.servo.util.MemoizingStatsReceiver

@Singleton
class VerifyPoliciesService @Inject() (
  storage: StorageInterface[String, RouteTag],
  evaluationService: EvaluationServiceInterface[ScalaStaticExpression],
  protected val statsReceiver: StatsReceiver,
  protected val logger: JsonLogger,
  protected val advancedStatsReceiver: MemoizingStatsReceiver)
    extends VerifyPolicies.ReqRepServicePerEndpointServiceType
    with PolicyResultsMapper
    with PolicyResultsAggregation
    with FailOpenPolicies {

  def apply(
    thriftRequest: Request[VerifyPolicies.Args]
  ): Future[Response[VerifyPoliciesResponse]] = {
    storage
      .getPoliciesByIds(
        thriftRequest.args.request.policyIds.map(PolicyId(_)).toSeq
      ).map { policies =>
        // remap policies to future of collection of results
        Future.collect(
          policies
          // remap each policy to future result
            .map { policy =>
              mapPolicyEvaluationResult(
                evaluationService
                  .execute(
                    policy = policy,
                    routeInformation = None,
                    customInput = thriftRequest.args.request._2 match {
                      case Some(inputMap) =>
                        // remap custom input to ExpressionInput
                        Some(ExpressionInput(underlyingMap = inputMap
                          .map {
                            case (k, v) =>
                              (
                                ExpressionInputParameterName.fromString(k),
                                ExpressionInputParameterValue(v))
                          }.collect {
                            case (Some(k), v) => (k, v)
                          }.toMap))
                      case None => None
                    },
                    authMetadata = thriftRequest.args.request.authMetadata
                  )).map(result => handleFailOpenPolicyResult(policy, result))
            }
        )
      }.flatMap { futureSequenceOfResults =>
        // interpret multiple results as single result
        futureSequenceOfResults.map(policyResultsAggregator)
      }.map(v => Response(VerifyPoliciesResponse(executionResult = v)))
  }

}
