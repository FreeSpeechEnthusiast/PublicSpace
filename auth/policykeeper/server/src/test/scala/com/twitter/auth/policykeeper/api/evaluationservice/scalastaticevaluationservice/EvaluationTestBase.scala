package com.twitter.auth.policykeeper.api.evaluationservice.scalastaticevaluationservice

import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.services.PassbirdService
import com.twitter.decider.Decider
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.logging.Logger
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import com.twitter.servo.util.MemoizingStatsReceiver

class EvaluationTestBase
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  protected val statsReceiver = new InMemoryStatsReceiver
  protected val perPolicyStatsReceiver = new MemoizingStatsReceiver(statsReceiver)
  protected val logger = Logger.get()
  protected val jsonLogger = JsonLogger(logger)
  protected val decider = mock[Decider]
  protected val passbirdServiceMock = mock[PassbirdService]

  protected val evaluationService =
    ScalaStaticEvaluationService(
      statsReceiver = statsReceiver,
      logger = jsonLogger,
      decider = decider,
      passbirdService = passbirdServiceMock,
      perPolicyStatsReceiver = perPolicyStatsReceiver
    )

}
