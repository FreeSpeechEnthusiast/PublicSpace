package com.twitter.auth.policykeeper.api.evaluationservice.scalastaticevaluationservice

import com.google.inject.Inject
import com.twitter.servo.util.MemoizingStatsReceiver
import com.twitter.auth.policykeeper.api.dataproviderservice.DataProviderService
import com.twitter.auth.policykeeper.api.evaluationengine.EvaluationEngineInterface
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionParserInterface
import com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine.ScalaStaticEvaluationEngine
import com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine.ScalaStaticExpression
import com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine.ScalaStaticExpressionParser
import com.twitter.auth.policykeeper.api.evaluationservice.EvaluationServiceInterface
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.services.PassbirdService
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.decider.Decider

final case class ScalaStaticEvaluationService @Inject() (
  override protected val statsReceiver: StatsReceiver,
  override protected val logger: JsonLogger,
  override protected val decider: Decider,
  passbirdService: PassbirdService,
  override protected val perPolicyStatsReceiver: MemoizingStatsReceiver)
    extends EvaluationServiceInterface[ScalaStaticExpression] {
  protected val expressionParser: ExpressionParserInterface[ScalaStaticExpression] =
    ScalaStaticExpressionParser()
  override protected val evaluationEngine: EvaluationEngineInterface[ScalaStaticExpression] =
    ScalaStaticEvaluationEngine(
      statsReceiver = statsReceiver,
      logger = logger,
      perPolicyStatsReceiver = perPolicyStatsReceiver,
      passbirdService = passbirdService)
  override protected val dataProviderService: DataProviderService[ScalaStaticExpression] =
    DataProviderService(
      expressionParser,
      perDataProviderStatsReceiver = perPolicyStatsReceiver,
      statsReceiver = statsReceiver,
      logger = logger)
}
