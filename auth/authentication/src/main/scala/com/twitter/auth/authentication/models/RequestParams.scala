package com.twitter.auth.authentication.models

import com.twitter.auth.authresultcode.thriftscala.AuthResultCode
import com.twitter.auth.models.AccessToken
import com.twitter.auth.models.ClientApplication
import com.twitter.joauth.OAuthParams
import com.twitter.joauth.Request
import java.util

case class ActAsUserParams(
  actAsUserIdCookie: Option[Long],
  actAsUserIdHeader: Option[Long],
  isSTEAMDelegatedRequest: Boolean = false)

trait RequestParams {
  val passportId: String
}

trait OAuth1RequestParams {
  val actAsUserParams: Option[ActAsUserParams]
  val oauthParams: OAuthParams.OAuth1Params
  val otherParams: Option[util.List[Request.Pair]]
  val scheme: Option[String]
  val host: Option[String]
  val port: Option[Int]
  val verb: Option[String]
  val path: String
  val ip: Option[String]
  val twitterClient: Option[String]
}

case class OAuth1TwoLeggedRequestParams(
  passportId: String,
  actAsUserParams: Option[ActAsUserParams],
  oauthParams: OAuthParams.OAuth1Params,
  otherParams: Option[util.List[Request.Pair]],
  scheme: Option[String],
  host: Option[String],
  port: Option[Int],
  verb: Option[String],
  path: String,
  ip: Option[String],
  twitterClient: Option[String])
    extends RequestParams
    with OAuth1RequestParams

case class OAuth1ThreeLeggedRequestParams(
  passportId: String,
  actAsUserParams: Option[ActAsUserParams],
  oauthParams: OAuthParams.OAuth1Params,
  otherParams: Option[util.List[Request.Pair]],
  scheme: Option[String],
  host: Option[String],
  port: Option[Int],
  verb: Option[String],
  path: String,
  ip: Option[String],
  twitterClient: Option[String])
    extends RequestParams
    with OAuth1RequestParams

case class OAuth2RequestParams(
  passportId: String,
  bearerToken: String,
  otherParams: Map[String, Seq[String]],
  path: String,
  ip: Option[String],
  twitterClient: Option[String])
    extends RequestParams

case class OAuth2ClientCredentialRequestParams(
  passportId: String,
  clientAccessToken: String,
  otherParams: Map[String, Seq[String]],
  path: String,
  ip: Option[String],
  twitterClient: Option[String])
    extends RequestParams

case class OAuth2SessionRequestParams(
  passportId: String,
  actAsUserParams: Option[ActAsUserParams],
  bearerToken: String,
  session: String,
  otherParams: Map[String, Seq[String]],
  path: String,
  ip: Option[String],
  twitterClient: Option[String],
  guestToken: Option[Long])
    extends RequestParams

case class RestrictedSessionRequestParams(
  passportId: String,
  actAsUserParams: Option[ActAsUserParams],
  bearerToken: String,
  session: String,
  otherParams: Map[String, Seq[String]],
  path: String,
  ip: Option[String],
  twitterClient: Option[String],
  guestToken: Option[Long])
    extends RequestParams

case class GuestAuthRequestParams(
  passportId: String,
  bearerToken: String,
  otherParams: Map[String, Seq[String]],
  path: String,
  ip: Option[String],
  twitterClient: Option[String],
  guestToken: Option[Long])
    extends RequestParams

case class TiaAuthRequestParams(
  passportId: String,
  userId: Option[Long],
  signature: String,
  clientAppId: Option[String],
  additionalFields: Seq[String])
    extends RequestParams

case class SessionRequestParams(
  passportId: String,
  actAsUserParams: Option[ActAsUserParams],
  authToken: String,
  clientTokenCookie: Option[String],
  otherParams: Map[String, Seq[String]])
    extends RequestParams

case class PartnerAppParams(
  authResultCode: AuthResultCode,
  partnerToken: Option[AccessToken] = None,
  partnerApp: Option[ClientApplication] = None)

case class AmpEmailRequestParams(
  passportId: String,
  ampEmailToken: String)
    extends RequestParams

case class BadRequestParams(
  passportId: String,
  authResult: AuthResultCode)
    extends RequestParams

object RequestParams {
  val AuthorizationHeaderKey = "authorization"
}
