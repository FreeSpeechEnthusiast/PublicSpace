package com.twitter.auth.authentication.models

import com.twitter.finagle.http.Request
import com.twitter.flightauth.thriftscala.AuthenticateRequest
import org.apache.http.client.utils.URIBuilder

object RequestWithAmpEmailToken {
  var AmpTokenQueryParameterName = "amp_email_token"

  def unapply(r: Request): Option[String] = {
    val token = (new URIBuilder(r.uri)).getQueryParams
      .stream()
      .filter(p =>
        p.getName
          .equals(AmpTokenQueryParameterName))
      .findFirst()
    if (token.isPresent) {
      Some(token.get().getValue)
    } else {
      None
    }
  }

  def unapply(r: AuthRequest): Option[String] = {
    r.ampEmailToken
  }

  def unapply(r: AuthenticateRequest): Option[String] = {
    r.additionalRequestMetadata match {
      case Some(metadata) => metadata.ampEmailToken
      case None => None
    }
  }

  def redactToken(r: Request): Unit = {
    val uriBuilder = new URIBuilder(r.uri);
    uriBuilder.setParameter(AmpTokenQueryParameterName, "xxx")
    r.uri = uriBuilder.build().toString
  }
}
