package com.twitter.auth.customerauthtooling.api.services

import com.twitter.auth.customerauthtooling.api.components.pacmanngroutestorage.PacmanNgRouteStorageServiceInterface
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.GetRoutesByProjects
import com.twitter.auth.customerauthtooling.thriftscala.GetRoutesByProjectsResponse
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import javax.inject.Inject
import javax.inject.Singleton
import scala.util.chaining._

@Singleton
class RouteByProjectRetrieverThriftService @Inject() (
  pacmanNgRouteStorageServiceInterface: PacmanNgRouteStorageServiceInterface)
    extends GetRoutesByProjects.ReqRepServicePerEndpointServiceType
    with Logging
    with DefaultInputValues {

  def apply(
    thriftRequest: Request[GetRoutesByProjects.Args]
  ): Future[Response[GetRoutesByProjectsResponse]] = {

    val request = thriftRequest.args.request

    pacmanNgRouteStorageServiceInterface
      .getRoutesByProjects(
        projects = request.projects.toSet
      ).map { r =>
        Response(
          GetRoutesByProjectsResponse(
            status = true,
            routes = Some(
              r.map(_.pipe(RouteInformation.fromRawNgRouteWithResourceInfo)
                  .pipe(_.toThrift))
                .toSet)))
      }.rescue {
        case _ => Future.value(Response(GetRoutesByProjectsResponse(status = false, routes = None)))
      }
  }
}
