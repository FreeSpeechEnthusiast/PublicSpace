package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.thriftscala.Code
import com.twitter.auth.policykeeper.thriftscala.Result
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PolicyResultsAggregationTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter
    with PolicyResultsAggregation {

  test("test policy results aggregator (one result, true)") {
    policyResultsAggregator(Seq(Result(policyExecutionCode = Code.True))) mustBe Result(Code.True)
  }

  test("test policy results aggregator (one result, false)") {
    policyResultsAggregator(Seq(Result(policyExecutionCode = Code.False))) mustBe Result(Code.False)
  }

  test("test policy results aggregator (one result, failed)") {
    policyResultsAggregator(Seq(Result(policyExecutionCode = Code.Failed))) mustBe Result(
      Code.Failed)
  }

  test("test policy results aggregator (two results, true + false)") {
    policyResultsAggregator(
      Seq(
        Result(policyExecutionCode = Code.True, Some(501)),
        Result(policyExecutionCode = Code.False))) mustBe Result(Code.Mixed, Some(501))
  }

  test("test policy results aggregator (two results, true + true)") {
    policyResultsAggregator(
      Seq(
        Result(policyExecutionCode = Code.True, Some(501)),
        Result(policyExecutionCode = Code.True, Some(502)))) mustBe Result(Code.True, Some(501))
  }

  test("test policy results aggregator (two results, true + true, another order)") {
    policyResultsAggregator(
      Seq(
        Result(policyExecutionCode = Code.True, Some(502)),
        Result(policyExecutionCode = Code.True, Some(501)))) mustBe Result(Code.True, Some(502))
  }

  test("test policy results aggregator (two results, true without code + true)") {
    policyResultsAggregator(
      Seq(
        Result(policyExecutionCode = Code.True),
        Result(policyExecutionCode = Code.True, Some(502)))) mustBe Result(Code.True, Some(502))
  }

  test("test policy results aggregator (two results, false + failed)") {
    policyResultsAggregator(
      Seq(
        Result(policyExecutionCode = Code.Failed),
        Result(policyExecutionCode = Code.False))) mustBe Result(Code.Mixed)
  }

  test("test policy results aggregator (two results, false + true + true)") {
    policyResultsAggregator(
      Seq(
        Result(policyExecutionCode = Code.False),
        Result(policyExecutionCode = Code.True, Some(502)),
        Result(policyExecutionCode = Code.True, Some(501)))) mustBe Result(Code.Mixed, Some(502))
  }

  test("test policy results aggregator (0 results)") {
    policyResultsAggregator(Seq()) mustBe Result(Code.Noresults)
  }

}
