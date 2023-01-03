package com.twitter.auth.apiverification.models

case class AccessTokenResponse(accessToken: String, refreshToken: Option[String])
