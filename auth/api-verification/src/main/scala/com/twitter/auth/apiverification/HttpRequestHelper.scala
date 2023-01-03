package com.twitter.auth.apiverification

import com.twitter.auth.apiverification.CommonFixtures._
import com.twitter.finagle.http.{MediaType, Method, Request, Response, Status}
import com.twitter.finagle.stats.StatsReceiver
import scala.util.parsing.json.JSON

object HttpRequestHelper {

  def createAuthCodeRequest(
    clientId: String,
    redirectUri: String,
    scopes: String,
    state: String,
    codeChallenge: String,
    codeChallengeMethod: String,
    csrfToken: String,
    appOnlyToken: String,
    sessionToken: String
  ): Request = {
    val request = Request(
      method = Method.Get,
      uri = authCodeUri(clientId, redirectUri, scopes, state, codeChallenge, codeChallengeMethod)
    )
    request.host(API_TWITTER_DOMAIN)
    request.headerMap.add(TFE_CSRF_TOKEN_HEADER, csrfToken)
    request.headerMap.add(AUTHORIZATION_HEADER, bearerAuthHeader(appOnlyToken))
    request.headerMap.add(COOKIE, oauth2SessionCookie(sessionToken, csrfToken))
    request
  }

  def getAuthCodeFromCreateAuthCodeResponse(response: Response): String = {
    JSON
      .parseFull(response.contentString).map { jsonObj =>
        jsonObj.asInstanceOf[Map[String, Any]](AUTH_CODE).asInstanceOf[String]
      }.get
  }

  def approveAuthCodeRequest(
    authCode: String,
    csrfToken: String,
    appOnlyToken: String,
    sessionToken: String
  ): Request = {
    val request = Request(
      method = Method.Post,
      uri = AUTHORIZE_PATH
    )
    request.host(API_TWITTER_DOMAIN)
    request.setContentType(URL_ENCODED_FORM_CONTENT_TYPE)
    request.headerMap.add(TFE_CSRF_TOKEN_HEADER, csrfToken)
    request.headerMap.add(AUTHORIZATION_HEADER, bearerAuthHeader(appOnlyToken))
    request.headerMap.add(COOKIE, oauth2SessionCookie(sessionToken, csrfToken))
    request.contentType = MediaType.WwwForm
    request.contentString = approveAuthCodeBody(authCode)
    request
  }

  def accessTokenRequest(
    authCode: String,
    clientId: String,
    redirectUri: String,
    codeVerifier: String
  ): Request = {
    val request = Request(
      method = Method.Post,
      uri = TOKEN_PATH
    )
    request.host(API_TWITTER_DOMAIN)
    request.contentType = MediaType.WwwForm
    request.contentString = accessTokenBody(authCode, clientId, redirectUri, codeVerifier)
    request
  }

  def refreshTokenRequest(refreshToken: String, clientId: String): Request = {
    val request = Request(
      method = Method.Post,
      uri = TOKEN_PATH
    )
    request.host(API_TWITTER_DOMAIN)
    request.contentType = MediaType.WwwForm
    request.contentString = refreshTokenBody(refreshToken, clientId)
    request
  }

  def revokeTokenRequest(tokenToRevoke: String, clientId: String): Request = {
    val request = Request(
      method = Method.Post,
      uri = REVOKE_PATH
    )
    request.host(API_TWITTER_DOMAIN)
    request.contentType = MediaType.WwwForm
    request.contentString = revokeTokenBody(token = tokenToRevoke, clientId = clientId)
    request
  }

  def vnextV2GetRequest(tokenInHeader: String, path: String): Request = {
    val request = Request(
      method = Method.Get,
      uri = path
    )
    request.host(API_TWITTER_DOMAIN)
    request.headerMap.add(AUTHORIZATION_HEADER, bearerAuthHeader(tokenInHeader))
    request
  }

  def vnextV2PostRequest(tokenInHeader: String, path: String, content: String): Request = {
    val request = Request(
      method = Method.Post,
      uri = path
    )
    request.mediaType = MediaType.Json
    request.contentString = content
    request.host(API_TWITTER_DOMAIN)
    request.headerMap.add(AUTHORIZATION_HEADER, bearerAuthHeader(tokenInHeader))
    request
  }

  def vnextV2DeleteRequest(tokenInHeader: String, path: String, content: String): Request = {
    val request = Request(
      method = Method.Delete,
      uri = path
    )
    request.mediaType = MediaType.Json
    request.contentString = content
    request.host(API_TWITTER_DOMAIN)
    request.headerMap.add(AUTHORIZATION_HEADER, bearerAuthHeader(tokenInHeader))
    request
  }

  def authTestRequest(tokenInHeader: String, path: String): Request = {
    val request = Request(
      method = Method.Get,
      uri = path
    )
    request.host(API_TWITTER_DOMAIN)
    request.headerMap.add(HOST, TFE_TEST_SERVICE_DOMAIN)
    request.headerMap.add(AUTHORIZATION_HEADER, bearerAuthHeader(tokenInHeader))

    request
  }

  def getAccessTokenFromResponse(response: Response): String = {
    JSON
      .parseFull(response.contentString).map { jsonObj =>
        jsonObj.asInstanceOf[Map[String, Any]]("access_token").asInstanceOf[String]
      }.get
  }

  def getRefreshTokenFromResponse(response: Response): Option[String] = {
    try {
      JSON
        .parseFull(response.contentString).map { jsonObj =>
          jsonObj.asInstanceOf[Map[String, Any]]("refresh_token").asInstanceOf[String]
        }
    } catch {
      case e: NoSuchElementException =>
        None
    }
  }

  def assertStatusCode(expected: Status, actual: Status, statsReceiver: StatsReceiver): Unit = {
    statsReceiver.counter(s"$actual").incr()
    if (expected == actual) {
      statsReceiver.counter("ok").incr()
    } else {
      statsReceiver.counter("unexpected_status_code").incr()
    }
  }

  private[this] def authCodeUri(
    clientId: String,
    redirectUri: String,
    scopes: String,
    state: String,
    codeChallenge: String,
    codeChallengeMethod: String
  ): String =
    s"$AUTHORIZE_PATH?" +
      s"response_type=$Code" +
      s"&client_id=$clientId" +
      s"&redirect_uri=$redirectUri" +
      s"&scope=$scopes" +
      s"&state=$state" +
      s"&code_challenge=$codeChallenge" +
      s"&code_challenge_method=$codeChallengeMethod"

  private[this] def accessTokenBody(
    authCode: String,
    clientId: String,
    redirectUri: String,
    codeVerifier: String
  ): String = {
    s"code=$authCode&grant_type=$AUTH_CODE_RESPONSE_TYPE&client_id=$clientId&redirect_uri=$redirectUri&code_verifier=$codeVerifier"
  }

  private[this] def refreshTokenBody(refreshToken: String, clientId: String): String = {
    s"refresh_token=$refreshToken&grant_type=$REFRESH_TOKEN&client_id=$clientId"
  }

  private[this] def approveAuthCodeBody(authCode: String) = {
    s"code=$authCode&approval=true"
  }

  private[this] def revokeTokenBody(token: String, clientId: String) = {
    s"token_type_hint=access_token&token=$token&client_id=$clientId"
  }

  private[this] def bearerAuthHeader(token: String): String = {
    s"Bearer $token"
  }

  private[this] def oauth2SessionCookie(sessionToken: String, csrfToken: String) = {
    s"$CT0=$csrfToken; $AUTH_TOKEN=$sessionToken"
  }

}
