package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.customerauthtooling.api.components.AdoptionCheckerService
import com.twitter.auth.customerauthtooling.api.components.adoptionchecker.AdoptionCheckerServiceInterface
import com.twitter.auth.customerauthtooling.api.components.parameterresolver.AdoptionParameterResolverInterface
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.inject.TwitterModule

object AdoptionCheckerServiceModule extends TwitterModule {
  @Provides
  @Singleton
  def providesAdoptionCheckerService(
    foundInTfeResolver: AdoptionParameterResolverInterface[
      AdoptionParameter.FoundInTfe
    ],
    isInternalEndpointResolver: AdoptionParameterResolverInterface[
      AdoptionParameter.IsInternalEndpoint
    ],
    isNgRouteResolver: AdoptionParameterResolverInterface[
      AdoptionParameter.IsNgRoute
    ],
    requiresAuthResolver: AdoptionParameterResolverInterface[
      AdoptionParameter.RequiresAuth
    ],
    isAppOnlyOrGuestResolver: AdoptionParameterResolverInterface[
      AdoptionParameter.IsAppOnlyOrGuest
    ],
    oauth1OrSessionResolver: AdoptionParameterResolverInterface[
      AdoptionParameter.Oauth1OrSession
    ],
    alreadyAdoptedDpsResolver: AdoptionParameterResolverInterface[
      AdoptionParameter.AlreadyAdoptedDps
    ],
  ): AdoptionCheckerServiceInterface = {
    AdoptionCheckerService(
      foundInTfeResolver = foundInTfeResolver,
      isInternalEndpointResolver = isInternalEndpointResolver,
      isNgRouteResolver = isNgRouteResolver,
      requiresAuthResolver = requiresAuthResolver,
      isAppOnlyOrGuestResolver = isAppOnlyOrGuestResolver,
      oauth1OrSessionResolver = oauth1OrSessionResolver,
      alreadyAdoptedDpsResolver = alreadyAdoptedDpsResolver,
    )
  }
}
