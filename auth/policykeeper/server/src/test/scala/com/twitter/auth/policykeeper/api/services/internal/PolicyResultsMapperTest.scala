package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputTypeCastException
import com.twitter.auth.policykeeper.api.evaluationengine.IncompleteInputException
import com.twitter.auth.policykeeper.api.evaluationengine.InvalidRuleExpression
import com.twitter.auth.policykeeper.api.evaluationengine.UnknownInputTypeException
import com.twitter.auth.policykeeper.api.evaluationservice.DataProviderWaitDeadlineExceededException
import com.twitter.auth.policykeeper.api.evaluationservice.EvaluatedActionWithInput
import com.twitter.auth.policykeeper.thriftscala.Code
import com.twitter.auth.policykeeper.thriftscala.Result
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PolicyResultsMapperTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter
    with PolicyResultsMapper {

  protected val input = ExpressionInput(Map())

  test("test mapper with negative result") {
    Await.result(
      mapPolicyEvaluationResult(Future.value(EvaluatedActionWithInput(None, input)))) mustBe Result(
      Code.False)
  }

  test("test mapper with positive result without action") {
    Await.result(
      mapPolicyEvaluationResult(Future.value(
        EvaluatedActionWithInput(Some(RuleAction(actionNeeded = false)), input)))) mustBe Result(
      Code.True)
  }

  test("test mapper with positive result and action") {
    Await.result(
      mapPolicyEvaluationResult(
        Future.value(EvaluatedActionWithInput(
          Some(RuleAction(actionNeeded = true, apiErrorCode = Some(501))),
          input)))) mustBe Result(Code.True, apiErrorCode = Some(501))
  }

  test("test mapper with IncompleteInputException exception") {
    Await.result(
      mapPolicyEvaluationResult(Future.exception(IncompleteInputException()))) mustBe Result(
      Code.Noinput)
  }

  test("test mapper with UnknownInputTypeException exception") {
    Await.result(
      mapPolicyEvaluationResult(Future.exception(UnknownInputTypeException()))) mustBe Result(
      Code.Badinput)
  }

  test("test mapper with ExpressionInputTypeCastException exception") {
    Await.result(
      mapPolicyEvaluationResult(
        Future.exception(ExpressionInputTypeCastException()))) mustBe Result(Code.Badinput)
  }

  test("test mapper with InvalidRuleExpression exception") {
    Await.result(
      mapPolicyEvaluationResult(Future.exception(InvalidRuleExpression()))) mustBe Result(
      Code.Impossible)
  }

  test("test mapper with DataProviderWaitDeadlineExceededException exception") {
    Await.result(
      mapPolicyEvaluationResult(
        Future.exception(DataProviderWaitDeadlineExceededException()))) mustBe Result(Code.Timeout)
  }

  test("test mapper with other exceptions") {
    Await.result(mapPolicyEvaluationResult(Future.exception(new Exception()))) mustBe Result(
      Code.Failed)
  }

}
