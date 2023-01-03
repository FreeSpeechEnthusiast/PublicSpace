package com.twitter.auth.pasetoheaders.s2s.models

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PrincipalsTest extends AnyFunSuite with OneInstancePerTest with Matchers with BeforeAndAfter {

  /**
   * Create mapper with the same properties as in ClaimMapping
   */
  private def jsonMapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    mapper
  }

  val ldapUser: Principals.LdapUser = Principals.LdapUser("twitterUser")
  val ldapGroup: Principals.LdapGroup = Principals.LdapGroup("twitterGroup")
  val botMakerRule: Principals.BotMakerRule = Principals.BotMakerRule("rule")
  val stratoColumn: Principals.StratoColumn = Principals.StratoColumn("column")
  val serviceIdentifier: Principals.ServiceIdentifier =
    Principals.ServiceIdentifier("twtr:svc:role:service:env:dc")

  val ldapUserSerialized = "{\"prl\":\"usr\",\"nam\":\"twitterUser\"}"
  val ldapGroupSerialized = "{\"prl\":\"grp\",\"nam\":\"twitterGroup\"}"
  val botMakerRuleSerialized = "{\"prl\":\"brl\",\"nam\":\"rule\"}"
  val stratoColumnSerialized = "{\"prl\":\"scl\",\"nam\":\"column\"}"
  val serviceIdentifierSerialized = "{\"prl\":\"sid\",\"sid\":\"twtr:svc:role:service:env:dc\"}"

  test("test ldapUser principal serialization") {
    val result = jsonMapper.writeValueAsString(ldapUser)
    assertEquals(ldapUserSerialized, result)
  }

  test("test ldapGroup principal serialization") {
    val result = jsonMapper.writeValueAsString(ldapGroup)
    assertEquals(ldapGroupSerialized, result)
  }

  test("test botMakerRule principal serialization") {
    val result = jsonMapper.writeValueAsString(botMakerRule)
    assertEquals(botMakerRuleSerialized, result)
  }

  test("test stratoColumn principal serialization") {
    val result = jsonMapper.writeValueAsString(stratoColumn)
    assertEquals(stratoColumnSerialized, result)
  }

  test("test serviceIdentifier principal serialization") {
    val result = jsonMapper.writeValueAsString(serviceIdentifier)
    assertEquals(serviceIdentifierSerialized, result)
  }

  test("test ldapUser principal deserialization") {
    val result = jsonMapper.readValue(ldapUserSerialized, classOf[Principals.Principal])
    assertEquals(ldapUser, result)
  }

  test("test ldapGroup principal deserialization") {
    val result = jsonMapper.readValue(ldapGroupSerialized, classOf[Principals.Principal])
    assertEquals(ldapGroup, result)
  }

  test("test botMakerRule principal deserialization") {
    val result = jsonMapper.readValue(botMakerRuleSerialized, classOf[Principals.Principal])
    assertEquals(botMakerRule, result)
  }

  test("test stratoColumn principal deserialization") {
    val result = jsonMapper.readValue(stratoColumnSerialized, classOf[Principals.Principal])
    assertEquals(stratoColumn, result)
  }

  test("test serviceIdentifier principal deserialization") {
    val result = jsonMapper.readValue(serviceIdentifierSerialized, classOf[Principals.Principal])
    assertEquals(serviceIdentifier, result)
  }

}
