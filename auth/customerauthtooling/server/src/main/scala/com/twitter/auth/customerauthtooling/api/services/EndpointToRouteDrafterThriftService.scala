package com.twitter.auth.customerauthtooling.api.services

import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.DraftRouteFromEndpoint
import com.twitter.auth.customerauthtooling.thriftscala.DraftRouteFromEndpointResponse
import com.twitter.auth.customerauthtooling.api.components.PacmanRouteDrafterService
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EndpointToRouteDrafterThriftService @Inject() (
  pacmanRouteDrafterService: PacmanRouteDrafterService)
    extends DraftRouteFromEndpoint.ReqRepServicePerEndpointServiceType
    with Logging
    with DefaultInputValues {

  def apply(
    thriftRequest: Request[DraftRouteFromEndpoint.Args]
  ): Future[Response[DraftRouteFromEndpointResponse]] = {

    val request = thriftRequest.args.request

    pacmanRouteDrafterService
      .draftRouteFromEndpoint(
        endpoint = EndpointInfo.fromThrift(request.endpointInfo),
        project = request.project,
        dpProviderName = Some(request.dpProviderName.getOrElse(DefaultDpProvider)),
        update = request.update.getOrElse(DefaultUpdateOnDraft),
        automaticDecider = request.automaticDecider.getOrElse(DefaultAutoDecider)
      ).map {
        case Some(draft) =>
          Response(DraftRouteFromEndpointResponse(status = true, routeDraft = Some(draft.toThrift)))
        case None => Response(DraftRouteFromEndpointResponse(status = false, routeDraft = None))
      }
  }
}
