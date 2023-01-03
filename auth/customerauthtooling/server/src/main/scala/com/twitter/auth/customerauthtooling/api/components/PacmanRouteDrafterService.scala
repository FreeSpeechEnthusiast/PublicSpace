package com.twitter.auth.customerauthtooling.api.components

import com.twitter.cim.pacman.plugin.tferoute.models.CreateOrUpdateRouteRequest
import com.twitter.auth.customerauthtooling.api.components.pacmanngroutestorage.PacmanNgRouteStorageServiceInterface
import com.twitter.auth.customerauthtooling.api.components.routedrafter.RouteDrafterServiceInterface
import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.auth.customerauthtooling.api.models.BatchRouteDraft
import com.twitter.auth.customerauthtooling.api.models.PartialRouteInformation
import com.twitter.auth.customerauthtooling.api.models.RequestMethod.RequestMethod
import com.twitter.auth.customerauthtooling.api.models.RouteDraft
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.util.Future
import javax.inject.Inject
import javax.inject.Singleton
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.customerauthtooling.api.models.RouteAppliedAction
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Return
import com.twitter.util.Throw

@Singleton
final case class PacmanRouteDrafterService @Inject() (
  private val ngRouteStorage: PacmanNgRouteStorageServiceInterface,
  override protected val routeInformationService: RouteInformationServiceInterface,
  private val statsReceiver: StatsReceiver,
  private val logger: JsonLogger)
    extends RouteDrafterServiceInterface {

  val Scope: String = this.getClass.getSimpleName
  private val loggerScope = logger.withScope(Scope)
  private val statsScope = statsReceiver.scope(Scope)

  private[components] val UnableToLoadRoutes = "unableToLoadRoutes"
  private[components] val ImportStopped = "importStopped"
  private[components] val ImportStoppedDuringValidation = "importStoppedDuringValidation"
  private[components] val ImportFinished = "importFinished"
  private[components] val NoRoutes = "noRoutes"

  private[components] val UnableToLoadRoutesCounter =
    statsScope.counter(UnableToLoadRoutes)
  private[components] val ImportStoppedCounter =
    statsScope.counter(ImportStopped)
  private[components] val ImportStoppedDuringValidationCounter =
    statsScope.counter(ImportStoppedDuringValidation)
  private[components] val ImportFinishedCounter =
    statsScope.counter(ImportFinished)
  private[components] val NoRoutesCounter =
    statsScope.counter(NoRoutes)

  private val MaxSampleSize = 5

  class UnknownProjectIdException(
    routePath: String,
    routeCluster: String,
    routeMethod: RequestMethod)
      extends Exception

  override def draftRoute(
    route: RouteInformation,
    update: Boolean = false,
    automaticDecider: Boolean = true
  ): Future[Option[RouteDraft]] = {
    val request = route.projectId match {
      case Some(projectId) =>
        CreateOrUpdateRouteRequest(
          route =
            prepareRoute(route = route, project = projectId, automaticDecider = automaticDecider)
              .toRawNgRoute(),
          project = projectId,
          requestReason = "",
          // selfManagedRoute = true means route will require a peer approval
          selfManagedRoute = false
        )
      case None =>
        throw new UnknownProjectIdException(
          routePath = route.path,
          routeCluster = route.cluster,
          routeMethod = route.method)
    }
    (if (update) {
       ngRouteStorage.updateRoute(request = request)
     } else {
       ngRouteStorage.createRoute(request = request)
     }).map { r =>
      r.uuid match {
        // request was unsuccessful, possibly incorrect input or route already exists
        case "" => None
        // request was successful
        case _ =>
          Some(
            RouteDraft(
              uuid = r.uuid,
              expectedRouteId = route.expectedNgRouteId,
              action =
                if (update) Some(RouteAppliedAction.Update) else Some(RouteAppliedAction.Insert)))
      }
    }
  }

  /**
   * NGRoutes defines a decider for each endpoint, this function builds a unique decider name
   * for a path with format /a/{b}/c -> project-method-a-b-c
   *
   * @param project
   * @param method
   * @param path
   * @param cluster
   * @return
   */
  private def deciderForRoute(
    project: String,
    method: String,
    path: String
  ): String = {
    s"${project}-${method.toLowerCase()}-${path.split('/').filter(_ != "").mkString("-")}"
      .replaceAll("\\{", "")
      .replaceAll("}", "")
  }

  /**
   * Wraps extra information to detected or user provided route information
   *
   * @param route
   * @param project
   * @param automaticDecider
   * @return
   */
  private def prepareRoute(
    route: RouteInformation,
    project: String,
    automaticDecider: Boolean = true
  ): RouteInformation = {
    // gather decider information
    val decider = route.decider match {
      case None if automaticDecider =>
        Some(deciderForRoute(project = project, method = route.method.toString, path = route.path))
      case _ => route.decider
    }
    // wrap gathered information
    route.copy(decider = decider)
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
  override def applyRoutes(
    routes: Set[PartialRouteInformation],
    automaticDecider: Boolean,
    ignoreInvalid: Boolean,
    ignoreErrors: Boolean
  ): Future[BatchRouteDraft] = {
    // validate provided partial route information
    val invalidRecords =
      routes.filter(r => r.providedOrGeneratedRouteId.isEmpty)
    val hasInvalid = invalidRecords.nonEmpty
    if (hasInvalid) {
      loggerScope.info(
        message = "Some records are invalid",
        metadata = Some(
          Map(
            "routes_count" -> routes.size,
            "invalid_records_count" -> invalidRecords.size,
            "invalid_records_sample" -> invalidRecords.slice(0, MaxSampleSize)
          ))
      )
    }
    val ignoredInvalidRoutesSize = invalidRecords.size
    if (hasInvalid && !ignoreInvalid) {
      ImportStoppedDuringValidationCounter.incr()
      return Future.value(
        BatchRouteDraft.Empty.copy(ignoredInvalid = ignoredInvalidRoutesSize, wasStopped = true))
    }
    // collect valid records and transform them to Map(routeId -> routeInfo)
    val routesMap = routes.collect {
      case route if route.providedOrGeneratedRouteId.isDefined =>
        (route.providedOrGeneratedRouteId.get, route)
    }.toMap
    if (routesMap.isEmpty) {
      // log if there is no input
      NoRoutesCounter.incr()
      return Future.value(
        BatchRouteDraft.Empty.copy(
          ignoredInvalid = ignoredInvalidRoutesSize,
          warnings = Some(Seq("No routes provided"))))
    }
    ngRouteStorage
      .getRoutesByIds(routeIds = routesMap.keySet).transform {
        case Throw(e) =>
          UnableToLoadRoutesCounter.incr()
          loggerScope.warning(
            message = "Unable to load routes from pacman",
            metadata = Some(
              Map(
                "routes_count" -> routes.size,
                "invalid_records_count" -> invalidRecords.size,
                "invalid_records_sample" -> invalidRecords.slice(0, MaxSampleSize),
                "exception" -> e.getMessage
              ))
          )
          Future.value(
            BatchRouteDraft.Empty.copy(
              ignoredInvalid = ignoredInvalidRoutesSize,
              errors = Some(Seq("Unable to load routes from pacman"))))
        case Return(thriftRoutesWithResourceInformation) =>
          val routes =
            thriftRoutesWithResourceInformation
              .map(RouteInformation.fromRawNgRouteWithResourceInfo).groupBy(_.id)
          // remap existing routes to Map(routeId -> routeInfo)
          val existingRoutesMap = routes.collect {
            // if there is one route returned use it (for example, if we are going to make draft change for production route)
            case (Some(routeId), List(r)) => (routeId, r)
            // if there are many routes returned use draft or canary or production
            case (Some(routeId), listOfRoutes) =>
              (
                routeId,
                listOfRoutes
                  .find(_.lifeCycle == "draft").getOrElse(
                    listOfRoutes
                      .find(_.lifeCycle == "canary").getOrElse(listOfRoutes
                        .filter(_.lifeCycle == "production").head)))
          }
          if (existingRoutesMap.size != routes.size) {
            loggerScope.warning(
              message = "Some loaded routes don't have route id",
              metadata = Some(
                Map(
                  "routes_count" -> routes.size,
                  "existing_routes_count" -> routes.size,
                  "existing_routes_sample" -> routes(None).slice(0, MaxSampleSize)
                ))
            )
            return Future.value(
              BatchRouteDraft.Empty.copy(
                ignoredInvalid = ignoredInvalidRoutesSize,
                errors = Some(Seq("Some loaded routes don't have route id"))))
          }
          PacmanBatchRouteApply(
            routeDrafter = this,
            automaticDecider = automaticDecider,
            ignoreInvalid = ignoreInvalid,
            ignoreErrors = ignoreErrors,
            logger = loggerScope)(
            routeChanges = routesMap,
            existingRoutes = existingRoutesMap
          )
          // append pre-validation info
            .onSuccess { d =>
              if (d.wasStopped) {
                ImportStoppedCounter.incr()
                loggerScope.warning(
                  message = "Batch import stopped",
                  metadata = Some(
                    Map(
                      "routes_count" -> routes.size,
                      "routes_sample" -> routes.slice(0, MaxSampleSize),
                      "results" -> d
                    ))
                )
              } else {
                ImportFinishedCounter.incr()
                loggerScope.info(
                  message = "Batch import finished",
                  metadata = Some(
                    Map(
                      "routes_count" -> routes.size,
                      "routes_sample" -> routes.slice(0, MaxSampleSize),
                      "results" -> d
                    ))
                )
              }
            }
            .map(d => d.copy(ignoredInvalid = d.ignoredInvalid + ignoredInvalidRoutesSize))
      }
  }
}
