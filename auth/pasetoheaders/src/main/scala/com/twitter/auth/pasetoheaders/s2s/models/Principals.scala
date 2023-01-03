package com.twitter.auth.pasetoheaders.s2s.models

import JacksonAnnotations._
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

object Principals {
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "prl")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[LdapUser], name = "usr"),
      new JsonSubTypes.Type(value = classOf[LdapGroup], name = "grp"),
      new JsonSubTypes.Type(value = classOf[BotMakerRule], name = "brl"),
      new JsonSubTypes.Type(value = classOf[ServiceIdentifier], name = "sid"),
      new JsonSubTypes.Type(value = classOf[StratoColumn], name = "scl")
    ))
  abstract class Principal()

  @JsonTypeName("brl")
  final case class BotMakerRule @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) (
    @JsonProperty("nam") name: String)
      extends Principal

  @JsonTypeName("grp")
  final case class LdapGroup @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) (
    @JsonProperty("nam") name: String)
      extends Principal

  @JsonTypeName("usr")
  final case class LdapUser @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) (
    @JsonProperty("nam") name: String)
      extends Principal

  @JsonTypeName("sid")
  final case class ServiceIdentifier @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) (
    @JsonProperty("sid") serviceIdentifier: String)
      extends Principal

  @JsonTypeName("scl")
  final case class StratoColumn @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) (
    @JsonProperty("nam") name: String)
      extends Principal
}
