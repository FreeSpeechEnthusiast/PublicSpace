package com.twitter.auth.authorizationscope

/*
import com.twitter.auth.authorizationscope.NewScopeToLegacyScopeConverter.{
  DmRead,
  EmailAddress,
  IsWritable
}
import com.twitter.util.Var
import org.mockito.Mockito.when
 */

import org.junit.runner.RunWith

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class NewScopeToLegacyScopeConverterSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers
    with BeforeAndAfter {

  val ReadWriteScope = "read_write_scope"
  val ReadWriteDmScope = "read_write_dm_scope"
  val EmailScope = "email_scope"
  /*
  TODO - FIXME - move this to TFE (at least for basic checks



  val asLegacy_read =
    new AuthorizationScope(0, read_scope, "legacy", "ui text", "internal", List("data-products"))
  //  val asLegacyWrite = AuthorizationScope(FixedAuthorizationScopeRetriever.ReadWriteScope)
  //  val asLegacyDmRead = AuthorizationScope(FixedAuthorizationScopeRetriever.ReadWriteDMScope)
  //  val asLegacyEmail = AuthorizationScope(FixedAuthorizationScopeRetriever.EmailScope)

  val authorizationScopesMap: AuthorizationScopesMap = AuthorizationScopesMap(
    List(asLegacy_read)
  )
  private var newScopeToLegacyScopeConverterf: NewScopeToLegacyScopeConverter = _

  before {
    val mockScopesMap = mock[Var[AuthorizationScopesMap]]
    when(mockScopesMap.sample()) thenReturn authorizationScopesMap
    before {
      newScopeToLegacyScopeConverterf = new NewScopeToLegacyScopeConverter(mockScopesMap)
    }
  }

  test("test authorization scope to legacy scope mappings") {
    newScopeToLegacyScopeConverterf.convertNewScopesToLegacyScopes(Set("dummy")) mustBe Set()
    NewScopeToLegacyScopeConverter.convertNewScopesToLegacyScopes(Set()) mustBe Set()
    NewScopeToLegacyScopeConverter.convertNewScopesToLegacyScopes(Set(ReadWriteScope)) mustBe Set(
      IsWritable)
    NewScopeToLegacyScopeConverter
      .convertNewScopesToLegacyScopes(Set(ReadWriteDmScope)) mustBe Set(IsWritable, DmRead)
    NewScopeToLegacyScopeConverter.convertNewScopesToLegacyScopes(Set(EmailScope)) mustBe Set(
      EmailAddress)
    NewScopeToLegacyScopeConverter.convertNewScopesToLegacyScopes(
      Set(ReadWriteScope, ReadWriteDmScope)) mustBe Set(IsWritable, DmRead)
    NewScopeToLegacyScopeConverter.convertNewScopesToLegacyScopes(
      Set(ReadWriteScope, ReadWriteDmScope, EmailScope)) mustBe Set(
      IsWritable,
      DmRead,
      EmailAddress)
  }*/
}
