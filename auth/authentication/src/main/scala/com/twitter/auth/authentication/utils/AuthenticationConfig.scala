package com.twitter.auth.authentication.utils

import scala.collection.immutable.HashSet

object AuthenticationConfig {
  val WhitelistedOAuth1RequestTokenPaths = Set("/oauth/access_token")
  val OauthAuthRequests = HashSet("/oauth/authorize", "/oauth/authenticate")
  val maxClockFloatAheadMins: Int = 24 * 60
  val maxClockFloatBehindMins: Int = 25 * 60
  val maxClockFloatAheadSecs: Long = maxClockFloatAheadMins * 60L
  val maxClockFloatBehindSecs: Long = maxClockFloatBehindMins * 60L
  val BITS_PER_ENCODED_BYTE = 6
  val BYTES_PER_ENCODED_BLOCK = 4
  // For APPSERVICES-14200
  val IOsID = 312240L
  val TwitterForIphoneId = 1082764L
  val Callback = "callback"
  val ApplicationId = "application_id"
  val XReverseAuthParameters = "x_reverse_auth_parameters"

  /**
   * This is to distinguish Twitter for Ipad from Twitter for Iphone, for analytics purposes
   */
  val TwitterIpadHeader = "Twitter-iPad"
  val TwitterForIpadID = 191841L

  val LoginVerificationUserId = "login_verification_user_id"
  val LoginVerificationRequestId = "login_verification_request_id"
  val LoginVerificationChallengeResponse =
    "login_verification_challenge_response"
  val XAuthUsername = "x_auth_username"
  val XAuthPassword = "x_auth_password"
  val XAuthMode = "x_auth_mode"
  val ClientAuth = "client_auth"
}
