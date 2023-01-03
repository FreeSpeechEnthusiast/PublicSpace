package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.DraftRouteRequest
import com.twitter.auth.customerauthtooling.thriftscala.DraftRouteResponse
import com.twitter.auth.customerauthtooling.thriftscala.GetRoutesByRouteIdsRequest
import com.twitter.auth.customerauthtooling.thriftscala.{RequestMethod => TRequestMethod}
import com.twitter.auth.customerauthtooling.thriftscala.RouteInfo
import com.twitter.auth.customerauthtooling.cli.commands.converters.AuthTypeSetWrapper
import com.twitter.auth.customerauthtooling.cli.commands.converters.StringSetWrapper
import com.twitter.util.Future
import picocli.CommandLine.Mixin

abstract class DraftRouteCommands() extends BaseCustomerAuthToolingCommand with RouteBuilder {

  @Mixin protected[commands] var commonOptions: CommonOptions = _

  protected def buildRouteFromCommonOptions(
    domains: StringSetWrapper,
    authTypes: AuthTypeSetWrapper,
    path: String,
    cluster: String,
    method: Option[TRequestMethod],
    project: String
  ): RouteInfo = {
    buildRouteFromOptions(
      domains = domains,
      authTypes = authTypes,
      path = path,
      cluster = cluster,
      method = method,
      project = project,
      dps = commonOptions.dps,
      userRoles = commonOptions.userRoles,
      routeFlags = commonOptions.routeFlags,
      featurePermissions = commonOptions.featurePermissions,
      subscriptionPermissions = commonOptions.subscriptionPermissions,
      routeTags = commonOptions.routeTags,
      uaTags = commonOptions.uaTags,
      scopes = commonOptions.scopes,
      decider = commonOptions.decider,
      ldapOwners = commonOptions.ldapOwners,
      priority = commonOptions.priority,
      rateLimit = Some(commonOptions.rateLimit),
      timeoutMs = commonOptions.timeoutMs,
      experimentBuckets = commonOptions.experimentBuckets
    )
  }

  private[commands] def draftRouteResult(
    project: String,
    domains: StringSetWrapper,
    authTypes: AuthTypeSetWrapper,
    path: String,
    cluster: String,
    method: Option[TRequestMethod]
  ): Future[DraftRouteResponse] = {
    customerAuthToolingService
      .draftRoute(request = DraftRouteRequest(
        routeInfo = buildRouteFromCommonOptions(
          domains = domains,
          authTypes = authTypes,
          path = path,
          cluster = cluster,
          method = method,
          project = project).copy(
          id = None
        ),
        automaticDecider = commonOptions.automaticDecider,
        update = Some(false)
      ))
  }

  private[commands] def getExistingRoute(
    id: Option[String],
    pathOpt: Option[String],
    clusterOpt: Option[String],
    methodOpt: Option[TRequestMethod]
  ) = {
    customerAuthToolingService.getRoutesByRouteIds(
      GetRoutesByRouteIdsRequest(routeIds = Set(
        routeIdBasedOnProvidedIdentifiers(
          id = id,
          path = pathOpt,
          cluster = clusterOpt,
          method = methodOpt))))
  }

  private[commands] def mergeInputWithExistingRoute(
    existingRoute: RouteInfo,
    domains: Option[StringSetWrapper],
    authTypes: Option[AuthTypeSetWrapper],
    pathOpt: Option[String],
    clusterOpt: Option[String],
    methodOpt: Option[TRequestMethod],
    projectOpt: Option[String]
  ): RouteInfo = {
    val pr = commandParseResult
    // generate route from input
    val routeFromInput = buildRouteFromCommonOptions(
      // we can omit this values if they are not provided
      // due to validation they either have a value or hasMatchedOption for them is false
      domains = domains.getOrElse(StringSetWrapper(Set())),
      authTypes = authTypes.getOrElse(AuthTypeSetWrapper(Set())),
      path = pathOpt.getOrElse(""),
      cluster = clusterOpt.getOrElse(""),
      method = methodOpt,
      project = projectOpt.getOrElse(""),
    )
    // merge route properties with command input
    existingRoute.copy(
      projectId =
        if (pr.hasMatchedOption("project")) routeFromInput.projectId
        else existingRoute.projectId,
      method =
        if (pr.hasMatchedOption("method")) routeFromInput.method
        else existingRoute.method,
      path =
        if (pr.hasMatchedOption("path")) routeFromInput.path
        else existingRoute.path,
      cluster =
        if (pr.hasMatchedOption("cluster")) routeFromInput.cluster
        else existingRoute.cluster,
      domains =
        if (pr.hasMatchedOption("domains")) routeFromInput.domains
        else existingRoute.domains,
      authTypes =
        if (pr.hasMatchedOption("auth_types")) routeFromInput.authTypes
        else existingRoute.authTypes,
      requiredDps =
        if (pr.hasMatchedOption("dps")) routeFromInput.requiredDps
        else existingRoute.requiredDps,
      userRoles =
        if (pr.hasMatchedOption("userRoles")) routeFromInput.userRoles
        else existingRoute.userRoles,
      routeFlags =
        if (pr.hasMatchedOption("flags")) routeFromInput.routeFlags
        else existingRoute.routeFlags,
      featurePermissions =
        if (pr.hasMatchedOption("fps")) routeFromInput.featurePermissions
        else existingRoute.featurePermissions,
      subscriptionPermissions =
        if (pr.hasMatchedOption("sps")) routeFromInput.subscriptionPermissions
        else existingRoute.subscriptionPermissions,
      decider =
        if (pr.hasMatchedOption("decider")) routeFromInput.decider
        else existingRoute.decider,
      priority =
        if (pr.hasMatchedOption("priority")) routeFromInput.priority
        else existingRoute.priority,
      tags =
        if (pr.hasMatchedOption("tags")) routeFromInput.tags
        else existingRoute.tags,
      experimentBuckets =
        if (pr.hasMatchedOption("experiment_buckets"))
          routeFromInput.experimentBuckets
        else existingRoute.experimentBuckets,
      uaTags =
        if (pr.hasMatchedOption("ua_tags")) routeFromInput.uaTags
        else existingRoute.uaTags,
      scopes =
        if (pr.hasMatchedOption("scopes")) routeFromInput.scopes
        else existingRoute.scopes,
      rateLimit =
        if (pr.hasMatchedOption("rate_limit")) routeFromInput.rateLimit
        else existingRoute.rateLimit,
      timeoutMs =
        if (pr.hasMatchedOption("timeout_ms")) routeFromInput.timeoutMs
        else existingRoute.timeoutMs,
      ldapOwners =
        if (pr.hasMatchedOption("ldap_owners")) routeFromInput.ldapOwners
        else existingRoute.ldapOwners,
    )
  }

}
