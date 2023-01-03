package com.twitter.auth.customerauthtooling.api.controllers

import com.twitter.auth.customerauthtooling.api.services.AdoptionStatsService
import com.twitter.finatra.thrift.Controller
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.ApplyRoute
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.ApplyRoutes
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.GenerateAdoptionStats
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.CheckAdoptionStatus
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.DraftRoute
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.DraftRouteFromEndpoint
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.GetRoutesByRouteIds
import com.twitter.auth.customerauthtooling.api.services.AdoptionCheckerThriftService
import com.twitter.auth.customerauthtooling.api.services.EndpointToRouteDrafterThriftService
import com.twitter.auth.customerauthtooling.api.services.RouteDrafterThriftService
import com.twitter.auth.customerauthtooling.api.services.RouteApplyThriftService
import com.twitter.auth.customerauthtooling.api.services.RouteBatchApplyThriftService
import com.twitter.auth.customerauthtooling.api.services.RouteByProjectRetrieverThriftService
import com.twitter.auth.customerauthtooling.api.services.RouteRetrieverThriftService
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.GetRoutesByProjects
import javax.inject.Inject

class CustomerAuthToolingServiceThriftController @Inject() (
  adoptionStatsService: AdoptionStatsService,
  adoptionCheckerThriftService: AdoptionCheckerThriftService,
  routeDrafterThriftService: RouteDrafterThriftService,
  routeApplyThriftService: RouteApplyThriftService,
  routeBatchApplyThriftService: RouteBatchApplyThriftService,
  endpointToRouteDrafterThriftService: EndpointToRouteDrafterThriftService,
  routeRetrieverThriftService: RouteRetrieverThriftService,
  routeByProjectRetrieverThriftService: RouteByProjectRetrieverThriftService)
    extends Controller(CustomerAuthToolingService) {

  handle(GenerateAdoptionStats).withService(
    adoptionStatsService
  )

  /**
   * Checks customer auth DP adoption status
   */
  handle(CheckAdoptionStatus).withService(
    adoptionCheckerThriftService
  )

  /**
   * Drafts a route based on user defined route properties
   */
  handle(DraftRoute).withService(
    routeDrafterThriftService
  )

  /**
   * Drafts a route if the route doesn't exist or update a route if the route exists
   */
  handle(ApplyRoute).withService(
    routeApplyThriftService
  )

  /**
   * Drafts non existing routes and updates existing routes
   */
  handle(ApplyRoutes).withService(
    routeBatchApplyThriftService
  )

  /**
   * Drafts a route from actual endpoint. The main purpose is non-ng route migration.
   * A customer just need to provide a existing endpoint URL and method
   */
  handle(DraftRouteFromEndpoint).withService(
    endpointToRouteDrafterThriftService
  )

  /**
   * Retrieves routes by route ids
   */
  handle(GetRoutesByRouteIds).withService(
    routeRetrieverThriftService
  )

  /**
   * Retrieves routes by project id
   */
  handle(GetRoutesByProjects).withService(
    routeByProjectRetrieverThriftService
  )

}
