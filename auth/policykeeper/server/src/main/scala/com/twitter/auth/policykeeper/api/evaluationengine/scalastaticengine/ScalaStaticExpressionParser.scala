package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionParserInterface

final case class ScalaStaticExpressionParser()
    extends ExpressionParserInterface[ScalaStaticExpression] {
  override def parse(expression: String): ScalaStaticExpression = {
    ScalaStaticExpression(expression)
  }
}
