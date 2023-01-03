package com.twitter.auth.policy

import com.twitter.auth.authorizationscope.AuthorizationScopeConstants._
import com.twitter.auth.authorizationscope.{AuthorizationScope, AuthorizationScopesMap}
import com.twitter.util.Var
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class DynamicAuthorizationScopesPolicySpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers
    with BeforeAndAfter {

  private val SessionAuthScopeSet =
    Set[String](ReadScopeName, ReadWriteScopeName, ReadWriteDMScopeName, EmailScopeName)
  private val DeviceAuthScopeSet = Set[String](ReadScopeName, ReadWriteScopeName)
  private val GuestAuthScopeSet = Set[String](ReadScopeName)
  private val AppOnlyScopeSet = Set[String](ReadScopeName)
  private val TiaScopeSet = Set[String](ReadScopeName, ReadWriteScopeName)

  private val ReadOnlyAds = Set[String](AdsRead)
  private val ReadOnlyDm = Set[String](DmRead)
  private val ReadOnlyEmail = Set[String](EmailAddress)
  private val ReadOnlyAdsDm = Set[String](AdsRead, DmRead)
  private val ReadOnlyAdsEmail = Set[String](AdsRead, EmailAddress)
  private val ReadOnlyDmEmail = Set[String](DmRead, EmailAddress)
  private val ReadOnlyAdsDmEmail = Set[String](AdsRead, DmRead, EmailAddress)

  private val Writable = Set[String](IsWritable)
  private val WritableAds = Set[String](IsWritable, AdsRead)
  private val WritableDm = Set[String](IsWritable, DmRead)
  private val WritableEmail = Set[String](IsWritable, EmailAddress)
  private val WritableAdsDm = Set[String](IsWritable, AdsRead, DmRead)
  private val WritableAdsEmail = Set[String](IsWritable, AdsRead, EmailAddress)
  private val WritableDmEmail = Set[String](IsWritable, DmRead, EmailAddress)
  private val WritableAdsDmEmail = Set[String](IsWritable, AdsRead, DmRead, EmailAddress)

  private val SessionScope = Set[String](Session)
  private val DeviceScope = Set[String](DeviceAuth)
  private val GuestScope = Set[String](GuestAuth)
  private val TiaScope = Set[String](Tia)
  private val AppOnlyScope = Set[String](AppOnly)

  private var dynamicAuthZScopesPolicy: DynamicAuthorizationScopesPolicy = _

  val read_scope = "read_scope"
  val Read_Scope = "Read_Scope"

  val ReadWriteScope = "read_write_scope"
  val readwritescope = "Read_Write_Scope"

  val ReadWriteDmScope = "read_write_dm_scope"
  val readwritedmscope = "Read_Write_Dm_Scope"

  val EmailScope = "email_scope"
  val emailscope = "Email_Scope"

  val authorizationscope1 = "TestAuthorizationScope1"
  val authorizationscope2 = "TestAuthorizationScope2"

  val tweet_read_scope = "tweet.read"
  val users_read_scope = "users.read"

  val TweetRead = Set(tweet_read_scope)
  val UsersRead = Set(users_read_scope)

  val asLegacy_read =
    AuthorizationScope(0, read_scope, "legacy", None, None, "production", Set("data-products"))

  val tweet_read =
    AuthorizationScope(1, tweet_read_scope, "vnext", None, None, "production", Set("data-products"))

  val users_read =
    AuthorizationScope(2, users_read_scope, "vnext", None, None, "production", Set("data-products"))

  val authorizationScopesMap: AuthorizationScopesMap = AuthorizationScopesMap(
    List(asLegacy_read, tweet_read, users_read)
  )

  val mockScopesMap = mock[Var[AuthorizationScopesMap]]
  when(mockScopesMap.sample()) thenReturn authorizationScopesMap

  before {
    dynamicAuthZScopesPolicy = new DynamicAuthorizationScopesPolicy(mockScopesMap)
  }

  test("test mapping for fine-grained scope - tweet.read") {
    // fine-grained scope only
    dynamicAuthZScopesPolicy.authorizationScopes(Set(tweet_read_scope)) mustBe TweetRead
    dynamicAuthZScopesPolicy.authorizationScopes(
      Set(tweet_read_scope, users_read_scope)) mustBe TweetRead.union(UsersRead)
    dynamicAuthZScopesPolicy.authorizationScopes(
      Set(tweet_read_scope, users_read_scope, "dummy")) mustBe TweetRead.union(UsersRead)
    // if there's any fine-grained scope provided, ignore other legacy scopes
    dynamicAuthZScopesPolicy.authorizationScopes(
      Set(tweet_read_scope).union(ReadOnlyAds)) mustBe TweetRead
    dynamicAuthZScopesPolicy.authorizationScopes(
      Set(tweet_read_scope).union(ReadOnlyDm)) mustBe TweetRead
    dynamicAuthZScopesPolicy.authorizationScopes(
      Set(tweet_read_scope).union(ReadOnlyEmail)) mustBe TweetRead
    dynamicAuthZScopesPolicy.authorizationScopes(
      Set(tweet_read_scope).union(ReadOnlyAdsDm)) mustBe TweetRead
    dynamicAuthZScopesPolicy.authorizationScopes(
      Set(tweet_read_scope).union(ReadOnlyAdsEmail)) mustBe TweetRead
    dynamicAuthZScopesPolicy.authorizationScopes(
      Set(tweet_read_scope).union(ReadOnlyDmEmail)) mustBe TweetRead
    dynamicAuthZScopesPolicy
      .authorizationScopes(Set(tweet_read_scope).union(ReadOnlyAdsDmEmail)) mustBe TweetRead
  }

  test("test mapping between token privileges and authorization scopes") {
    dynamicAuthZScopesPolicy
      .authorizationScopes(ReadOnlyAds) mustBe Set(AdsReadScopeName, ReadScopeName)
    dynamicAuthZScopesPolicy.authorizationScopes(ReadOnlyDm) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      ReadWriteDMScopeName)
    dynamicAuthZScopesPolicy
      .authorizationScopes(ReadOnlyEmail) mustBe Set(EmailScopeName, ReadScopeName)
    dynamicAuthZScopesPolicy.authorizationScopes(ReadOnlyAdsDm) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      ReadWriteDMScopeName,
      AdsReadScopeName)
    dynamicAuthZScopesPolicy
      .authorizationScopes(ReadOnlyAdsEmail) mustBe Set(
      AdsReadScopeName,
      EmailScopeName,
      ReadScopeName)
    dynamicAuthZScopesPolicy.authorizationScopes(ReadOnlyDmEmail) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      ReadWriteDMScopeName,
      EmailScopeName)
    dynamicAuthZScopesPolicy
      .authorizationScopes(ReadOnlyAdsDmEmail) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      ReadWriteDMScopeName,
      AdsReadScopeName,
      EmailScopeName)
    dynamicAuthZScopesPolicy
      .authorizationScopes(Writable) mustBe Set(ReadScopeName, ReadWriteScopeName)
    dynamicAuthZScopesPolicy.authorizationScopes(WritableAds) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      AdsReadScopeName)
    dynamicAuthZScopesPolicy.authorizationScopes(WritableDm) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      ReadWriteDMScopeName)
    dynamicAuthZScopesPolicy.authorizationScopes(WritableEmail) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      EmailScopeName)
    dynamicAuthZScopesPolicy.authorizationScopes(WritableAdsDm) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      ReadWriteDMScopeName,
      AdsReadScopeName)
    dynamicAuthZScopesPolicy.authorizationScopes(WritableAdsEmail) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      AdsReadScopeName,
      EmailScopeName)
    dynamicAuthZScopesPolicy.authorizationScopes(WritableDmEmail) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      ReadWriteDMScopeName,
      EmailScopeName)
    dynamicAuthZScopesPolicy.authorizationScopes(WritableAdsDmEmail) mustBe Set(
      ReadScopeName,
      ReadWriteScopeName,
      ReadWriteDMScopeName,
      AdsReadScopeName,
      EmailScopeName)
  }

  test("test mapping between token privileges and authorization scope - session") {
    dynamicAuthZScopesPolicy.authorizationScopes(SessionScope) mustBe SessionAuthScopeSet

  }

  test("test mapping between token privileges and authorization scope - device") {
    dynamicAuthZScopesPolicy.authorizationScopes(DeviceScope) mustBe DeviceAuthScopeSet
  }

  test("test mapping between token privileges and authorization scope - guest") {
    dynamicAuthZScopesPolicy.authorizationScopes(GuestScope) mustBe GuestAuthScopeSet
  }

  test("test mapping between token privileges and authorization scope - appOnly") {
    dynamicAuthZScopesPolicy.authorizationScopes(AppOnlyScope) mustBe AppOnlyScopeSet
  }

  test("test mapping between token privileges and authorization scope - Tia") {
    dynamicAuthZScopesPolicy.authorizationScopes(TiaScope) mustBe TiaScopeSet
  }
}
