package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationengine.IncompleteInputException
import com.twitter.util.Future

class DoesntHaveAccessToken extends BaseScalaStaticRuleInterface {

  /**
   * Defines the scala expression and calculate the result
   * Expression syntax is DoesntHaveAccessToken(hasAccessToken)
   *
   * @param positionalVars expression input
   * @return
   * @throws IncompleteInputException
   */
  override def apply(
    positionalVars: Seq[ExpressionInputParameterValue]
  ): Future[Boolean] = {
    positionalVars match {
      case Seq(hasAccessToken) =>
        Future.value(
          !hasAccessToken.toBoolean //target expression
        )
      case _ => throw IncompleteInputException()
    }

  }
}
