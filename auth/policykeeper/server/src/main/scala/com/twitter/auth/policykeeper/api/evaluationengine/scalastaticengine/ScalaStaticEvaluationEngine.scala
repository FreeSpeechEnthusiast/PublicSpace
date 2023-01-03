package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.google.inject.Inject
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.evaluationengine.EvaluationEngineInterface
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionEvaluatorInterface
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionParserInterface
import com.twitter.auth.policykeeper.api.services.PassbirdService
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.servo.util.MemoizingStatsReceiver

final case class ScalaStaticEvaluationEngine @Inject() (
  override protected val statsReceiver: StatsReceiver,
  override protected val logger: JsonLogger,
  // reuse memoizing stats receiver from the evaluation service for optimized memory consumption
  override protected val perPolicyStatsReceiver: MemoizingStatsReceiver,
  passbirdService: PassbirdService)
    extends EvaluationEngineInterface[ScalaStaticExpression] {
  override protected val expressionParser: ExpressionParserInterface[ScalaStaticExpression] =
    ScalaStaticExpressionParser()
  override protected val expressionEvaluator: ExpressionEvaluatorInterface[
    ScalaStaticExpression
  ] =
    ScalaStaticExpressionEvaluator(passbirdService)
}
