package com.twitter.auth.customerauthtooling.api.services

import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.GetRoutesByRouteIds
import com.twitter.auth.customerauthtooling.thriftscala.GetRoutesByRouteIdsResponse
import com.twitter.auth.customerauthtooling.api.components.pacmanngroutestorage.PacmanNgRouteStorageServiceInterface
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import scala.util.chaining._
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRetrieverThriftService @Inject() (
  pacmanNgRouteStorageServiceInterface: PacmanNgRouteStorageServiceInterface)
    extends GetRoutesByRouteIds.ReqRepServicePerEndpointServiceType
    with Logging
    with DefaultInputValues {

  def apply(
    thriftRequest: Request[GetRoutesByRouteIds.Args]
  ): Future[Response[GetRoutesByRouteIdsResponse]] = {

    val request = thriftRequest.args.request

    pacmanNgRouteStorageServiceInterface
      .getRoutesByIds(
        routeIds = request.routeIds.toSet
      ).map { r =>
        Response(
          GetRoutesByRouteIdsResponse(
            status = true,
            routes = Some(
              r.map(_.pipe(RouteInformation.fromRawNgRouteWithResourceInfo)
                  .pipe(_.toThrift))
                .toSet)))
      }.rescue {
        case _ => Future.value(Response(GetRoutesByRouteIdsResponse(status = false, routes = None)))
      }
  }
}
