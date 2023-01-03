package com.twitter.auth.customerauthtooling.api.components.routedrafter

import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.auth.customerauthtooling.api.models.BatchRouteDraft
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.auth.customerauthtooling.api.models.PartialRouteInformation
import com.twitter.auth.customerauthtooling.api.models.RouteDraft
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.util.Future

trait RouteDrafterServiceInterface {

  protected val routeInformationService: RouteInformationServiceInterface

  /**
   * Draft a route based on endpoint information (simple info provided by user such as endpoint url).
   *
   * @param endpoint Endpoint parameters
   * @param project KITE project for the route
   * @param update If update param is true, update the existing route
   * @param dpProviderName If set, name of data permission provider to use
   * @param automaticDecider Generate a decider name if decider is not provided
   * @return
   */
  final def draftRouteFromEndpoint(
    endpoint: EndpointInfo,
    project: String,
    update: Boolean = false,
    dpProviderName: Option[String] = None,
    automaticDecider: Boolean = true
  ): Future[Option[RouteDraft]] = {
    routeInformationService
      .checkEndpoint(endpoint = endpoint, dpProviderName = dpProviderName).flatMap {
        // route information successfully obtained
        case Some(info) =>
          draftRoute(route = info, update = update, automaticDecider = automaticDecider)
        // there were not enough route information detected
        case None => Future.value(Option.empty)
      }
  }

  /**
   * Draft a route based on route information.
   *
   * @param route Route parameters
   * @param project KITE project for the route
   * @param update If update param is true, update the existing route
   * @param automaticDecider Generate a decider name if decider is not provided
   * @return
   */
  def draftRoute(
    route: RouteInformation,
    update: Boolean = false,
    automaticDecider: Boolean = true
  ): Future[Option[RouteDraft]]

  /**
   * Drafts a route if the route doesn't exist or update a route if the route exists
   *
   * @param route Route parameters
   * @param automaticDecider Generate a decider name if decider is not provided
   * @return
   */
  final def applyRoute(
    route: PartialRouteInformation,
    automaticDecider: Boolean = true
  ): Future[Option[RouteDraft]] = {
    applyRoutes(
      routes = Set(route),
      automaticDecider = automaticDecider,
      ignoreInvalid = false,
      ignoreErrors = false).map(_.routeDrafts.flatMap(_.headOption))
  }

  /**
   * Drafts non existing routes and updates existing routes
   *
   * @param routes Route parameters
   * @param automaticDecider Generate a decider name if decider is not provided
   * @param ignoreInvalid If set to false job will stop on first invalid route
   * @param ignoreErrors If set to false job will stop on first error
   * @return
   */
  def applyRoutes(
    routes: Set[PartialRouteInformation],
    automaticDecider: Boolean,
    ignoreInvalid: Boolean,
    ignoreErrors: Boolean
  ): Future[BatchRouteDraft]

}
