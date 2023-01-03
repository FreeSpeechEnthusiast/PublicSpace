package com.twitter.auth.customerauthtooling.api.services

import com.twitter.auth.customerauthtooling.thriftscala.ApplyRoutesResponse
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.ApplyRoutes
import com.twitter.auth.customerauthtooling.api.components.PacmanRouteDrafterService
import com.twitter.auth.customerauthtooling.api.models.PartialRouteInformation
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteBatchApplyThriftService @Inject() (
  pacmanRouteDrafterService: PacmanRouteDrafterService)
    extends ApplyRoutes.ReqRepServicePerEndpointServiceType
    with Logging
    with DefaultInputValues {

  def apply(
    thriftRequest: Request[ApplyRoutes.Args]
  ): Future[Response[ApplyRoutesResponse]] = {

    val request = thriftRequest.args.request

    pacmanRouteDrafterService
      .applyRoutes(
        routes = request.routes.map(PartialRouteInformation.fromThrift).toSet,
        automaticDecider = request.automaticDecider.getOrElse(DefaultAutoDecider),
        ignoreInvalid = request.ignoreInvalid.getOrElse(DefaultIgnoreInvalid),
        ignoreErrors = request.ignoreErrors.getOrElse(DefaultIgnoreErrors),
      ).map { draft =>
        Response(
          ApplyRoutesResponse(status = !draft.wasStopped, batchRouteDraft = Some(draft.toThrift)))
      }
  }
}
