package com.twitter.auth.policykeeper.api.evaluationengine

abstract class Expression(underlyingExpression: String) {
  def requiredInput(): Set[ExpressionInputParameterName]
  def isValid: Boolean
}
