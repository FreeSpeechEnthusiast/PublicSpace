package com.twitter.auth.utils

import com.twitter.appsec.sanitization.URLSafety.{httpOnly, sanitizeUrl}
import scala.collection.mutable

case class ValidationError(message: String, fieldName: Option[String])

/*
 * Use URLSafety.sanitizeUrl for redirect URLs (e.g. sanitize JSON or Java Script)
 * Use URLSafety.httpOnly for any url other than redirect URLs
 */
object SanitizationUtils {

  def validateHttpOnlyField(
    url: String,
    field: String,
    errorFields: mutable.Set[ValidationError]
  ): Unit = {
    if (!url.equals(httpOnly(url))) {
      errorFields.add(ValidationError("Only valid HTTP(S) urls are allowed.", Some(field)))
    }
  }

  def validateUrlField(
    url: String,
    field: String,
    errorFields: mutable.Set[ValidationError]
  ): Unit = {
    if (!url.equals(sanitizeUrl(url))) {
      errorFields.add(ValidationError("Disallowed characters detected in url.", Some(field)))
    }
  }
}
