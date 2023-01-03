package com.twitter.auth.customerauthtooling.api.components.parameterresolver

import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import javax.inject.Singleton
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameterValue
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.util.Future
import javax.inject.Inject

@Singleton
final case class FoundInTfeResolver @Inject() (
  routeInformationService: RouteInformationServiceInterface)
    extends AdoptionParameterResolverInterface[AdoptionParameter.FoundInTfe] {
  protected def check(
    endpoint: EndpointInfo
  ): Future[Option[AdoptionParameterValue[AdoptionParameter.FoundInTfe]]] = {
    routeInformationService.checkEndpoint(endpoint = endpoint).map {
      case Some(_) => Some(AdoptionParameterValue(true))
      case None => Some(AdoptionParameterValue(false))
    }
  }
}
