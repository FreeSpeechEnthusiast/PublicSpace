package com.twitter.auth.policykeeper.api.evaluationengine

trait ExpressionParserInterface[T <: Expression] {
  def parse(
    expression: String
  ): T
}
