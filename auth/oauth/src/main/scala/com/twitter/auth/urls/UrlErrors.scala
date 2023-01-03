package com.twitter.auth.urls

object UrlErrors extends Enumeration {
  type UrlErrors = Value

  val APP_WITHOUT_REDIRECT_URI = Value
  val INVALID_REDIRECT_URI = Value
  val REDIRECT_URI_LOCKED = Value
  val APP_NOT_FOUND = Value
  val OOB_NOT_ALLOWED = Value
  val URI_TOO_LONG = Value
  val INVALID_URI_FORMAT = Value
  val XSS = Value
  val LOCK_DOWN_EXEMPT = Value

}
