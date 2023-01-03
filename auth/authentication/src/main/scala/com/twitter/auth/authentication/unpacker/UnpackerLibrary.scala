package com.twitter.auth.authentication.unpacker

import com.google.common.primitives.UnsignedLongs
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode
import com.twitter.common.ip_address_utils.ClientIpAddressUtils
import com.twitter.finagle.http.{Fields, HeaderMap}
import com.twitter.finagle.stats.{Counter, StatsReceiver}
import com.twitter.finatra.tfe.HttpHeaderNames
import com.twitter.finatra.tfe.HttpHeaderNames.{X_TFE_TRANSACTION_ID, X_TWITTER_AUTH_TYPE}
import com.twitter.logging.Logger
import com.twitter.auth.authentication.models.{AuthRequest, BadRequestParams}
import com.twitter.joauth.OAuthParams.OAuthParamsBuilder
import com.twitter.joauth.Request.Pair
import scala.collection.{Map, mutable}

object UnpackerLibrary {

  private[auth] val TEAMS_ACT_AS_USER_ID_COOKIE = "aa_u"
  private[this] val log = Logger(getClass.getName)

  def actAsUserIdVerified(request: AuthRequest): Boolean = {
    val headerActAsUserId = getHeaderActAsUserId(request)
    val cookieActAsUserId = getCookieTeamsActAsUserId(request)
    if (headerActAsUserId.isEmpty && cookieActAsUserId.isDefined)
      false
    else
      true
  }

  def getHeaderActAsUserId(request: AuthRequest): Option[String] = {
    if (request.headerMap.getOrElse(HttpHeaderNames.X_ACT_AS_USER_ID, "").nonEmpty)
      request.headerMap.get(HttpHeaderNames.X_ACT_AS_USER_ID)
    else
      None
  }

  def getCookieTeamsActAsUserId(request: AuthRequest): Option[String] = {
    if (request.cookies.exists(_.contains(TEAMS_ACT_AS_USER_ID_COOKIE)))
      Some(request.cookies.get(TEAMS_ACT_AS_USER_ID_COOKIE))
    else
      None
  }

  val Unknown = "unknown"
  def createPassportId(headerMap: HeaderMap): String = {
    headerMap.getOrElse(X_TFE_TRANSACTION_ID, Unknown)
  }

  def createBadRequestParams(
    headerMap: HeaderMap,
    authResultCode: AuthResultCode,
    counter: Counter
  ): BadRequestParams = {
    counter.incr()
    BadRequestParams(createPassportId(headerMap), authResultCode)
  }

  def isXTwitterAuthTypeOAuth2Session(
    headerMap: HeaderMap,
    authTokenFromHeader: String,
    authTokenFromCookie: Option[String],
    shouldAllowOAuth2SessionWithoutAuthTypeHeader: Boolean,
    statsReceiver: StatsReceiver
  ): AuthResultCode = {

    val isViableOAuth2SessionRequest = authTokenFromHeader != null &&
      authTokenFromHeader.nonEmpty && authTokenFromCookie.isDefined

    headerMap.get(X_TWITTER_AUTH_TYPE).map(_.toLowerCase) match {

      // explicit definition of OAuth2Session means we MUST have both tokens
      case Some("oauth2session") =>
        // The client intends OAuth2Session auth but is missing material
        // and may accidentally app auth unless we reject the request.
        // See discussion at go/oauth2session_auth_header.
        if (!isViableOAuth2SessionRequest) {
          statsReceiver.counter("authTypeHeader/oAuth2Session/mismatch").incr()
          AuthResultCode.AuthTypeHeaderMismatch
        } else {
          AuthResultCode.Ok
        }

      // some random auth type
      case Some(otherAuthType) =>
        // The client declared an auth type which has no validity checks.
        // (This is unusual, unless you're rolling out a new authtype.)
        log.warning("Unexpected authTypeHeader=" + otherAuthType)
        statsReceiver.counter("authTypeHeader/unknown").incr()
        AuthResultCode.Ok

      // no auth header type at all
      case None =>
        // This is the expected path for most requests.
        statsReceiver.counter("authTypeHeader/none").incr()

        if (isViableOAuth2SessionRequest) {
          // The client is missing the auth type header for OAuth2Session.
          statsReceiver.counter("authTypeHeader/none/oAuth2SessionRequest").incr()

          /*
          // TODO - this must move to GUEST_TOKEN_HEADER auth verification?
          if (guestTokenHeader.isDefined) {
            // The client is making a guest auth request but has provided
            // enough material for an OAuth2Session auth attempt incidentally.
            // It could break guest auth users to require an auth type header.
            statsReceiver.counter("authTypeHeader/none/guestAuthAndOAuth2SessionRequest").incr()
          }
          else
           */
          if (!shouldAllowOAuth2SessionWithoutAuthTypeHeader) {
            // We enforce request hygiene because without an auth type header
            // the client doesn't unambiguously understand the auth request they
            // are actually making.
            statsReceiver.counter("authTypeHeader/none/oAuth2SessionRequest/disallowed").incr()
            AuthResultCode.AuthTypeHeaderMissing
          } else {
            // Allow OAuth2Session requests without auth type header for now.
            statsReceiver.counter("authTypeHeader/none/oAuth2SessionRequest/allowed").incr()
            AuthResultCode.Ok
          }
        } else {
          AuthResultCode.Ok
        }
    }
  }

