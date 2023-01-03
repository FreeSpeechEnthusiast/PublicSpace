package com.twitter.auth.policykeeper.api.storage.configbus

import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.configbus.parser.SerializableBouncerSettings
import com.twitter.auth.policykeeper.api.storage.configbus.parser.SerializableBouncerTargetSettings
import com.twitter.auth.policykeeper.api.storage.configbus.parser.SerializablePolicy
import com.twitter.auth.policykeeper.api.storage.configbus.parser.SerializableRule
import com.twitter.auth.policykeeper.api.storage.configbus.parser.SerializableRuleAction
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.BouncerSettings
import com.twitter.auth.policykeeper.thriftscala.BouncerTargetSettings
import com.twitter.auth.policykeeper.thriftscala.DeepStringMap
import com.twitter.auth.policykeeper.thriftscala.Rule
import com.twitter.auth.policykeeper.thriftscala.RuleAction
import scala.reflect.runtime.universe._

private case class PolicyConvertor(logger: JsonLogger) {

  case class GenericType[T](value: T)

  def deepStringMapFromConfig[T: TypeTag](
    jsonDeepStringMap: GenericType[T]
  ): DeepStringMap = {
    jsonDeepStringMap match {
      case GenericType(value: String) =>
        DeepStringMap(isMap = false, stringVal = Some(value), mapVal = None)
      case GenericType(value: Map[String, Any] @unchecked) =>
        DeepStringMap(
          isMap = true,
          stringVal = None,
          mapVal = Some(value.collect {
            case (k, v) => (k, deepStringMapFromConfig(GenericType(value = v)))
          }))
      case _ =>
        logger.info(
          message = "invalid template_mapping value",
          metadata = Some(Map("value" -> jsonDeepStringMap))
        )
        DeepStringMap(false, stringVal = Some(""), mapVal = None)
    }
  }

  def bouncerTargetSettingsFromConfig(
    jsonBouncerTargetSettings: SerializableBouncerTargetSettings
  ): BouncerTargetSettings = {
    BouncerTargetSettings(
      targetType = jsonBouncerTargetSettings.targetType,
      userId = jsonBouncerTargetSettings.userId,
      sessionHash = jsonBouncerTargetSettings.sessionHash,
      feature = jsonBouncerTargetSettings.feature
    )
  }

  def bouncerSettingsFromConfig(
    jsonBouncerSettings: SerializableBouncerSettings
  ): BouncerSettings = {
    BouncerSettings(
      location = jsonBouncerSettings.location,
      errorMessage = jsonBouncerSettings.errorMessage,
      deepLink = jsonBouncerSettings.deepLink,
      experience = jsonBouncerSettings.experience,
      templateIds = jsonBouncerSettings.templateIds,
      templateMapping = jsonBouncerSettings.templateMapping match {
        case Some(map) =>
          Some(map.collect {
            case (k, v) => (k, deepStringMapFromConfig(GenericType(value = v)))
          })
        case None => None
      },
      referringTags = jsonBouncerSettings.referringTags,
      target = bouncerTargetSettingsFromConfig(jsonBouncerSettings.target)
    )
  }

  def ruleActionFromConfig(jsonRuleAction: SerializableRuleAction): RuleAction = {
    RuleAction(
      actionNeeded = jsonRuleAction.actionNeeded,
      apiErrorCode = jsonRuleAction.apiErrorCode,
      bouncerSettings = jsonRuleAction.bouncerSettings match {
        case Some(settings) => Some(bouncerSettingsFromConfig(settings))
        case None => None
      }
    )
  }

  def ruleFromConfig(jsonRule: SerializableRule): Rule = {
    Rule(
      expression = jsonRule.expression,
      action = ruleActionFromConfig(jsonRule.action),
      priority = jsonRule.priority,
      fallbackAction = jsonRule.fallbackAction match {
        case Some(action) => Some(ruleActionFromConfig(action))
        case None => None
      }
    )
  }

  def policyFromConfig(jsonPolicy: SerializablePolicy): Policy = {
    Policy(
      policyId = jsonPolicy.policyId,
      decider = jsonPolicy.decider,
      dataProviders = jsonPolicy.dataProviders,
      eligibilityCriteria = jsonPolicy.eligibilityCriteria,
      rules = jsonPolicy.rules.map(ruleFromConfig),
      name = jsonPolicy.name,
      description = jsonPolicy.description,
      failClosed = jsonPolicy.failClosed
    )
  }

}
