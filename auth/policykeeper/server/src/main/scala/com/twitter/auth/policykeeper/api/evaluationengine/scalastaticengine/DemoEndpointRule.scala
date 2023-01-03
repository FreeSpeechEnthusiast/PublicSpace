package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationengine.IncompleteInputException
import com.twitter.util.Future

/**
 * This rule is designed for unit testing only.
 * Do not use for any other purposes!
 */
class DemoEndpointRule extends BaseScalaStaticRuleInterface {

  /**
   * Defines the scala expression and calculate the result
   * Expression syntax is DemoEndpointRule(arg1, arg2, arg3)
   *
   * @param positionalVars expression input
   * @return
   * @throws IncompleteInputException
   */
  override def apply(
    positionalVars: Seq[ExpressionInputParameterValue]
  ): Future[Boolean] = {
    positionalVars match {
      case Seq(arg1, arg2, arg3) =>
        Future.value(
          arg3.toBoolean && (arg1.toInt < 100 && arg2.toInt > 1000) //target expression
        )
      case _ => throw IncompleteInputException()
    }

  }
}
