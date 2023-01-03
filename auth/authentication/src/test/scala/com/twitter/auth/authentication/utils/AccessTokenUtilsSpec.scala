package com.twitter.auth.authentication.utils

import com.twitter.util.Time
import org.junit.runner.RunWith
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AccessTokenUtilsSpec extends AnyFunSuite with Matchers {
  import com.twitter.auth.authentication.CommonFixtures._

  test("isInvalid") {
    AccessTokenUtils.isInvalid(ValidOAuth1AccessToken) mustEqual false
    AccessTokenUtils.isInvalid(ValidOAuth1AccessToken.copy(invalidatedAt = Some(0))) mustEqual false
    AccessTokenUtils.isInvalid(
      ValidOAuth1AccessToken.copy(invalidatedAt = Some(Time.now.inSeconds + 10))) mustEqual false
    AccessTokenUtils.isInvalid(
      ValidOAuth1AccessToken.copy(invalidatedAt = Some(Time.now.inSeconds - 10))) mustEqual true

    AccessTokenUtils.isInvalid(ValidOAuth2AppOnlyToken) mustEqual false
    AccessTokenUtils.isInvalid(ValidOAuth2AppOnlyToken.copy(invalidateAt = Some(0))) mustEqual false
    AccessTokenUtils.isInvalid(
      ValidOAuth2AppOnlyToken.copy(invalidateAt = Some(Time.now.inSeconds + 10))) mustEqual false
    AccessTokenUtils.isInvalid(
      ValidOAuth2AppOnlyToken.copy(invalidateAt = Some(Time.now.inSeconds - 10))) mustEqual true

    AccessTokenUtils.isInvalid(ValidOAuth2ClientAccessToken) mustEqual false
    AccessTokenUtils.isInvalid(
      ValidOAuth2ClientAccessToken.copy(invalidateAt = Some(0))) mustEqual false
    AccessTokenUtils.isInvalid(ValidOAuth2ClientAccessToken.copy(invalidateAt =
      Some(Time.now.inSeconds + 10))) mustEqual false
    AccessTokenUtils.isInvalid(ValidOAuth2ClientAccessToken.copy(invalidateAt =
      Some(Time.now.inSeconds - 10))) mustEqual true

    AccessTokenUtils.isInvalid(ValidOAuth2AppOnlyToken) mustEqual false
    AccessTokenUtils.isInvalid(ValidOAuth2AppOnlyToken.copy(invalidateAt = Some(0))) mustEqual false
    AccessTokenUtils.isInvalid(
      ValidOAuth2AppOnlyToken.copy(invalidateAt = Some(Time.now.inSeconds + 10))) mustEqual false
    AccessTokenUtils.isInvalid(
      ValidOAuth2AppOnlyToken.copy(invalidateAt = Some(Time.now.inSeconds - 10))) mustEqual true

    AccessTokenUtils.isInvalid(ValidOAuth2AccessToken) mustEqual false
    AccessTokenUtils.isInvalid(ValidOAuth2AccessToken.copy(expiresAt = 0)) mustEqual false
    AccessTokenUtils.isInvalid(
      ValidOAuth2AccessToken.copy(expiresAt = Time.now.inSeconds + 10)) mustEqual false
    AccessTokenUtils.isInvalid(
      ValidOAuth2AccessToken.copy(expiresAt = Time.now.inSeconds - 10)) mustEqual true
  }

  test("isCreatedPreInvalidationFix") {
    AccessTokenUtils.isCreatedPreInvalidationFix(
      ValidOAuth1AccessToken.copy(createdAt = 0L)) mustEqual true
    AccessTokenUtils.isCreatedPreInvalidationFix(
      ValidOAuth1AccessToken.copy(createdAt = Time.now.inSeconds - 10000L)
    ) mustEqual false

    AccessTokenUtils.isCreatedPreInvalidationFix(
      ValidOAuth2AppOnlyToken.copy(createdAt = 0L)) mustEqual true
    AccessTokenUtils.isCreatedPreInvalidationFix(
      ValidOAuth2AppOnlyToken.copy(createdAt = Time.now.inSeconds - 10000L)
    ) mustEqual false

    AccessTokenUtils.isCreatedPreInvalidationFix(
      ValidOAuth2AccessToken.copy(createdAt = 0L)) mustEqual true
    AccessTokenUtils.isCreatedPreInvalidationFix(
      ValidOAuth2AccessToken.copy(createdAt = Time.now.inSeconds - 10000L)
    ) mustEqual false
  }

}
