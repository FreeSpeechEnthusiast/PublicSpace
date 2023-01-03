package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.customerauthtooling.api.components.parameterresolver.AdoptionParameterResolverInterface
import com.twitter.auth.customerauthtooling.api.components.parameterresolver.Oauth1OrSessionResolver
import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.inject.TwitterModule

object Oauth1OrSessionResolverModule extends TwitterModule {
  @Provides
  @Singleton
  def providesOauth1OrSessionChecker(
    routeInformationService: RouteInformationServiceInterface
  ): AdoptionParameterResolverInterface[AdoptionParameter.Oauth1OrSession] = {
    Oauth1OrSessionResolver(routeInformationService = routeInformationService)
  }
}
