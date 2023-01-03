package com.twitter.auth.authentication.utils

import com.twitter.auth.authentication.models.ActAsUserParams
import com.twitter.auth.authentication.models.AuthRequest
import com.twitter.finagle.stats.DefaultStatsReceiver
import com.twitter.finatra.tfe.HttpHeaderNames
import com.twitter.util.Base64Long
import com.twitter.util.Try

/**
 * Provides utils for parsing and processing Act-As/Contributor/Team headers and cookies.
 */
object ActAsUserUtils {

  private[this] lazy val statsReceiver = DefaultStatsReceiver.scope("act_as_user_handler")
  private[this] val invalidCookieCounter = statsReceiver.counter("invalid_cookie")
  private[this] val invalidHeaderCounter = statsReceiver.counter("invalid_header")
  private[this] val legacyHeaderCounter = statsReceiver.counter("legacy_header")
  private[this] val missingHeaderCounter = statsReceiver.counter("missing_header")

  // splitter for separating the userId and expiration time part from aa_u cookie
  private[auth] val TeamsActAsUserIdCookieValueSplitter = "#"

  /**
   * The name of the cookie used to store the encoded information of the
   * userId who the authenticated user is contributing to and the expiration time
   * of this contribution. The cookie is created as a session cookie
   * by macaw-login's authenticate_web_view endpoint. Details in go/team-webview.
   */
  val TEAMS_ACT_AS_USER_ID_COOKIE = "aa_u"

  val STEAM_DELEGATION_CONTRIBUTOR_VERSION = "1"

  def isActAsUserRequest(request: AuthRequest): Boolean =
    request.headerMap.contains(
      HttpHeaderNames.X_CONTRIBUTE_TO_USER_ID) || // TODO: this is a field being deprecated
      request.headerMap.contains(HttpHeaderNames.X_ACT_AS_USER_ID) ||
      request.cookies.exists(_.contains(TEAMS_ACT_AS_USER_ID_COOKIE))

  def isSTEAMDelegatedRequest(request: AuthRequest): Boolean =
    isActAsUserRequest(request) && request.headerMap
      .getOrElse(HttpHeaderNames.X_CONTRIBUTOR_VERSION, "") == STEAM_DELEGATION_CONTRIBUTOR_VERSION

  /**
   * When both actAsUserId and aa_u cookie present, they must match so that
   * We know the request is playing on behalf the same contributee
   * Specifically, if the cookie exists and is mal-formed to parse a Long from it,
   * it will fail the validation here as well
   */
  def createActAsUserParams(request: AuthRequest): Option[ActAsUserParams] = {
    val optContributeToUserIdHeader =
      request.headerMap.get(
        HttpHeaderNames.X_CONTRIBUTE_TO_USER_ID
      ) // TODO: this is a field being deprecated
    val optActAsUserIdHeader = request.headerMap.get(HttpHeaderNames.X_ACT_AS_USER_ID)
    // parse user id from header
    val optUserIdHeader: Option[Long] = (
      (optContributeToUserIdHeader, optActAsUserIdHeader) match {
        case (_, Some(userId)) =>
          Some(userId)
        case (Some(legacy), None) =>
          legacyHeaderCounter.incr()
          Some(legacy)
        case _ => None
      }
    ).flatMap(parseUserIdFromHeader)
    // parse user id from cookie
    val optUserIdCookie: Option[Long] = request.cookies
      .flatMap(_.get(TEAMS_ACT_AS_USER_ID_COOKIE))
      .flatMap(parseUserIdFromActAsUserIdCookie)

    (optUserIdCookie, optUserIdHeader) match {
      // when both the header and cookie are present, the cookie value must be in valid format and match the header's value
      case (Some(userIdCookie), Some(userIdHeader)) if userIdCookie == userIdHeader =>
        Some(
          ActAsUserParams(
            actAsUserIdCookie = Some(userIdCookie),
            actAsUserIdHeader = Some(userIdHeader),
            isSTEAMDelegatedRequest = isSTEAMDelegatedRequest(request)))
      // when only the cookie is present, the cookie value must be in valid format
      case (Some(userIdCookie), None) =>
        Some(
          ActAsUserParams(
            actAsUserIdCookie = Some(userIdCookie),
            actAsUserIdHeader = None,
            isSTEAMDelegatedRequest = isSTEAMDelegatedRequest(request)))
      // when only the header is present
      case (None, Some(userIdHeader)) =>
        Some(
          ActAsUserParams(
            actAsUserIdCookie = None,
            actAsUserIdHeader = Some(userIdHeader),
            isSTEAMDelegatedRequest = isSTEAMDelegatedRequest(request)))
      case _ =>
        None
    }
  }

  /**
   * For OAuth requests, actAsUserId should always respect the Header,
   * and aa_u cookie should not be present if the header is empty
   *
   * This function allows you to check exactly that
   */
  def hasUnexpectedActAsUserParams(actAsUserParamsOpt: Option[ActAsUserParams]): Boolean = {
    actAsUserParamsOpt match {
      case Some(params: ActAsUserParams) =>
        (params.actAsUserIdHeader, params.actAsUserIdCookie) match {
          case (None, Some(_)) =>
            missingHeaderCounter.incr()
            true
          case _ =>
            false
        }
      case _ => false
    }
  }

  def getOAuthActAsUserId(paramsOpt: Option[ActAsUserParams]): Option[Long] = {
    paramsOpt match {
      case Some(params) => params.actAsUserIdHeader
      case _ => None
    }
  }

  def getSessionActAsUserId(paramsOpt: Option[ActAsUserParams]): Option[Long] = {
    paramsOpt match {
      case Some(params) => params.actAsUserIdHeader.orElse(params.actAsUserIdCookie)
      case _ => None
    }
  }

  def getIsSTEAMDelegatedRequest(paramsOpt: Option[ActAsUserParams]): Boolean = {
    paramsOpt.exists(_.isSTEAMDelegatedRequest)
  }

  /**
   * Parse act-as user id from Cookie string
   */
  private[this] def parseUserIdFromActAsUserIdCookie(cookieString: String): Option[Long] = {
    // None means the cookie's value is in bad format for aa_u
    cookieString.split(TeamsActAsUserIdCookieValueSplitter).headOption flatMap { userIdEncoded =>
      Try(Some(Base64Long.fromBase64(userIdEncoded))).getOrElse {
        invalidCookieCounter.incr()
        None
      }
    }
  }

  /**
   * Parse act-as user id from Header string
   */
  private[this] def parseUserIdFromHeader(header: String): Option[Long] = {
    Try(Some(header.toLong)).getOrElse {
      invalidHeaderCounter.incr()
      None
    }
  }
}
