package com.twitter.auth.customerauthtooling.api.components

import com.twitter.auth.customerauthtooling.api.components.routedrafter.RouteDrafterServiceInterface
import com.twitter.auth.customerauthtooling.api.models.BatchRouteDraft
import com.twitter.auth.customerauthtooling.api.models.PartialRouteInformation
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.util.Future
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import scala.util.control.NonFatal
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.customerauthtooling.api.models.RouteAppliedAction
import com.twitter.auth.customerauthtooling.api.models.RouteDraft
import java.util.concurrent.atomic.AtomicReference

final case class PacmanBatchRouteApply(
  routeDrafter: RouteDrafterServiceInterface,
  automaticDecider: Boolean,
  ignoreInvalid: Boolean,
  ignoreErrors: Boolean,
  private val logger: JsonLogger) {

  val Scope: String = this.getClass.getSimpleName
  private val loggerScope = logger.withScope(Scope)

  // thread safe counters
  private val updatedRoutes = new AtomicInteger()
  private val insertedRoutes = new AtomicInteger()
  private val ignoredInvalidRoutes = new AtomicInteger()
  private val ignoredDueToErrorsRoutes = new AtomicInteger()
  private val unchangedRoutes = new AtomicInteger()

  // thread safe stop flag
  private val stopped = new AtomicBoolean()

  // thread safe messages
  private val messages = new AtomicReference(List.empty[String])
  private val errors = new AtomicReference(List.empty[String])
  private val warnings = new AtomicReference(List.empty[String])
  private def insertMsg(list: AtomicReference[List[String]], message: String): Unit = {
    while (true) {
      val oldList = list.get
      if (list.compareAndSet(oldList, message :: oldList))
        return
    }
  }

  private class JobExceptions() extends Exception
  private case class InputRelatedException(message: String) extends JobExceptions {
    override def getMessage() = message
  }

  def apply(
    routeChanges: Map[String, PartialRouteInformation],
    existingRoutes: Map[String, RouteInformation]
  ): Future[BatchRouteDraft] = {
    Future
      .collect(
        // routes to be updated
        routeChanges
          .filterKeys(existingRoutes.contains).map {
            case (routeId, routeChange) =>
              existingRoutes.get(routeId) match {
                case Some(existingRoute) =>
                  updateRoute(existingRoute = existingRoute, routeChange = routeChange)
                case None =>
                  // normally shouldn't happen because we check it using filterKeys
                  insertMsg(
                    list = warnings,
                    message = s"Unable to find a request route in a response")
                  Future.None
              }
            case _ =>
              // normally shouldn't happen because we check it using filterKeys
              Future.None
          }.toSeq ++
          // routes to be created
          routeChanges
            .filterKeys(!existingRoutes.contains(_)).map {
              case (routeId, newRoute) =>
                newRoute.toNewRoute match {
                  case Some(route) => insertRoute(route = route)
                  case None =>
                    handleExceptions {
                      Future.exception(InputRelatedException(
                        s"Unable to make a new route (${routeId}). Some required parameters are missing: auth_types, domains, path, cluster or project"))
                    }
                }
            }.toSeq).map { s =>
        BatchRouteDraft(
          updated = updatedRoutes.get(),
          inserted = insertedRoutes.get(),
          ignoredInvalid = ignoredInvalidRoutes.get(),
          ignoredDueToErrors = ignoredDueToErrorsRoutes.get(),
          unchanged = unchangedRoutes.get(),
          routeDrafts = Some(s.collect { case Some(d) => d }.toSet),
          wasStopped = stopped.get(),
          errors = Some(errors.get()),
          warnings = Some(warnings.get()),
          messages = Some(messages.get())
        )
      }.rescue {
        case _: CancellationException =>
          loggerScope.info(
            message = "Operation have stopped",
            metadata = None
          )
          insertMsg(
            list = warnings,
            message = "Operation have stopped due to invalid record or error")
          Future.value(
            BatchRouteDraft(
              updated = updatedRoutes.get(),
              inserted = insertedRoutes.get(),
              ignoredInvalid = ignoredInvalidRoutes.get(),
              ignoredDueToErrors = ignoredDueToErrorsRoutes.get(),
              unchanged = unchangedRoutes.get(),
              routeDrafts = None,
              wasStopped = stopped.get(),
              errors = Some(errors.get()),
              warnings = Some(warnings.get()),
              messages = Some(messages.get())
            ))
        case e: Exception =>
          loggerScope.info(
            message = "Operation have failed due to unknown exception",
            metadata = None
          )
          insertMsg(
            list = errors,
            message =
              s"Operation have failed due to unknown exception (${e.getClass.getSimpleName}): ${e.getMessage}")
          Future.value(
            BatchRouteDraft(
              updated = updatedRoutes.get(),
              inserted = insertedRoutes.get(),
              ignoredInvalid = ignoredInvalidRoutes.get(),
              ignoredDueToErrors = ignoredDueToErrorsRoutes.get(),
              unchanged = unchangedRoutes.get(),
              routeDrafts = None,
              wasStopped = stopped.get(),
              errors = Some(errors.get()),
              warnings = Some(warnings.get()),
              messages = Some(messages.get())
            ))
      }
  }

  private def updateRoute(existingRoute: RouteInformation, routeChange: PartialRouteInformation) = {
    handleExceptions {
      val updatedRoute = routeChange.toUpdatedRoute(existingRoute = existingRoute)
      if (updatedRoute != existingRoute) {
        routeDrafter
          .draftRoute(
            route = updatedRoute,
            update = true,
            automaticDecider = automaticDecider
          ).onSuccess {
            case Some(r) =>
              updatedRoutes.incrementAndGet()
              loggerScope.info(
                message = "Route was updated",
                metadata = Some(
                  Map(
                    "uuid" -> r.uuid,
                    "routeId" -> r.expectedRouteId
                  ))
              )
            case None =>
              Future.exception(
                new Exception(s"Unable to update the route (${existingRoute.expectedNgRouteId})"))
          }
      } else {
        // route is not changed
        unchangedRoutes.incrementAndGet()
        loggerScope.info(
          message = "Route wasn't changed",
          metadata = Some(
            Map(
              "routeId" -> existingRoute.expectedNgRouteId
            ))
        )
        Future.value(
          Some(
            RouteDraft(
              uuid = "n/a",
              expectedRouteId = existingRoute.expectedNgRouteId,
              action = Some(RouteAppliedAction.Nothing))))
      }
    }
  }

  private def insertRoute(route: RouteInformation) = {
    handleExceptions {
      routeDrafter
        .draftRoute(
          route = route,
          automaticDecider = automaticDecider,
          update = false
        ).onSuccess {
          case Some(r) =>
            insertedRoutes.incrementAndGet()
            loggerScope.info(
              message = "Route was created",
              metadata = Some(
                Map(
                  "uuid" -> r.uuid,
                  "routeId" -> r.expectedRouteId
                ))
            )
          case None =>
            Future.exception(
              new Exception(
                s"Unable to make a new route (${route.expectedNgRouteId}) due to api problem!"))
        }
    }
  }

  private def handleExceptions[T](f: Future[Option[T]]): Future[Option[T]] = {
    f.rescue {
      case NonFatal(e: InputRelatedException) =>
        ignoredInvalidRoutes.incrementAndGet()
        // just log messages if errors are ignorable
        if (ignoreInvalid) {
          insertMsg(list = warnings, message = e.getMessage())
          Future.None
        } else {
          // log the first non ignorable message and stop logging on other threads
          if (stopped.compareAndSet(false, true)) {
            insertMsg(list = errors, message = e.getMessage())
          }
          // cancel feature chain
          f.raise(e)
          // propagate the exception as CancellationException
          Future.exception(new CancellationException(e.getMessage()))
        }
      case NonFatal(e) =>
        ignoredDueToErrorsRoutes.incrementAndGet()
        // just log messages if errors are ignorable
        if (ignoreErrors) {
          insertMsg(list = warnings, message = e.getMessage)
          Future.None
        } else {
          // log the first non ignorable message and stop logging on other threads
          if (stopped.compareAndSet(false, true)) {
            insertMsg(list = errors, message = e.getMessage)
          }
          // cancel feature chain
          f.raise(e)
          // propagate the exception as CancellationException
          Future.exception(new CancellationException(e.getMessage))
        }
    }
  }

}
