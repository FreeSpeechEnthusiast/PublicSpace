package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.google.inject.Inject
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationengine.IncompleteInputException
import com.twitter.auth.policykeeper.api.services.PassbirdService
import com.twitter.util.Future

class TslaInvalidCurrentPassword @Inject() (passbirdService: PassbirdService)
    extends BaseScalaStaticRuleInterface {

  /**
   * Defines the scala expression and calculate the result
   * Expression syntax is TslaInvalidCurrentPassword(userId, currentPasswordInput, sessionHash)
   *
   * @param positionalVars expression input
   *
   * @return
   *
   * @throws IncompleteInputException
   */
  override def apply(
    positionalVars: Seq[ExpressionInputParameterValue]
  ): Future[Boolean] = {
    positionalVars match {
      case Seq(userId, currentPasswordInput, sessionHash) =>
        passbirdService
          .verifyUserPassword(
            userId.toLong,
            currentPasswordInput.toString,
            Some(sessionHash.toString)).map {
            case true => false
            case false => true
          }
      case _ => throw IncompleteInputException()
    }
  }
}
