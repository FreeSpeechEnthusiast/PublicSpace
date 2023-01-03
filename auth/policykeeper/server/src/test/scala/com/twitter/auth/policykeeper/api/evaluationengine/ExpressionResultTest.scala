package com.twitter.auth.policykeeper.api.evaluationengine

import com.twitter.auth.policykeeper.thriftscala.RuleAction
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExpressionResultTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  val prioritizer = ExpressionResultPrioritizer()

  before {
    prioritizer.clear()
  }

  test("test expression result prioritizer without input") {
    prioritizer.top() mustBe (None)
  }

  test("test expression result prioritizer clear") {
    prioritizer.mergeResults(Seq(ExpressionResult(5, true, RuleAction(actionNeeded = false, None))))
    prioritizer.clear()
    prioritizer.top() mustBe (None)
  }

  test("test expression result prioritizer with input") {
    prioritizer.mergeResults(
      Seq(
        ExpressionResult(5, true, RuleAction(actionNeeded = false, None)),
        ExpressionResult(50, false, RuleAction(actionNeeded = false, None)),
        ExpressionResult(2, true, RuleAction(actionNeeded = false, None)),
        ExpressionResult(0, false, RuleAction(actionNeeded = false, None)),
        ExpressionResult(3, true, RuleAction(actionNeeded = false, None))
      )
    )
    prioritizer.top() mustBe Some(ExpressionResult(2, true, RuleAction(actionNeeded = false, None)))
  }

}
