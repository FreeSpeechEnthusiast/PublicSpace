package com.twitter.auth.policykeeper.api.storage.configbus.parser

import com.fasterxml.jackson.core.JsonParser.Feature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.configbus.subscriber.JsonConfigParser
import com.twitter.io.Buf

/**
 * Declares YAML file data format, for example:
 *  policies:
 *  - policy_id: policy1
 *    decider:
 *    data_providers:
 *    eligibility_criteria:
 *    fail_closed: false
 *    rules:
 *      - expression: exp
 *        action:
 *          action_needed: false
 *          api_error_code:
 *          bouncer_settings:
 *            target:
 *              target_type: session
 *              user_id: "{{gizmoduck_user.id}}"
 *              session_hash: "{{auth.sessionHash}}"
 *              feature: auth_challenge
 *            location: "/account/access?feature=auth_challenge&session={{auth.sessionHash}}"
 *            experience: FullOptional
 *            template_ids:
 *              - module_tsla
 *            template_mapping:
 *              tslaAuthChallengeData:
 *                token: "{{access_token.token}}"
 *                tokenKind: "{{access_token.tokenKind}}"
 *            referring_tags:
 *              - TSLA
 *              - MODULE
 *        priority: 0
 *    name: testPolicy
 *    description: ""
 */
case class SerializableBouncerTargetSettings(
  targetType: String,
  userId: Option[String],
  sessionHash: Option[String],
  feature: Option[String],
)

case class SerializableBouncerSettings(
  location: Option[String],
  errorMessage: Option[String],
  deepLink: Option[String],
  experience: Option[String],
  templateIds: Seq[String],
  templateMapping: Option[Map[String, Any]],
  referringTags: Option[Set[String]],
  target: SerializableBouncerTargetSettings,
)

case class SerializableRuleAction(
  actionNeeded: Boolean,
  apiErrorCode: Option[Int],
  bouncerSettings: Option[SerializableBouncerSettings])

case class SerializableRule(
  expression: String,
  action: SerializableRuleAction,
  priority: Long,
  fallbackAction: Option[SerializableRuleAction])

case class SerializablePolicy(
  policyId: String,
  decider: Option[String],
  dataProviders: Option[Set[String]],
  eligibilityCriteria: Option[Set[String]],
  rules: Set[SerializableRule],
  name: String,
  description: String,
  failClosed: Option[Boolean] = Some(false)) /* A policy is fail-open by default */

case class PolicyValidationException(message: String) extends Exception(message)

case class PolicyDatabaseFile(policies: Set[SerializablePolicy] = Set()) {
  private val policyIdPattern = PolicyId.Pattern.r

  def withValidation(): PolicyDatabaseFile = {
    policies.foreach { p =>
      p.policyId match {
        case policyIdPattern(_) =>
        case _ =>
          throw PolicyValidationException(
            "PolicyId `" + p.policyId + "` is invalid. " + PolicyId.Pattern + " value expected")
      }
      p.rules.groupBy(_.priority).filter(g => g._2.size > 1).foreach { r =>
        throw PolicyValidationException(
          "Non-unique rule priority " + r._1 + " in PolicyId `" + p.policyId + "`")
      }
    }
    policies.groupBy(_.policyId).filter(g => g._2.size > 1).foreach { p =>
      throw PolicyValidationException("Non-unique PolicyId `" + p._1 + "`")
    }
    //TODO: Bouncer settings validation
    this
  }
}

object PolicyDatabaseFile {
  val EMPTY: PolicyDatabaseFile = PolicyDatabaseFile(Set.empty)
}

// PolicyDatabaseParser is extending JsonConfigParser from com.twitter.configbus.subscriber
// the only difference is YAMLFactory injected to JsonObjectMapper
object PolicyDatabaseParser extends JsonConfigParser[PolicyDatabaseFile] {
  override def load(
    buf: Buf
  )(
    implicit manifest: scala.Predef.Manifest[PolicyDatabaseFile]
  ): PolicyDatabaseFile = {
    buf match {
      case Buf.Utf8(str) =>
        JsonObjectMapper().readValue[PolicyDatabaseFile](str)(manifest)
    }
  }

  object JsonObjectMapper {
    private lazy val instance = {
      val mapper = new ObjectMapper(new YAMLFactory()) with ScalaObjectMapper
      mapper.setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy)
      mapper.registerModule(DefaultScalaModule)
      mapper.configure(Feature.ALLOW_COMMENTS, true)
      mapper
    }

    def apply(): ScalaObjectMapper = instance
  }
}
