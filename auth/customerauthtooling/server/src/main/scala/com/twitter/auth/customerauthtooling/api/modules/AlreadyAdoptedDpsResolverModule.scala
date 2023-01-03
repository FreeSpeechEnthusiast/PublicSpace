package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.customerauthtooling.api.components.parameterresolver.AdoptionParameterResolverInterface
import com.twitter.auth.customerauthtooling.api.components.parameterresolver.AlreadyAdoptedDpsResolver
import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.inject.TwitterModule

object AlreadyAdoptedDpsResolverModule extends TwitterModule {
  @Provides
  @Singleton
  def providesAlreadyAdoptedDpsChecker(
    routeInformationService: RouteInformationServiceInterface
  ): AdoptionParameterResolverInterface[AdoptionParameter.AlreadyAdoptedDps] = {
    AlreadyAdoptedDpsResolver(routeInformationService = routeInformationService)
  }
}
