package com.twitter.auth.customerauthtooling.api.components

import com.twitter.auth.customerauthtooling.api.components.dpprovider.DpProviderInterface
import com.twitter.auth.customerauthtooling.api.components.dpprovider.SupportedDpProviders
import com.twitter.auth.customerauthtooling.api.components.pacmanngroutestorage.PacmanNgRouteStorageServiceInterface
import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.util.Future
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RouteInformationService should utilize TFE ConfigBus for NgRoutes and
 * TFE classes for legacy routes to match endpoint information to TFE routes.
 * RouteInformation is being used by adoption checkers
 */
@Singleton
private[api] case class RouteInformationService @Inject() (
  ngRouteStorage: PacmanNgRouteStorageServiceInterface,
  namedDpProviders: Map[SupportedDpProviders.Value, DpProviderInterface])
    extends RouteInformationServiceInterface {
  override def checkEndpoint(
    endpoint: EndpointInfo,
    dpProviderName: Option[String] = None
  ): Future[Option[RouteInformation]] = {
    // TODO (AUTHPLT-2642): implement RouteInformationService for ng routes using ngRouteStorage
    // TODO (AUTHPLT-2643): implement RouteInformationService for legacy routes
    // TODO: use dpProviderService to inject dps
    // Temporary we are using None that means unable to match endpoint with any route
    Future.None
  }
}
