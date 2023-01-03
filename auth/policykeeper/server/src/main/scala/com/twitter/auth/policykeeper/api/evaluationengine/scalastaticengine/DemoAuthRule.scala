package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationengine.IncompleteInputException
import com.twitter.util.Future

/**
 * This rule is designed for unit testing only.
 * Do not use for any other purposes!
 */
class DemoAuthRule extends BaseScalaStaticRuleInterface {

  /**
   * Defines the scala expression and calculate the result
   * Expression syntax is DemoAuthRule(authenticatedUserId)
   *
   * @param positionalVars expression input
   * @return
   * @throws IncompleteInputException
   */
  override def apply(
    positionalVars: Seq[ExpressionInputParameterValue]
  ): Future[Boolean] = {
    positionalVars match {
      case Seq(authenticatedUserId) =>
        Future.value(
          authenticatedUserId.toInt > 0 //target expression
        )
      case _ => throw IncompleteInputException()
    }

  }
}
