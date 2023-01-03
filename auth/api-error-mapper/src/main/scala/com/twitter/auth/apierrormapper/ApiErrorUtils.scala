package com.twitter.auth.apierrormapper

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status
import com.twitter.finatra.api11.ApiError
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode
import com.twitter.finatra.request.ApiVersion
import com.twitter.finatra.request.TwitterHeaderNames

object ApiErrorUtils {
  def getApiError(
    request: Request,
    authStatusCode: Option[Int],
    authResultCode: Option[AuthResultCode]
  ): ApiError = {
    (authStatusCode, authResultCode) match {
      case (Some(statusCode), Some(resultCode)) =>
        getAuthError(request, statusCode, resultCode)
      case _ =>
        ApiError.InvalidCredentials
    }
  }

  private def shouldGenerateIOSError(request: Request): Boolean =
    (!ApiVersion.isApiV11(request) && request.headerMap
      .get(TwitterHeaderNames.TwitterClient)
      .exists(_.startsWith("Twitter-i")))

  private def getAuthError(
    request: Request,
    statusCode: Int,
    authResultCode: AuthResultCode
  ): ApiError = {
    val apiErrorMap: Map[Int, ApiError] = shouldGenerateIOSError(request) match {
      case true => IOSHackErrorServiceMap
      case false => authResultCodeMap(authResultCode).getOrElse(ErrorServiceMap)
    }

    apiErrorMap.get(statusCode).getOrElse(ApiError.InternalError)
  }

  /**
   * Migrated from: twitter/finatra/api11/filters/RequireAuthenticationFilter.scala
   * */
  private def authResultCodeMap(authResultCode: AuthResultCode): Option[Map[Int, ApiError]] = {
    def makeMap(err: ApiError) = Some(Map(err.status.code -> err))

    authResultCode match {
      case AuthResultCode.BadAccessToken => makeMap(ApiError.BadOauthToken)
      case AuthResultCode.OrphanedAccessToken => makeMap(ApiError.BadOauthToken)
      case AuthResultCode.AccessTokenNotFound => makeMap(ApiError.BadOauthToken)
      case AuthResultCode.ClientAccessTokenNotFound => makeMap(ApiError.BadOauthToken)
      case AuthResultCode.TimestampOutOfRange => makeMap(ApiError.OauthTimestampException)
      case AuthResultCode.BadGuestToken => makeMap(ApiError.BadGuestToken)
      case AuthResultCode.BadDeviceToken => makeMap(ApiError.BadDeviceToken)
      case AuthResultCode.DevicePinInvalid => makeMap(ApiError.DevicePinInvalid)
      case AuthResultCode.DevicePinRequired => makeMap(ApiError.DevicePinRequired)
      case AuthResultCode.UnexpectedDevicePin => makeMap(ApiError.UnexpectedDeviceProvided)
      case AuthResultCode.ContributorsRelationshipInvalid =>
        makeMap(ApiError.ContributionNotPermitted)
      case _ => None
    }
  }

  private val ErrorServiceMap: Map[Int, ApiError] = Map(
    400 -> ApiError.BadAuthenticationData,
    401 -> ApiError.InvalidCredentials,
    403 -> ApiError.RestrictedAuthToken
  )

  // Old IOS clients still out there will break unless we use these specific errors
  private val IOSHackErrorServiceMap: Map[Int, ApiError] = Map(
    400 -> ApiError.InvalidCredentials.copy(status = Status.BadRequest),
    401 -> ApiError.InvalidCredentials
  )
}
