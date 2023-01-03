package com.twitter.auth.authentication.utils

import com.twitter.auth.authentication.models.ActAsUserParams
import com.twitter.auth.authentication.models.AuthRequest
import com.twitter.finatra.tfe.HttpHeaderNames
import com.twitter.util.Base64Long
import org.junit.runner.RunWith
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ActAsUserUtilsSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers {

  import com.twitter.auth.authentication.utils.ActAsUserUtils._

  val ACT_AS_USER_ID = 11L
  val CONTRIBUTOR_USER_ID = 22L
  val authNRequestEmpty = new AuthRequest(headerParams = Map.empty, url = Some("test"))
  val authNRequestDummy = new AuthRequest(headerParams = Map("dummy" -> "foo"), url = Some("test"))
  val authNRequestBadCookie = new AuthRequest(
    headerParams = Map.empty,
    cookies = Some(Map(TEAMS_ACT_AS_USER_ID_COOKIE -> "%%%%")),
    url = Some("test")
  )
  val authNRequestBadHeader = new AuthRequest(
    headerParams = Map(HttpHeaderNames.X_ACT_AS_USER_ID -> "aaa"),
    url = Some("test")
  )
  val authNRequestContributeToUserIdHeader = new AuthRequest(
    headerParams = Map(HttpHeaderNames.X_CONTRIBUTE_TO_USER_ID -> CONTRIBUTOR_USER_ID.toString),
    url = Some("test")
  )
  val authNRequestActAsUserIdHeader = new AuthRequest(
    headerParams = Map(HttpHeaderNames.X_ACT_AS_USER_ID -> ACT_AS_USER_ID.toString),
    url = Some("test")
  )
  val authNRequestActAsUserIdCookie = new AuthRequest(
    headerParams = Map.empty,
    cookies = Some(Map(TEAMS_ACT_AS_USER_ID_COOKIE -> Base64Long.toBase64(ACT_AS_USER_ID))),
    url = Some("test")
  )
  val authNRequestTwoDifferentHeaders = new AuthRequest(
    headerParams = Map(
      HttpHeaderNames.X_CONTRIBUTE_TO_USER_ID -> CONTRIBUTOR_USER_ID.toString,
      HttpHeaderNames.X_ACT_AS_USER_ID -> ACT_AS_USER_ID.toString),
    url = Some("test")
  )
  val authNRequestTwoSameHeaders = new AuthRequest(
    headerParams = Map(
      HttpHeaderNames.X_CONTRIBUTE_TO_USER_ID -> ACT_AS_USER_ID.toString,
      HttpHeaderNames.X_ACT_AS_USER_ID -> ACT_AS_USER_ID.toString),
    url = Some("test")
  )

  val authNRequestTwoHeadersOneCookie = new AuthRequest(
    headerParams = Map(
      HttpHeaderNames.X_CONTRIBUTE_TO_USER_ID -> ACT_AS_USER_ID.toString,
      HttpHeaderNames.X_ACT_AS_USER_ID -> ACT_AS_USER_ID.toString),
    cookies = Some(Map(TEAMS_ACT_AS_USER_ID_COOKIE -> Base64Long.toBase64(ACT_AS_USER_ID))),
    url = Some("test")
  )
  val authNRequestOneHeadersOneCookie = new AuthRequest(
    headerParams = Map(HttpHeaderNames.X_CONTRIBUTE_TO_USER_ID -> CONTRIBUTOR_USER_ID.toString),
    cookies = Some(Map(TEAMS_ACT_AS_USER_ID_COOKIE -> Base64Long.toBase64(CONTRIBUTOR_USER_ID))),
    url = Some("test")
  )
  val authNRequestOneHeadersOneCookie2 = new AuthRequest(
    headerParams = Map(HttpHeaderNames.X_ACT_AS_USER_ID -> ACT_AS_USER_ID.toString),
    cookies = Some(Map(TEAMS_ACT_AS_USER_ID_COOKIE -> Base64Long.toBase64(ACT_AS_USER_ID))),
    url = Some("test")
  )
  val authNRequestDifferentHeaderCookie = new AuthRequest(
    headerParams = Map(HttpHeaderNames.X_ACT_AS_USER_ID -> ACT_AS_USER_ID.toString),
    Some(Map(TEAMS_ACT_AS_USER_ID_COOKIE -> Base64Long.toBase64(CONTRIBUTOR_USER_ID))),
    url = Some("test")
  )

  val authNRequestWithSteamDelegationHeader = new AuthRequest(
    headerParams = Map(
      HttpHeaderNames.X_ACT_AS_USER_ID -> ACT_AS_USER_ID.toString,
      HttpHeaderNames.X_CONTRIBUTOR_VERSION -> "1"),
    url = Some("test")
  )

  val authNRequestWithSteamDelegationWithoutActAsUserIdHeader = new AuthRequest(
    headerParams = Map(HttpHeaderNames.X_CONTRIBUTOR_VERSION -> "1"),
    url = Some("test")
  )

  test("test isActAsUserRequest") {
    isActAsUserRequest(authNRequestEmpty) mustBe false

    isActAsUserRequest(authNRequestDummy) mustBe false

    isActAsUserRequest(authNRequestContributeToUserIdHeader) mustBe true

    isActAsUserRequest(authNRequestActAsUserIdHeader) mustBe true

    isActAsUserRequest(authNRequestActAsUserIdCookie) mustBe true
  }

  test("test isSTEAMDelegatedRequest") {
    isSTEAMDelegatedRequest(authNRequestWithSteamDelegationHeader) mustBe true

    isSTEAMDelegatedRequest(authNRequestWithSteamDelegationWithoutActAsUserIdHeader) mustBe false

    isSTEAMDelegatedRequest(authNRequestDummy) mustBe false
  }

  test("test createActAsUserParams") {
    createActAsUserParams(authNRequestEmpty) mustBe None

    createActAsUserParams(authNRequestDummy) mustBe None

    createActAsUserParams(authNRequestBadCookie) mustBe None

    createActAsUserParams(authNRequestBadHeader) mustBe None

    createActAsUserParams(authNRequestContributeToUserIdHeader) mustBe Some(
      ActAsUserParams(None, Some(CONTRIBUTOR_USER_ID)))

    createActAsUserParams(authNRequestActAsUserIdHeader) mustBe Some(
      ActAsUserParams(None, Some(ACT_AS_USER_ID)))

    createActAsUserParams(authNRequestActAsUserIdCookie) mustBe Some(
      ActAsUserParams(Some(ACT_AS_USER_ID), None))

    createActAsUserParams(authNRequestTwoDifferentHeaders) mustBe Some(
      ActAsUserParams(None, Some(ACT_AS_USER_ID)))

    createActAsUserParams(authNRequestTwoSameHeaders) mustBe Some(
      ActAsUserParams(None, Some(ACT_AS_USER_ID)))

    createActAsUserParams(authNRequestTwoHeadersOneCookie) mustBe Some(
      ActAsUserParams(Some(ACT_AS_USER_ID), Some(ACT_AS_USER_ID)))

    createActAsUserParams(authNRequestOneHeadersOneCookie) mustBe Some(
      ActAsUserParams(Some(CONTRIBUTOR_USER_ID), Some(CONTRIBUTOR_USER_ID)))

    createActAsUserParams(authNRequestOneHeadersOneCookie2) mustBe Some(
      ActAsUserParams(Some(ACT_AS_USER_ID), Some(ACT_AS_USER_ID)))

    createActAsUserParams(authNRequestDifferentHeaderCookie) mustBe None
  }

  test("test hasUnexpectedActAsUserParams") {
    // input is none
    hasUnexpectedActAsUserParams(None) mustBe false
    // user id from header is none
    hasUnexpectedActAsUserParams(Some(ActAsUserParams(Some(ACT_AS_USER_ID), None))) mustBe true
    // otherwise
    hasUnexpectedActAsUserParams(
      Some(ActAsUserParams(Some(CONTRIBUTOR_USER_ID), Some(ACT_AS_USER_ID)))) mustBe false
  }

  test("test getOAuthActAsUserId") {
    // with no input params
    getOAuthActAsUserId(None) mustBe None
    // user id from header is none
    getOAuthActAsUserId(Some(ActAsUserParams(Some(ACT_AS_USER_ID), None))) mustBe None
    // otherwise
    getOAuthActAsUserId(
      Some(ActAsUserParams(Some(CONTRIBUTOR_USER_ID), Some(ACT_AS_USER_ID)))) mustBe Some(
      ACT_AS_USER_ID)
  }

  test("test getSessionActAsUserId") {
    // with no input params
    getSessionActAsUserId(None) mustBe None
    // user id from header only
    getSessionActAsUserId(Some(ActAsUserParams(None, Some(CONTRIBUTOR_USER_ID)))) mustBe Some(
      CONTRIBUTOR_USER_ID)
    // user id from cookie only
    getSessionActAsUserId(Some(ActAsUserParams(Some(ACT_AS_USER_ID), None))) mustBe Some(
      ACT_AS_USER_ID)
    // header == cookie
    getSessionActAsUserId(
      Some(ActAsUserParams(Some(ACT_AS_USER_ID), Some(ACT_AS_USER_ID)))) mustBe Some(ACT_AS_USER_ID)
    // header != cookie => header
    getSessionActAsUserId(
      Some(ActAsUserParams(Some(CONTRIBUTOR_USER_ID), Some(ACT_AS_USER_ID)))) mustBe Some(
      ACT_AS_USER_ID)
  }
}
