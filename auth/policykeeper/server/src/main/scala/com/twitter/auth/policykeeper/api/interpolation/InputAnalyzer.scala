package com.twitter.auth.policykeeper.api.interpolation

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.thriftscala.DeepStringMap
import com.twitter.auth.policykeeper.thriftscala.RuleAction

case class InputAnalyzer() {

  import InputInterpolator._

  private[interpolation] def requiredInputForString(
    string: String,
  ): Set[ExpressionInputParameterName] = {
    matcher
      .findAllMatchIn(string).collect {
        case m => matchToExpressionInputParameterName(m)
      }.toSet
  }

  private[interpolation] def extractValuesFromDeepStringMap(
    map: DeepStringMap,
  ): Seq[Option[String]] = {
    if (!map.isMap) {
      Seq(map.stringVal)
    } else {
      map.mapVal match {
        case Some(childMap) =>
          childMap
            .map {
              case (_, v) => extractValuesFromDeepStringMap(v)
            }.foldLeft(Seq.empty[Option[String]]) {
              _ ++ _
            }
        case None => Seq()
      }
    }

  }

  def requiredInputFor(action: RuleAction): Set[ExpressionInputParameterName] = {
    if (action.actionNeeded && action.apiErrorCode.isEmpty) {
      action.bouncerSettings match {
        case Some(b) => // collect all parameters with possible input
          (Seq(
            b.location,
            b.errorMessage,
            b.deepLink,
            b.experience,
            Some(b.target.targetType),
            b.target.userId,
            b.target.sessionHash,
            b.target.feature
          ) ++ b.templateIds.map(Some(_)) ++ (b.referringTags match {
            case Some(seq) => seq.map(Some(_))
            case None => Seq()
          }) ++ (b.templateMapping match {
            case Some(mapping) =>
              mapping
                .map {
                  case (_, v) => extractValuesFromDeepStringMap(v)
                }.foldLeft(Seq.empty[Option[String]]) {
                  _ ++ _
                }
            case None => Seq()
          }))
            .collect {
              case Some(v) => requiredInputForString(v)
            }
            // merge into single set
            .foldLeft(Set.empty[ExpressionInputParameterName]) {
              _ ++ _
            }
        case None => Set()
      }
    } else Set()
  }

}
