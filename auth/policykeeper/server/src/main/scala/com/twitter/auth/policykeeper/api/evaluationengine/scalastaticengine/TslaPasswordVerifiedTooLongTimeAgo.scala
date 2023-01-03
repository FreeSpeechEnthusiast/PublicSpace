package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationengine.IncompleteInputException
import com.twitter.util.Future
import com.twitter.util.Time
import com.twitter.util.Duration

class TslaPasswordVerifiedTooLongTimeAgo extends BaseScalaStaticRuleInterface {

  /**
   * Defines the scala expression and calculate the result
   * Expression syntax is TslaPasswordVerifiedTooLongTimeAgo(lastPasswordVerifiedTimestampMs, sessionLimitDurationMinutes)
   *
   * @param positionalVars expression input
   * @return
   * @throws IncompleteInputException
   */
  override def apply(
    positionalVars: Seq[ExpressionInputParameterValue]
  ): Future[Boolean] = {
    positionalVars match {
      case Seq(lastPasswordVerifiedTimestampMs, sessionLimitDurationMinutes) =>
        Future.value(
          (lastPasswordVerifiedTimestampMs.toLong < (Time.now - Duration
            .fromMinutes(sessionLimitDurationMinutes.toInt)).inMilliseconds) //target expression
        )
      case _ => throw IncompleteInputException()
    }

  }
}
