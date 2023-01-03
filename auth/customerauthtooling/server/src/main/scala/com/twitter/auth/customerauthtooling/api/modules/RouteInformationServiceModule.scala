package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.customerauthtooling.api.components.RouteInformationService
import com.twitter.auth.customerauthtooling.api.components.dpprovider.DpProviderInterface
import com.twitter.auth.customerauthtooling.api.components.dpprovider.SupportedDpProviders
import com.twitter.auth.customerauthtooling.api.components.pacmanngroutestorage.PacmanNgRouteStorageServiceInterface
import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.inject.TwitterModule

object RouteInformationServiceModule extends TwitterModule {
  @Provides
  @Singleton
  def providesRouteInformationService(
    ngRouteStorage: PacmanNgRouteStorageServiceInterface,
    namedDpProviders: Map[SupportedDpProviders.Value, DpProviderInterface]
  ): RouteInformationServiceInterface = {
    RouteInformationService(ngRouteStorage = ngRouteStorage, namedDpProviders = namedDpProviders)
  }
}
