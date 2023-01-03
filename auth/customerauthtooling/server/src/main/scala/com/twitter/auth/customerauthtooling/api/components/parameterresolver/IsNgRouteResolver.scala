package com.twitter.auth.customerauthtooling.api.components.parameterresolver

import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameterValue
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.util.Future
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
final case class IsNgRouteResolver @Inject() (
  routeInformationService: RouteInformationServiceInterface)
    extends AdoptionParameterResolverInterface[AdoptionParameter.IsNgRoute] {
  protected def check(
    endpoint: EndpointInfo
  ): Future[Option[AdoptionParameterValue[AdoptionParameter.IsNgRoute]]] = {
    routeInformationService.checkEndpoint(endpoint = endpoint).map {
      case Some(information) if information.isNgRoute => Some(AdoptionParameterValue(true))
      case Some(_) => Some(AdoptionParameterValue(false))
      // Unrecognized route gives unknown result
      case None => None
    }
  }
}
