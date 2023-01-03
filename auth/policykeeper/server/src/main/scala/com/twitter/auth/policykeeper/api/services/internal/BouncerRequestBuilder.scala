package com.twitter.auth.policykeeper.api.services.internal

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.interpolation.InputInterpolator
import com.twitter.auth.policykeeper.thriftscala.BouncerRequest
import com.twitter.auth.policykeeper.thriftscala.BouncerSettings
import com.twitter.auth.policykeeper.thriftscala.DeepStringMap
import com.twitter.bouncer.templates.thriftscala.TSLAAuthChallengeData
import com.twitter.bouncer.templates.thriftscala.Tag
import com.twitter.bouncer.templates.thriftscala.TemplateData
import com.twitter.bouncer.templates.thriftscala.TemplateId
import com.twitter.bouncer.thriftscala.Bounce
import com.twitter.bouncer.thriftscala.BounceExperience
import com.twitter.bouncer.thriftscala.FeatureTarget
import com.twitter.bouncer.thriftscala.SessionTarget
import com.twitter.bouncer.thriftscala.Target
import com.twitter.bouncer.thriftscala.UserTarget
import com.twitter.tsla.authevents.thriftscala.AuthEventType

/**
 * Generate bouncer request based on bouncer settings and policy input
 */
case class BouncerRequestBuilder() {
  private[services] val inputInterpolator = InputInterpolator()

  def bouncerRequest(
    bouncerSettings: Option[BouncerSettings],
    input: ExpressionInput
  ): Option[BouncerRequest] = {
    bouncerSettings match {
      case Some(s) =>
        val interpolatedSettings = inputInterpolator.interpolateInputIntoSettings(s, input)
        Some(
          BouncerRequest(
            bounce = Bounce(
              experience = interpolatedSettings.experience match {
                case Some(e) =>
                  BounceExperience.valueOf(e)
                case None => None
              },
              location = interpolatedSettings.location,
              errorMessage = interpolatedSettings.errorMessage,
              deeplink = interpolatedSettings.deepLink
            ),
            templateIds = interpolatedSettings.templateIds.map { strId =>
              TemplateId(id = strId)
            },
            templateData = buildTemplateData(interpolatedSettings),
            referringTags = interpolatedSettings.referringTags match {
              case Some(t) =>
                Some(
                  t.map { strTag =>
                      Tag.valueOf(strTag)
                    }.collect {
                      case Some(v) => v
                    })
              case None => None
            },
            target = interpolatedSettings.target.targetType.toLowerCase match {
              case "user" if interpolatedSettings.target.userId.isDefined =>
                Target.User(UserTarget(userId = interpolatedSettings.target.userId.get.toLong))
              case "session"
                  if interpolatedSettings.target.userId.isDefined && interpolatedSettings.target.sessionHash.isDefined =>
                Target.Session(
                  SessionTarget(
                    userId = interpolatedSettings.target.userId.get.toLong,
                    sessionHash = interpolatedSettings.target.sessionHash.get,
                    feature = interpolatedSettings.target.feature
                  ))
              case "feature"
                  if interpolatedSettings.target.userId.isDefined && interpolatedSettings.target.feature.isDefined =>
                Target.Feature(
                  FeatureTarget(
                    userId = interpolatedSettings.target.userId.get.toLong,
                    feature = interpolatedSettings.target.feature.get))
            }
          ))
      case None => None
    }
  }

  private[services] def buildTemplateData(
    interpolatedBouncerSettings: BouncerSettings
  ): Option[TemplateData] = {
    interpolatedBouncerSettings.templateMapping match {
      case Some(map) =>
        Some(
          TemplateData(
            /** These options are not supported yet
            deleteAbusiveTweetIds = ???,
            timeoutOneOffExpiresAtMsec = ???,
            deleteMomentIds = ???,
            labelData = ???,
            engagementActionData = ???,
            legalRequestData = ???,
            readonlyOneOffExpiresAtMsec = ???,
            deleteProfileEntities = ???,
            interstitialDmIds = ???,
            userRatelimitValue = ???,
            deleteTweetIdToTag = ???,
            profileEntitiesToTag = ???,
            tslaAuthChallengeData = ???,
            policyActionData = ???
            **/
            redirectUrl = map.get("redirectUrl") match {
              case Some(redirectUrlParams) if !redirectUrlParams.isMap =>
                redirectUrlParams.stringVal
              case None => None
            },
            tslaAuthChallengeData = map.get("tslaAuthChallengeData") match {
              case Some(tslaAuthChallengeDataParams) if tslaAuthChallengeDataParams.isMap =>
                tslaAuthChallengeDataParams.mapVal match {
                  case Some(tslaAuthChallengeDataParamsMap) =>
                    (
                      tslaAuthChallengeDataParamsMap.get("token"),
                      tslaAuthChallengeDataParamsMap.get("tokenKind"),
                      tslaAuthChallengeDataParamsMap.get("authEventType")) match {
                      case (Some(token), Some(tokenKind), mayBeAuthEventType) =>
                        Some(
                          TSLAAuthChallengeData(
                            token = unpackDeepStringMapStringValue(token).getOrElse(""),
                            tokenKind =
                              unpackDeepStringMapStringValue(tokenKind).getOrElse("0").toInt,
                            authEventType = mayBeAuthEventType match {
                              case Some(v) =>
                                AuthEventType.valueOf(
                                  unpackDeepStringMapStringValue(v).getOrElse(""))
                              case None => None
                            }
                          ))
                      case _ => None
                    }
                  case None => None
                }
              case None => None
            }
          ))
      case None => None
    }
  }

  private[services] def unpackDeepStringMapStringValue(m: DeepStringMap): Option[String] = {
    if (!m.isMap) {
      m.stringVal
    } else {
      None
    }
  }
}
