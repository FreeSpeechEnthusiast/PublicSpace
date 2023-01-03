package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine.ScalaStaticExpression
import com.twitter.auth.policykeeper.api.evaluationservice.EvaluationServiceInterface
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.RouteTag
import com.twitter.auth.policykeeper.api.storage.RouteTags
import com.twitter.auth.policykeeper.api.storage.StorageInterface
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService.VerifyRoutePolicies
import com.twitter.auth.policykeeper.thriftscala.VerifyRoutePoliciesResponse
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Future
import javax.inject.Inject
import javax.inject.Singleton
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.servo.util.MemoizingStatsReceiver

@Singleton
class VerifyRoutePoliciesService @Inject() (
  storage: StorageInterface[String, RouteTag],
  evaluationService: EvaluationServiceInterface[ScalaStaticExpression],
  protected val statsReceiver: StatsReceiver,
  protected val logger: JsonLogger,
  protected val advancedStatsReceiver: MemoizingStatsReceiver)
    extends VerifyRoutePolicies.ReqRepServicePerEndpointServiceType
    with PolicyResultsMapper
    with PolicyResultsAggregation
    with FailOpenPolicies {

  def apply(
    thriftRequest: Request[VerifyRoutePolicies.Args]
  ): Future[Response[VerifyRoutePoliciesResponse]] = {
    storage
      .getAssociatedPolicies(
        RouteTags.fromRouteInformation(thriftRequest.args.request.routeInformation)
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
                    routeInformation = Some(thriftRequest.args.request.routeInformation),
                    customInput = None,
                    authMetadata = thriftRequest.args.request.authMetadata
                  )).map(result => handleFailOpenPolicyResult(policy, result))
            }
        )
      }.flatMap { futureSequenceOfResults =>
        // interpret multiple results as single result
        futureSequenceOfResults.map(policyResultsAggregator)
      }.map(v => Response(VerifyRoutePoliciesResponse(executionResult = v)))
  }

}
