package com.twitter.auth.policykeeper.api.evaluationengine

import com.twitter.auth.policykeeper.thriftscala.RuleAction
import scala.collection.mutable

case class ExpressionResult(priority: Long, evaluationResult: Boolean, requestedAction: RuleAction)

object ExpressionResult {
  val None: ExpressionResult =
    ExpressionResult(
      priority = 0L,
      evaluationResult = false,
      requestedAction = RuleAction(actionNeeded = false, apiErrorCode = Option.empty[Int]))
}

case class ExpressionResultPrioritizer() {
  object ResultPriority extends Ordering[ExpressionResult] {
    def compare(a: ExpressionResult, b: ExpressionResult): Int =
      b.priority compare a.priority
  }
  private val storage = mutable.PriorityQueue.empty(ResultPriority)
  def clear(): Unit = {
    storage.clear()
  }
  def mergeResults(expressionResults: Seq[ExpressionResult]) {
    expressionResults
      .filter { result =>
        result.evaluationResult
      }.foreach(storage += _)
  }
  def top(): Option[ExpressionResult] = {
    storage.headOption
  }
}
