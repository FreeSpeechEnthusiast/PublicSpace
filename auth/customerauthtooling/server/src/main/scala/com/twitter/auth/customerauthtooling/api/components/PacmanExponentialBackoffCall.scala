package com.twitter.auth.customerauthtooling.api.components

import com.twitter.cim.pacman.plugin.tferoute.models.ProvisioningResponse
import com.twitter.cim.pacman.thriftscala.PacmanException
import com.twitter.cim.pacman.thriftscala.PacmanServiceErrorCode
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finagle.util.DefaultTimer.Implicit
import com.twitter.util.Duration
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import java.util.concurrent.TimeUnit

// A request handler that waits an exponentially increasing amount of time before trying again.
// This is necessary because pacman is super slow sometimes.
//
// for factor 10 and maxTimeoutSeconds = 3 seconds following delays will be used
// 0ms, 20ms, 40ms, 80ms, 160ms, 320ms, 640ms, 1280ms, 2560ms, 3000ms
//
// Based on com.twitter.ads.api.routes_management.kite
case class PacmanExponentialBackoffCall[Req](
  f: Req => Future[ProvisioningResponse],
  factor: Float = 10f,
  maxTimeoutSeconds: Duration = 30.seconds)
    extends Logging {
  def apply(
    createOrUpdateRouteRequest: Req
  ): Future[ProvisioningResponse] = {
    call(createOrUpdateRouteRequest, attempt = 0)
      .raiseWithin(timeout = maxTimeoutSeconds, exc = PacmanTimeoutException(maxTimeoutSeconds))
  }

  private def call(
    request: Req,
    attempt: Long
  ): Future[ProvisioningResponse] = {
    val delayFuture = if (attempt > 0) {
      Future.sleep(Duration(Math.ceil(Math.pow(2, attempt) * factor).toInt, TimeUnit.MILLISECONDS))(
        DefaultTimer)
    } else {
      Future.Done
    }
    delayFuture.flatMap { _ =>
      f(request).rescue {
        case e: PacmanException =>
          e.errorCode.map {
            case PacmanServiceErrorCode.InternalServerError =>
              if (e.message.exists(_.startsWith("Unexpected end-of-input"))) {
                error(s"$e")
                Future.value(ProvisioningResponse(status = "skipped", uuid = ""))
              } else {
                throw e
              }
            case PacmanServiceErrorCode.ProvisioningConflict =>
              if (e.message.exists(_.contains("already in flight"))) {
                error(s"$e")
                call(request, attempt + 1)
              } else if (e.message.exists(_.contains("already exists"))) {
                error(s"$e")
                Future.value(ProvisioningResponse(status = "skipped", uuid = ""))
              } else {
                throw e
              }
            case _ => throw e
          }.get
      }
    }
  }
}
