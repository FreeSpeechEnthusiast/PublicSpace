package com.twitter.auth.apiverification

object CommonFixtures {

  val API_TWITTER_DOMAIN = "api.twitter.com"
  val TFE_TEST_SERVICE_DOMAIN = "tfetest.twitter.com"
  val TLS_PORT = 443
  val AUTHORIZE_PATH = "/2/oauth2/authorize"
  val TOKEN_PATH = "/2/oauth2/token"
  val REVOKE_PATH = "/2/oauth2/revoke"
  val Code = "code"
  val AUTH_CODE_RESPONSE_TYPE = "authorization_code"
  val AUTH_CODE = "auth_code"
  val TFE_CSRF_TOKEN_HEADER = "X-Csrf-Token"
  val AUTHORIZATION_HEADER = "Authorization"
  val HOST = "host"
  val COOKIE = "Cookie"
  val CT0 = "ct0"
  val AUTH_TOKEN = "auth_token"
  val REFRESH_TOKEN = "refresh_token"
  val URL_ENCODED_FORM_CONTENT_TYPE = "application/x-www-form-urlencoded"
  val FULL_SCOPE_WITH_OFFLINE_ACCESS =
    "tweet.read%20users.read%20tweet.moderate.write%20account.follows.read%20account.follows.write%20offline.access%20space.read"
  val FULL_SCOPE_WITHOUT_OFFLINE_ACCESS =
    "tweet.read%20users.read%20tweet.moderate.write%20account.follows.read%20account.follows.write%20space.read"
  val LIMITED_SCOPE = "tweet.read%20offline.access"

}
