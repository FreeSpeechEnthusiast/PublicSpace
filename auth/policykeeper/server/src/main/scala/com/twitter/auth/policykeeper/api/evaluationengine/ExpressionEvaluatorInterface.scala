package com.twitter.auth.policykeeper.api.evaluationengine

import com.twitter.util.Future

final case class IncompleteInputException() extends Exception

trait ExpressionEvaluatorInterface[T <: Expression] {

  /**
   * Evaluation algorithm implementation
   *
   * @param expression
   * @param expressionInput
   * @return
   */
  protected def doEvaluateExpression(
    expression: T,
    expressionInput: ExpressionInput
  ): Future[Boolean]

  /**
   * Evaluate an expression. Method works on top of concrete evaluation implementation [[doEvaluateExpression]]
   *
   * @param expression
   * @param expressionInput
   * @return
   */
  final def evaluateExpression(
    expression: T,
    expressionInput: ExpressionInput
  ): Future[Boolean] = {
    try {
      doEvaluateExpression(expression = expression, expressionInput = expressionInput)
    } catch {
      case e: Exception => Future.exception(e)
    }
  }
}
