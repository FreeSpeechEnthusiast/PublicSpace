package com.twitter.auth.customerauthtooling.api.components.routeinfoservice

import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.util.Future

/**
 * The interface exchanges endpoint information to route information or None if route is not recognized.
 */
trait RouteInformationServiceInterface {
  def checkEndpoint(
    endpoint: EndpointInfo,
    dpProviderName: Option[String] = None
  ): Future[Option[RouteInformation]]
}
