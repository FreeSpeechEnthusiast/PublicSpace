package com.twitter.auth.policykeeper.api.enforcement

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finagle.http.Fields
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.api11.ApiError.AccessDeniedByBouncer
import com.twitter.finatra.bouncer.exceptions.BounceExceptionMapper
import com.twitter.finatra.bouncer.exceptions.BouncerError
import com.twitter.finatra.bouncer.exceptions.BouncerErrors
import com.twitter.finatra.tfe.HttpHeaderNames
import com.twitter.tfe.core.http.MediaType

/**
 * For TFE compatibility we have to avoid ResponseBuilder
 * @param statsReceiver
 */
class TFEBounceExceptionMapper(statsReceiver: StatsReceiver)
    extends BounceExceptionMapper(response = null, statsReceiver = statsReceiver) {

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  override def renderJson(request: Request, error: BouncerError): Response = {
    val response = Response.apply(request.version, AccessDeniedByBouncer.status)
    response.headerMap.setUnsafe(Fields.ContentType, MediaType.APPLICATION_JSON)
    response.headerMap
      .add(HttpHeaderNames.X_TFE_ERROR_CODE, AccessDeniedByBouncer.code.toString)
    response.withOutputStream { os =>
      mapper.writeValue(os, BouncerErrors(Seq(error)))
    }
    response
  }
}
