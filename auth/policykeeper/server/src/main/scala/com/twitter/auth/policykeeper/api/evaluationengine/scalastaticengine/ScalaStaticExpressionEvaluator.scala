package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.google.inject.Inject
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionEvaluatorInterface
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.services.PassbirdService
import com.twitter.util.Future

final case class UnknownExpressionClassException() extends Exception

@Inject()
final case class ScalaStaticExpressionEvaluator @Inject() (passbirdService: PassbirdService)
    extends ExpressionEvaluatorInterface[ScalaStaticExpression] {

  private val doesntHaveAccessToken = new DoesntHaveAccessToken()
  private val tslaPasswordVerifiedTooLongTimeAgo = new TslaPasswordVerifiedTooLongTimeAgo()
  private val tslaInvalidCurrentPassword = new TslaInvalidCurrentPassword(
    passbirdService: PassbirdService)
  private val demoEndpointRule = new DemoEndpointRule()
  private val demoAuthRule = new DemoAuthRule()

  override def doEvaluateExpression(
    expression: ScalaStaticExpression,
    expressionInput: ExpressionInput
  ): Future[Boolean] = {
    // fill rule arguments with user input
    val positionalVars = expression.positionedArgs.map {
      expressionInput(_)
    }
    expression.expressionClass match {
      case Some("DemoAuthRule") =>
        demoAuthRule(positionalVars)
      case Some("DemoEndpointRule") =>
        demoEndpointRule(positionalVars)
      case Some("TslaInvalidCurrentPassword") =>
        tslaInvalidCurrentPassword(positionalVars)
      case Some("TslaPasswordVerifiedTooLongTimeAgo") =>
        tslaPasswordVerifiedTooLongTimeAgo(positionalVars)
      case Some("DoesntHaveAccessToken") =>
        doesntHaveAccessToken(positionalVars)
      //TODO: add more rules
      case _ => throw UnknownExpressionClassException()
    }
  }

}
