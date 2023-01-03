package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.util.Future

trait BaseScalaStaticRuleInterface {

  /**
   * Defines the scala expression and calculate the result
   *
   * @param positionalArgs function arguments
   * @return
   * @throws IncompleteInputException
   */
  def apply(positionalArgs: Seq[ExpressionInputParameterValue]): Future[Boolean]
}
