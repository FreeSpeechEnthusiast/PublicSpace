package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.customerauthtooling.api.components.parameterresolver.AdoptionParameterResolverInterface
import com.twitter.auth.customerauthtooling.api.components.parameterresolver.IsInternalEndpointResolver
import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.inject.TwitterModule

object IsInternalEndpointResolverModule extends TwitterModule {
  @Provides
  @Singleton
  def providesIsInternalEndpointChecker(
    routeInformationService: RouteInformationServiceInterface
  ): AdoptionParameterResolverInterface[AdoptionParameter.IsInternalEndpoint] = {
    IsInternalEndpointResolver(routeInformationService = routeInformationService)
  }
}
