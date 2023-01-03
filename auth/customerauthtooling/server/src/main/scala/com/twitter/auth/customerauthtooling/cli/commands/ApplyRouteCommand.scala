package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.AppliedAction
import com.twitter.auth.customerauthtooling.thriftscala.ApplyRouteRequest
import com.twitter.util.Await
import com.twitter.util.Future
import picocli.CommandLine.ArgGroup
import picocli.CommandLine.Command
import picocli.CommandLine.Help.Ansi
import picocli.CommandLine.Mixin
import com.twitter.auth.customerauthtooling.thriftscala.RouteDraft

@Command(
  name = "apply",
  description = Array("Apply changes to a route if route exists or else create a new route"),
  mixinStandardHelpOptions = true,
  defaultValueProvider = classOf[RouteDefaultParametersProvider])
class ApplyRouteCommand extends DraftRouteCommands() {

  @Mixin protected[commands] var commonButNotRequiredForUpdateOptions: CommonButNotRequiredForUpdateOptions =
    _

  @ArgGroup(exclusive = true, multiplicity = "1")
  var idOrPath: IdOrPathIdentifiers = _

  override def call(): Unit = {
    val routePathOpt = Option(idOrPath.pathDetails).map(_.routePath)
    val clusterOpt = Option(idOrPath.pathDetails).map(_.routeCluster)
    val methodOpt = Option(idOrPath.pathDetails).flatMap(_.method)
    val pr = commandParseResult

    Await.result {
      customerAuthToolingService
        .applyRoute(ApplyRouteRequest(
          routeInfo = buildPartialRouteFromOptions(
            path = if (pr.hasMatchedOption("path")) routePathOpt else None,
            domains =
              if (pr.hasMatchedOption("domains"))
                Option(commonButNotRequiredForUpdateOptions.domains)
              else None,
            cluster = if (pr.hasMatchedOption("cluster")) clusterOpt else None,
            id = if (pr.hasMatchedOption("id")) idOrPath.id else None,
            project =
              if (pr.hasMatchedOption("project"))
                Option(commonButNotRequiredForUpdateOptions.project)
              else None,
            method = if (pr.hasMatchedOption("method")) methodOpt else None,
            authTypes =
              if (pr.hasMatchedOption("auth_types"))
                Option(commonButNotRequiredForUpdateOptions.authTypes)
              else None,
            dps =
              if (pr.hasMatchedOption("dps")) commonOptions.dps
              else None,
            userRoles =
              if (pr.hasMatchedOption("user_roles")) commonOptions.userRoles
              else None,
            routeFlags =
              if (pr.hasMatchedOption("flags")) commonOptions.routeFlags
              else None,
            featurePermissions =
              if (pr.hasMatchedOption("fps")) commonOptions.featurePermissions
              else None,
            subscriptionPermissions =
              if (pr.hasMatchedOption("sps")) commonOptions.subscriptionPermissions
              else None,
            decider =
              if (pr.hasMatchedOption("decider")) commonOptions.decider
              else None,
            priority =
              if (pr.hasMatchedOption("priority")) commonOptions.priority
              else None,
            routeTags =
              if (pr.hasMatchedOption("tags")) commonOptions.routeTags
              else None,
            experimentBuckets =
              if (pr.hasMatchedOption("experiment_buckets"))
                commonOptions.experimentBuckets
              else None,
            uaTags =
              if (pr.hasMatchedOption("ua_tags")) commonOptions.uaTags
              else None,
            scopes =
              if (pr.hasMatchedOption("scopes")) commonOptions.scopes
              else None,
            rateLimit =
              if (pr.hasMatchedOption("rate_limit")) Option(commonOptions.rateLimit)
              else None,
            timeoutMs =
              if (pr.hasMatchedOption("timeout_ms")) commonOptions.timeoutMs
              else None,
            ldapOwners =
              if (pr.hasMatchedOption("ldap_owners")) commonOptions.ldapOwners
              else None,
          ),
          automaticDecider = commonOptions.automaticDecider,
        )).map {
          _.routeDraft match {
            case Some(RouteDraft(uuid, routeId, Some(action))) if action == AppliedAction.Update =>
              println(s"Route was updated, uuid is: ${uuid}, route id is ${routeId}")
            case Some(RouteDraft(uuid, routeId, Some(action))) if action == AppliedAction.Insert =>
              println(s"Route was created, uuid is: ${uuid}, route id is ${routeId}")
            case Some(RouteDraft(uuid, routeId, Some(action))) if action == AppliedAction.Nothing =>
              println(s"Route was untouched, uuid is: ${uuid}, route id is ${routeId}")
            case _ =>
              println(Ansi.AUTO.string("@|bold,red Unable to apply the route!|@"))
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
