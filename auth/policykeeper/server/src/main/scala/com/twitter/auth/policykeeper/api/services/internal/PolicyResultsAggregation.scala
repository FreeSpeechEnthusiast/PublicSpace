package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.thriftscala.Code
import com.twitter.auth.policykeeper.thriftscala.Result

trait PolicyResultsAggregation {

  private val blankResult = Result(Code.Noresults)

  /**
   * Decides what result to pick based on map of code of results
   *
   * @param resultsMap
   * @return
   */
  private def resultCodeMapAggregator(resultsMap: Map[Code, Seq[Result]]): Result = {
    // if any policy result returned true then return the first possible result
    val firstPossibleResult = resultsMap.contains(Code.True) match {
      case true =>
        resultsMap(Code.True).find(r => r._2.isDefined || r._3.isDefined)
      case false => None
    }
    Result(
      // if all policy results have same code then use that code otherwise use mixed
      policyExecutionCode = resultsMap.size match {
        case 1 => resultsMap.head._1
        case _ => Code.Mixed
      },
      apiErrorCode = firstPossibleResult match {
        case Some(topResult) => topResult.apiErrorCode
        case _ => None
      },
      bouncerRequest = firstPossibleResult match {
        case Some(topResult) => topResult.bouncerRequest
        case _ => None
      }
    )
  }

  /**
   * Decides what result to pick from multiple policy results
   *
   * @param policyResults
   * @return
   */
  def policyResultsAggregator(policyResults: Seq[Result]): Result = {
    policyResults match {
      case Seq(result) => result
      case Seq() => blankResult
      case _ =>
        resultCodeMapAggregator(
          policyResults
            .groupBy(_._1)
        )
    }
  }

}
