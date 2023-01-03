package com.twitter.auth.authentication.unpacker

import com.twitter.joauth.OAuthParams.StandardOAuthParamsHelperImpl

case class UnpackerHelper(allowFloatingPointTimestamps: Boolean)
    extends StandardOAuthParamsHelperImpl {

  override def parseTimestamp(s: String): java.lang.Long = {
    if (allowFloatingPointTimestamps) try {
      s.toDouble.toLong
    } catch {
      case _: Throwable => null
    }
    else {
      super.parseTimestamp(s)
    }
  }
}
