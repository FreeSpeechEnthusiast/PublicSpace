package com.twitter.auth.customerauthtooling.api.services

import com.twitter.auth.customerauthtooling.thriftscala.ApplyRouteResponse
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.ApplyRoute
import com.twitter.auth.customerauthtooling.api.components.PacmanRouteDrafterService
import com.twitter.auth.customerauthtooling.api.models.PartialRouteInformation
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteApplyThriftService @Inject() (
  pacmanRouteDrafterService: PacmanRouteDrafterService)
    extends ApplyRoute.ReqRepServicePerEndpointServiceType
    with Logging
    with DefaultInputValues {

  def apply(
    thriftRequest: Request[ApplyRoute.Args]
  ): Future[Response[ApplyRouteResponse]] = {

    val request = thriftRequest.args.request

    pacmanRouteDrafterService
      .applyRoute(
        route = PartialRouteInformation.fromThrift(request.routeInfo),
        automaticDecider = request.automaticDecider.getOrElse(DefaultAutoDecider)
      ).map {
        case Some(draft) =>
          Response(ApplyRouteResponse(status = true, routeDraft = Some(draft.toThrift)))
        case None => Response(ApplyRouteResponse(status = false, routeDraft = None))
      }
  }
}
