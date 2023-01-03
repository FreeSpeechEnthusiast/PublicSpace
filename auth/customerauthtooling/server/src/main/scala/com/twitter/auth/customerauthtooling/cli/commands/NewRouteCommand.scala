package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.util.Await
import com.twitter.util.Future
import picocli.CommandLine.Command
import picocli.CommandLine.Help.Ansi
import picocli.CommandLine.Mixin

@Command(
  name = "create",
  description = Array("Draft new preconfigured ngroute for customer auth adoption"),
  mixinStandardHelpOptions = true,
  defaultValueProvider = classOf[RouteDefaultParametersProvider])
class NewRouteCommand extends DraftRouteCommands {

  @Mixin protected[commands] var commonRequiredOptions: CommonRequiredOptions = _
  @Mixin protected[commands] var identifyingOptions: IdentifyingPathDetails = _

  override def call(): Unit = {
    Await.result {
      Future
        .collect(List(draftRouteResult(
          project = commonRequiredOptions.project,
          domains = commonRequiredOptions.domains,
          authTypes = commonRequiredOptions.authTypes,
          path = identifyingOptions.routePath,
          cluster = identifyingOptions.routeCluster,
          method = identifyingOptions.method
        ))).map {
          case Seq(draftRouteResponse) =>
            draftRouteResponse.routeDraft match {
              case Some(r) =>
                println(s"Route is created, uuid is: ${r.uuid}, route id is ${r.expectedRouteId}")
              case None => println(s"Unable to draft the route. Route might already exists")
            }
        }.rescue {
          case e: Exception =>
            println(Ansi.AUTO.string("@|bold,red Warning! Exception received!|@"))
            println(Ansi.AUTO.string("@|italic,yellow " + e.getMessage + "|@"))
            Future.Unit
        }
    }
  }

}