  def paramsToMap(params: java.util.List[Pair]): Map[String, Seq[String]] = {
    val paramsMap = mutable.Map[String, Seq[String]]()
    params.forEach { pair =>
      if (paramsMap.contains(pair.key)) {
        paramsMap.put(pair.key, paramsMap(pair.key) ++ Seq(pair.value))
      } else {
        paramsMap.put(pair.key, Seq(pair.value))
      }
    }

    paramsMap.toMap
  }

  def getIPFromHeader(request: AuthRequest): Option[String] =
    request.headerMap
      .get(HttpHeaderNames.X_TWITTER_AUDIT_IP_THRIFT)
      .flatMap(ClientIpAddressUtils.decodeClientIpAddress(_))
      .flatMap(ClientIpAddressUtils.getString(_))
      .orElse(request.headerMap.get(HttpHeaderNames.X_TWITTER_AUDIT_IP)) // backward compatible.

  // Twitter client type will be specified in the request header, for example,
  // Twitter-iPad. This function extracts the client type from header map.
  def getClientHeader(request: AuthRequest): Option[String] =
    request.headerMap.get(HttpHeaderNames.X_TWITTER_CLIENT)

  def decodeGuestToken(guestToken: Option[String]): Option[Long] = {
    guestToken match {
      case Some(token) =>
        try {
          val guestId = UnsignedLongs.decode(token)
          Some(guestId)
        } catch {
          case e: NumberFormatException =>
            None
        }
      case _ =>
        None
    }
  }

  def getGuestTokenFromHeader(request: AuthRequest): Option[String] =
    request.headerMap.get(HttpHeaderNames.X_GUEST_TOKEN)

  def getUserIdFromHeader(request: AuthRequest): Option[String] =
    request.headerMap.get(HttpHeaderNames.X_TFE_USER_ID)

  def getAssertionSignatureFromHeader(request: AuthRequest): Option[String] =
    request.headerMap.get(HttpHeaderNames.X_TFE_USER_ASSERTION_SIGNATURE)

  def getClientAppIdFromHeader(request: AuthRequest): Option[String] =
    request.headerMap.get(HttpHeaderNames.X_TFE_CLIENT_APPLICATION_ID)

  def getTransactionIdFromHeader(request: AuthRequest): Option[String] =
    request.headerMap.get(HttpHeaderNames.X_TFE_TRANSACTION_ID)

  def getScheme(request: AuthRequest): Option[String] =
    request.headerMap.get(HttpHeaderNames.X_FORWARDED_PROTO)

  // OAuth2 request is only allowed when sent on HTTPS, or it's an internal request
  // otherwise, it is not allowed and will be marked as unknown request
  def isOAuth2Allowed(request: AuthRequest): Boolean =
    getScheme(request).map(_.toLowerCase == "https").getOrElse(false) ||
      request.headerMap
        .get(HttpHeaderNames.X_TWITTER_INTERNAL).map(_.toLowerCase == "internal").getOrElse(false)

  def isValidOAuth2Req(request: AuthRequest, oAuthParamsBuilder: OAuthParamsBuilder): Boolean =
    isOAuth2Allowed(request) && oAuthParamsBuilder.isOAuth2

  def getHost(request: AuthRequest): Option[String] = request.headerMap.get(Fields.Host)

  def getPort(request: AuthRequest): Option[Int] = request.headerMap
    .get(HttpHeaderNames.X_TFE_PORT).map(_.toInt)

  def getVerb(request: AuthRequest): Option[String] = request.method

  def getQueryString(request: AuthRequest): Option[String] = {
    request.url.map { uri =>
      val queryIndex = uri.indexOf('?')
      var fragmentIndex = uri.lastIndexOf('#')
      if (fragmentIndex == -1 || fragmentIndex < queryIndex) fragmentIndex = uri.length
      if (queryIndex == -1) ""
      else uri.substring(queryIndex + 1, fragmentIndex)
    }
  }

  def getPath(uri: String): String = {
    val queryIndex = uri.indexOf('?')
    var fragmIndex = uri.lastIndexOf('#')
    if (fragmIndex == -1 || fragmIndex < queryIndex) fragmIndex = uri.length
    if (queryIndex == -1) uri.substring(0, fragmIndex)
    else uri.substring(0, queryIndex)
  }

  def getContentType(request: AuthRequest): Option[String] = request.headerMap
    .get(Fields.ContentType)

  def getAuthTypeHeader(request: AuthRequest): Option[String] =
    request.headerMap.get(HttpHeaderNames.X_TWITTER_AUTH_TYPE)
}
