package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.customerauthtooling.api.components.PacmanRouteDrafterService
import com.twitter.auth.customerauthtooling.api.components.pacmanngroutestorage.PacmanNgRouteStorageServiceInterface
import com.twitter.auth.customerauthtooling.api.components.routedrafter.RouteDrafterServiceInterface
import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.TwitterModule

object RouteDrafterServiceModule extends TwitterModule {
  @Provides
  @Singleton
  def providesRouteDrafterService(
    ngRouteStorage: PacmanNgRouteStorageServiceInterface,
    routeInformationService: RouteInformationServiceInterface,
    statsReceiver: StatsReceiver,
    logger: JsonLogger
  ): RouteDrafterServiceInterface = {
    PacmanRouteDrafterService(
      ngRouteStorage: PacmanNgRouteStorageServiceInterface,
      routeInformationService = routeInformationService,
      statsReceiver = statsReceiver,
      logger = logger)
  }
}
