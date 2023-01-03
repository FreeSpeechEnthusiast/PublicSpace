package com.twitter.auth.customerauthtooling.api.services

import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.DraftRoute
import com.twitter.auth.customerauthtooling.thriftscala.DraftRouteResponse
import com.twitter.auth.customerauthtooling.api.components.PacmanRouteDrafterService
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteDrafterThriftService @Inject() (
  pacmanRouteDrafterService: PacmanRouteDrafterService)
    extends DraftRoute.ReqRepServicePerEndpointServiceType
    with Logging
    with DefaultInputValues {

  def apply(
    thriftRequest: Request[DraftRoute.Args]
  ): Future[Response[DraftRouteResponse]] = {

    val request = thriftRequest.args.request

    pacmanRouteDrafterService
      .draftRoute(
        route = RouteInformation.fromThrift(request.routeInfo),
        update = request.update.getOrElse(DefaultUpdateOnDraft),
        automaticDecider = request.automaticDecider.getOrElse(DefaultAutoDecider)
      ).map {
        case Some(draft) =>
          Response(DraftRouteResponse(status = true, routeDraft = Some(draft.toThrift)))
        case None => Response(DraftRouteResponse(status = false, routeDraft = None))
      }
  }
}
