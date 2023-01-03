package com.twitter.auth.authentication.utils

import com.twitter.auth.authenforcement.thriftscala.Passport
import com.twitter.auth.authentication.models.AuthRequest
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode
import com.twitter.finagle.http.Fields
import com.twitter.finagle.mtls.authentication.ServiceIdentifier
import com.twitter.finagle.tracing.Trace
import com.twitter.finatra.tfe.HttpHeaderNames
import com.twitter.joauth.Request
import com.twitter.joauth.Request.Pair
import com.twitter.util.Future
import scala.collection.mutable
import java.util
import java.util.Base64
import java.util.regex.Pattern
import scala.collection.JavaConverters._

object AuthenticationUtils {
  private val BASE64_ENCODER = Base64.getUrlEncoder
  val WhitelistedOAuth1RequestTokenPaths: Set[String] = Set("/oauth/access_token")

  def getQueryString(request: AuthRequest): Option[String] = {
    request.url.map { uri =>
      val queryIndex = uri.indexOf('?')
      var fragmentIndex = uri.lastIndexOf('#')
      if (fragmentIndex == -1 || fragmentIndex < queryIndex) fragmentIndex = uri.length
      if (queryIndex == -1) ""
      else uri.substring(queryIndex + 1, fragmentIndex)
    }
  }

  def getContentType(request: AuthRequest): Option[String] = request.headerMap
    .get(Fields.ContentType)

  def getDeviceTokenFromAuthHeader(request: AuthRequest): Option[String] = {
    request.headerMap.get(HttpHeaderNames.X_DEVICE_TOKEN)
  }

  def getDevicePinFromAuthHeader(request: AuthRequest): Option[String] = {
    request.headerMap.get(HttpHeaderNames.X_DEVICE_PIN)
  }

  def processForTwitterForAndroid(request: AuthRequest): Boolean = {
    val clientHeader = request.headerMap.get(HttpHeaderNames.X_TWITTER_CLIENT)
    if (clientHeader.isDefined && "TwitterAndroid" == clientHeader.get) {
      val version = request.headerMap.get(HttpHeaderNames.X_TWITTER_CLIENT_VERSION)
      val ANDROID_VERSION_PATTERN: Pattern = Pattern.compile("(\\d+)\\.(\\d+)\\.\\d+")
      if (version.isDefined) {
        val m = ANDROID_VERSION_PATTERN.matcher(version.get)
        if (m.matches) {
          val major = m.group(1).toInt
          val minor = m.group(2).toInt
          // we want to do special things for anything earlier than 3.7.0
          return major < 3 || (major == 3 && minor < 7)
        }
      }
    }
    false
  }

  def allowAuthWithRequestToken(path: String): Boolean =
    AuthenticationConfig.WhitelistedOAuth1RequestTokenPaths.contains(path.toLowerCase())

  def getExternalAuthResultCode(authResultCode: AuthResultCode): Int = {
    authResultCode.getValue match {
      // OK = 300
      case 0 => 200
      // Passthrough server errors
      case id if id < 1000 => id.toInt
      // 1000 - 9999 are 401s
      case id if 1000 <= id && id < 10000 => 401
      // Others are 403s
      case _ => 403
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

  def getAuthTypeHeader(request: AuthRequest): Option[String] = {
    request.headerMap.get(HttpHeaderNames.X_TWITTER_AUTH_TYPE)
  }

  def tracePassport(passportFuture: Future[Passport]): Unit = {
    passportFuture
      .onSuccess(passport => Trace.recordBinary("passport", passport.toString))
      .onFailure(e => Trace.recordBinary("passport", e.getMessage))
  }

  def isDeveloperPassport(serviceIdentifier: ServiceIdentifier): Boolean =
    serviceIdentifier.isLocal && serviceIdentifier.environment == "devel"

  // check whether the request is Xauth request
  def isXauth(reqParams: Option[util.List[Request.Pair]]): Boolean = {
    reqParams match {
      case Some(params) =>
        val map = params.asScala.map(kv => kv.key -> kv.value).toMap
        val lvrChallengeResponsePresent: Boolean =
          map.contains(AuthenticationConfig.LoginVerificationUserId) && map.contains(
            AuthenticationConfig.LoginVerificationChallengeResponse
          )

        val usernameAndPasswordPresent: Boolean =
          map.contains(AuthenticationConfig.XAuthUsername) &&
            map.contains(AuthenticationConfig.XAuthPassword)

        val pollingForAccessToken: Boolean =
          map.contains(AuthenticationConfig.LoginVerificationUserId) &&
            map.contains(AuthenticationConfig.LoginVerificationRequestId)

        val exchangingCredentialsForAccessToken: Boolean =
          map.contains(AuthenticationConfig.XAuthMode) &&
            map.get(AuthenticationConfig.XAuthMode) == Some(AuthenticationConfig.ClientAuth) &&
            (usernameAndPasswordPresent || lvrChallengeResponsePresent)

        exchangingCredentialsForAccessToken || pollingForAccessToken
      case _ =>
        false
    }
  }

  /**
   * RFC-2617: https://datatracker.ietf.org/doc/html/rfc2617#section-2
   * The userid and password separated by a single colon (":") character, within a base64 encoded
   * string in the credentials.
   *
   * Example:
   * If the user agent wishes to send the userid "Aladdin" and password "open sesame",
   * it would use the following header field:
   *
   *  Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
   */
  def isValidBasicAuthHeader(authHeader: String, userId: String, password: String): Boolean = {
    authHeader.split(" +", 2) match {
      case Array("Basic", encodedPair) =>
        // userid:password
        encodedPair == BASE64_ENCODER.encodeToString(String.join(":", userId, password).getBytes)
      case _ => false
    }
  }
}
