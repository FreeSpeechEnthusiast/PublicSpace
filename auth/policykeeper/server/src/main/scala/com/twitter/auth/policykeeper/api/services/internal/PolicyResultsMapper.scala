package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputTypeCastException
import com.twitter.auth.policykeeper.api.evaluationengine.IncompleteInputException
import com.twitter.auth.policykeeper.api.evaluationengine.InvalidRuleExpression
import com.twitter.auth.policykeeper.api.evaluationengine.UnknownInputTypeException
import com.twitter.auth.policykeeper.api.evaluationengine.UnknownTargetTypeException
import com.twitter.auth.policykeeper.api.evaluationservice.DataProviderWaitDeadlineExceededException
import com.twitter.auth.policykeeper.api.evaluationservice.EvaluatedActionWithInput
import com.twitter.auth.policykeeper.thriftscala.Code
import com.twitter.auth.policykeeper.thriftscala.Result
import com.twitter.util.Future

trait PolicyResultsMapper {

  private val bouncerRequestBuilder = BouncerRequestBuilder()

  /**
   * Converts result from [[com.twitter.auth.policykeeper.api.evaluationservice.EvaluationServiceInterface]]
   * to Thrift result
   *
   * @param evaluationResult
   * @return
   */
  def mapPolicyEvaluationResult(
    evaluationResult: Future[EvaluatedActionWithInput]
  ): Future[Result] = {
    evaluationResult // remap results to thrift result
      .map {
        case EvaluatedActionWithInput(Some(r), i) if r.actionNeeded =>
          Result(
            Code.True,
            apiErrorCode = r.apiErrorCode,
            bouncerRequest = bouncerRequestBuilder.bouncerRequest(r.bouncerSettings, i))
        case EvaluatedActionWithInput(Some(r), _) =>
          Result(Code.True)
        case EvaluatedActionWithInput(None, _) =>
          Result(Code.False)
      }
      // remap exceptions to thrift result
      .rescue {
        case _: IncompleteInputException =>
          Future.value(Result(Code.Noinput))
        case _: UnknownTargetTypeException =>
          Future.value(Result(Code.Badinput))
        case _: UnknownInputTypeException =>
          Future.value(Result(Code.Badinput))
        case _: ExpressionInputTypeCastException =>
          Future.value(Result(Code.Badinput))
        case _: InvalidRuleExpression =>
          Future.value(Result(Code.Impossible))
        case _: DataProviderWaitDeadlineExceededException =>
          Future.value(Result(Code.Timeout))
        case _ =>
          Future.value(Result(Code.Failed))
      }
  }
}
