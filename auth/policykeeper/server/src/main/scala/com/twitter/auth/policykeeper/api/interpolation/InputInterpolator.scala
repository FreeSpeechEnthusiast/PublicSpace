package com.twitter.auth.policykeeper.api.interpolation

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.thriftscala.BouncerSettings
import com.twitter.auth.policykeeper.thriftscala.BouncerTargetSettings
import com.twitter.auth.policykeeper.thriftscala.DeepStringMap
import com.twitter.util.Try
import scala.util.matching.Regex

object InputInterpolator {
  private[interpolation] val matcher =
    "\\{\\{\\s*([a-zA-Z0-9_]+)(\\.([a-zA-Z0-9_]+))?\\s*\\}\\}".r(
      ExpressionInputParameterName.Namespace,
      "",
      ExpressionInputParameterName.Name)

  private val DefaultVarValue = ""

  private[interpolation] def matchToExpressionInputParameterName(
    m: Regex.Match
  ): ExpressionInputParameterName = {
    val mayBeName = Try {
      m.group(ExpressionInputParameterName.Name)
    }.toOption match {
      case Some(null) => None
      case Some(v) => Some(v)
      case None => None
    }
    val (name, namespace) = mayBeName match {
      case Some(n) => (n, m.group(ExpressionInputParameterName.Namespace))
      case None =>
        (
          m.group(ExpressionInputParameterName.Namespace),
          ExpressionInputParameterName.DefaultNamespace)
    }
    ExpressionInputParameterName(
      name = name,
      namespace = namespace
    )
  }
}

case class InputInterpolator() {
  import InputInterpolator._

  private[interpolation] def interpolateInputIntoString(
    string: String,
    input: ExpressionInput
  ): String = {
    matcher.replaceAllIn(
      string,
      m => {
        input.get(matchToExpressionInputParameterName(m)) match {
          case Some(varValue) => varValue.toString
          case None => DefaultVarValue
        }
      }
    )
  }

  private[interpolation] def interpolateInputIntoDeepStringMap(
    map: DeepStringMap,
    input: ExpressionInput
  ): DeepStringMap = {
    DeepStringMap(
      isMap = map.isMap,
      stringVal = map.stringVal match {
        case Some(s) => Some(interpolateInputIntoString(s, input))
        case None => None
      },
      mapVal = map.mapVal match {
        case Some(mapping) =>
          Some(mapping.map {
            case (k, v) => (k, interpolateInputIntoDeepStringMap(v, input))
          })
        case None => None
      }
    )
  }

  private[api] def interpolateInputIntoSettings(
    bouncerSettings: BouncerSettings,
    input: ExpressionInput
  ): BouncerSettings = {
    BouncerSettings(
      location = bouncerSettings.location match {
        case Some(v) => Some(interpolateInputIntoString(v, input))
        case None => None
      },
      errorMessage = bouncerSettings.errorMessage match {
        case Some(v) => Some(interpolateInputIntoString(v, input))
        case None => None
      },
      deepLink = bouncerSettings.deepLink match {
        case Some(v) => Some(interpolateInputIntoString(v, input))
        case None => None
      },
      experience = bouncerSettings.experience match {
        case Some(v) => Some(interpolateInputIntoString(v, input))
        case None => None
      },
      templateIds = bouncerSettings.templateIds.map { v =>
        interpolateInputIntoString(v, input)
      },
      templateMapping = bouncerSettings.templateMapping match {
        case Some(mapping) =>
          Some(mapping.map {
            case (k, v) => (k, interpolateInputIntoDeepStringMap(v, input))
          })
        case None => None
      },
      referringTags = bouncerSettings.referringTags match {
        case Some(tags) =>
          Some(tags.map { v =>
            interpolateInputIntoString(v, input)
          })
        case None => None
      },
      target = BouncerTargetSettings(
        targetType = interpolateInputIntoString(bouncerSettings.target.targetType, input),
        userId = bouncerSettings.target.userId match {
          case Some(v) => Some(interpolateInputIntoString(v, input))
          case None => None
        },
        sessionHash = bouncerSettings.target.sessionHash match {
          case Some(v) => Some(interpolateInputIntoString(v, input))
          case None => None
        },
        feature = bouncerSettings.target.feature match {
          case Some(v) => Some(interpolateInputIntoString(v, input))
          case None => None
        }
      )
    )
  }
}
