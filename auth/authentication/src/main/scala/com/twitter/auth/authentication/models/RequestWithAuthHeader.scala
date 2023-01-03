package com.twitter.auth.authentication.models

import com.twitter.finagle.http.Fields
import com.twitter.finagle.http.Request

object RequestWithAuthHeader {
  def unapply(r: Request): Option[AuthHeader] = {
    r.headerMap.get(Fields.Authorization).flatMap { header =>
      AuthHeader(header)
    }
  }
}
