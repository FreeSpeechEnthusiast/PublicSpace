package com.twitter.auth.authentication.unpacker

import com.twitter.auth.authresultcode.thriftscala.AuthResultCode
import com.twitter.finagle.http.HeaderMap
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.tfe.HttpHeaderNames
import com.twitter.util.{Return, Throw, Try}
import scala.collection.Map

case class AuthMultiCookieError(code: AuthResultCode) extends Exception

object AuthCookieParser {

  val AUTH_TOKEN_COOKIE = "auth_token"
  val AUTH_MULTI_COOKIE = "auth_multi"
  val CLIENT_TOKEN_COOKIE = "client_token"

  /***
   * A web api request (OAuth2+Session) may carry a cookie with a serialized map
   * of user_ids and auth_tokens, and a header indicating which to use for the request.
   *
   * @return
   */
  def apply(
    headers: HeaderMap,
    cookies: Option[Map[String, String]],
    shouldAllowWebAuthMultiUserIdHeader: Boolean,
    statsReceiver: StatsReceiver
  ): Try[Option[String]] = {

    val sr = statsReceiver.scope("AuthCookieParser")
    val authMultiUserIdHeader = Some(
      headers.getOrElse(HttpHeaderNames.X_WEB_AUTH_MULTI_USER_ID, None))

    authMultiUserIdHeader match {
      case Some(userId: String) if shouldAllowWebAuthMultiUserIdHeader =>
        val authMultiCookie = cookies.flatMap(_.get(AUTH_MULTI_COOKIE))
        authMultiCookie match {
          case Some(cookieString) =>
            val authMultiMapOption = parseAuthMultiCookie(cookieString)
            authMultiMapOption match {
              case Some(authMultiMap) =>
                val authToken = authMultiMap.get(userId)
                if (authToken.isDefined) {
                  sr.counter("auth_multi_success").incr()
                  Return(authToken)
                } else {
                  sr.counter("auth_multi_not_found_in_cookie").incr()
                  Throw(AuthMultiCookieError(AuthResultCode.AuthMultiNotFoundInCookie))
                }
              case None =>
                sr.counter("auth_multi_bad_cookie").incr()
                Throw(AuthMultiCookieError(AuthResultCode.AuthMultiBadCookie))
            }
          case _ =>
            sr.counter("auth_multi_cookie_missing").incr()
            Throw(AuthMultiCookieError(AuthResultCode.AuthMultiCookieMissing))
        }
      case _ =>
        // Default case is to just use the authCookie
        // (note that it can be missing as well)
        Return(getAuthTokenCookie(cookies))
    }
  }

  private[this] def parseAuthMultiCookie(cookieString: String): Option[Map[String, String]] = {
    // Cookie value is of the format "userid:token|userid:token|userid:token"
    Try {
      cookieString
        .split(":|\\|").grouped(2).map {
          case Array(k, v) => k -> v
        }.toMap
    }.toOption
  }

  private[auth] def getAuthTokenCookie(cookies: Option[Map[String, String]]): Option[String] = {
    cookies.flatMap(_.get(AUTH_TOKEN_COOKIE))
  }

  private[auth] def getClientTokenCookie(cookies: Option[Map[String, String]]): Option[String] = {
    cookies.flatMap(_.get(CLIENT_TOKEN_COOKIE))
  }
}
