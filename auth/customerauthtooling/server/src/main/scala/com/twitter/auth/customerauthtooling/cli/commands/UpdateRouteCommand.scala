package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.DraftRouteRequest
import com.twitter.auth.customerauthtooling.thriftscala.GetRoutesByRouteIdsResponse
import com.twitter.util.Await
import com.twitter.util.Future
import picocli.CommandLine.ArgGroup
import picocli.CommandLine.Command
import picocli.CommandLine.Help.Ansi
import picocli.CommandLine.Mixin

@Command(
  name = "update",
  description = Array("Update existing ngroute for customer auth adoption"),
  mixinStandardHelpOptions = true,
  defaultValueProvider = classOf[RouteDefaultParametersProvider])
class UpdateRouteCommand extends DraftRouteCommands {

  @Mixin protected[commands] var commonButNotRequiredForUpdateOptions: CommonButNotRequiredForUpdateOptions =
    _

  @ArgGroup(exclusive = true, multiplicity = "1")
  var idOrPath: IdOrPathIdentifiers = _

  override def call(): Unit = {
    val routePathOpt = Option(idOrPath.pathDetails).map(_.routePath)
    val clusterOpt = Option(idOrPath.pathDetails).map(_.routeCluster)
    val methodOpt = Option(idOrPath.pathDetails).flatMap(_.method)
    Await.result {
      getExistingRoute(
        id = idOrPath.id,
        pathOpt = routePathOpt,
        clusterOpt = clusterOpt,
        methodOpt = methodOpt
      ).flatMap {
          case GetRoutesByRouteIdsResponse(status, Some(routes)) if status && routes.nonEmpty =>
            routes.toSeq match {
              case Seq(existingRoute) =>
                customerAuthToolingService
                  .draftRoute(request = DraftRouteRequest(
                    // merge route properties with command input
                    routeInfo = mergeInputWithExistingRoute(
                      existingRoute = existingRoute,
                      domains = Option(commonButNotRequiredForUpdateOptions.domains),
                      authTypes = Option(commonButNotRequiredForUpdateOptions.authTypes),
                      pathOpt = routePathOpt,
                      clusterOpt = clusterOpt,
                      methodOpt = methodOpt,
                      projectOpt = Option(commonButNotRequiredForUpdateOptions.project)
                    ),
                    automaticDecider = commonOptions.automaticDecider,
                    update = Some(true)
                  )).map {
                    _.routeDraft match {
                      case Some(r) =>
                        println(
                          s"Route is updated, uuid is: ${r.uuid}, route id is ${r.expectedRouteId}")
                      case None =>
                        println(Ansi.AUTO.string("@|bold,red Unable to update the route!|@"))
                    }
                  }
              case _ =>
                // more than one route returned, should never happen
                println(Ansi.AUTO.string("@|bold,red More than one routes found!|@"))
                Future.Unit
            }
          case _ =>
            // route doesn't exist
            println(s"Route doesn't exist")
            Future.Unit
        }.rescue {
          case e: Exception =>
            println(Ansi.AUTO.string("@|bold,red Warning! Exception received!|@"))
            println(Ansi.AUTO.string("@|italic,yellow " + e.getMessage + "|@"))
            Future.Unit
        }
    }
  }

}
