package com.twitter.auth.policykeeper.api.modules.internal

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine.ScalaStaticExpression
import com.twitter.auth.policykeeper.api.evaluationservice.EvaluationServiceInterface
import com.twitter.auth.policykeeper.api.evaluationservice.scalastaticevaluationservice.ScalaStaticEvaluationService
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.services.PassbirdService
import com.twitter.decider.Decider
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.TwitterModule
import com.twitter.servo.util.MemoizingStatsReceiver

object EvaluationServiceModule extends TwitterModule {
  @Provides
  @Singleton
  def providesEvaluationService(
    statsReceiver: StatsReceiver,
    logger: JsonLogger,
    decider: Decider,
    passbirdService: PassbirdService,
    advancedStatsReceiver: MemoizingStatsReceiver
  ): EvaluationServiceInterface[ScalaStaticExpression] =
    ScalaStaticEvaluationService(
      statsReceiver = statsReceiver,
      logger = logger,
      decider = decider,
      passbirdService = passbirdService,
      perPolicyStatsReceiver = advancedStatsReceiver
    )
}
