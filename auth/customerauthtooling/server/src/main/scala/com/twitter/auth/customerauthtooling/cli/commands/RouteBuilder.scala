package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.DataPermissionAnnotation
import com.twitter.auth.customerauthtooling.thriftscala.PartialRouteInfo
import com.twitter.auth.customerauthtooling.thriftscala.RouteInfo
import com.twitter.auth.customerauthtooling.api.models.RequestMethod
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.auth.customerauthtooling.cli.commands.converters.AuthTypeSetWrapper
import com.twitter.auth.customerauthtooling.cli.commands.converters.StringSetWrapper
import com.twitter.auth.customerauthtooling.thriftscala.{RequestMethod => TRequestMethod}
import com.twitter.auth.customerauthtooling.api.models.RouteInformation.normalizeMethod
import com.twitter.auth.customerauthtooling.api.models.RouteInformation.normalizePath
import com.twitter.auth.customerauthtooling.cli.commands.converters.ExperimentBucketSetWrapper
import com.twitter.auth.customerauthtooling.cli.commands.converters.InvalidInputException
import com.twitter.auth.customerauthtooling.cli.commands.converters.LongSetWrapper
import picocli.CommandLine.Spec
import picocli.CommandLine.Model

trait RouteBuilder {
  @Spec var spec: Model.CommandSpec = _

  private[commands] def commandParseResult = spec.commandLine.getParseResult

  protected def automaticRouteId(path: String, cluster: String, method: Option[String]): String = {
    s"${normalizeMethod(method)}${normalizePath(path)}->cluster:$cluster"
  }

  protected def routeIdBasedOnProvidedIdentifiers(
    id: Option[String],
    path: Option[String],
    cluster: Option[String],
    method: Option[TRequestMethod]
  ): String = {
    id match {
      // use provided identifier
      case Some(routeId) => routeId
      // use routeId based on provided path information
      case _ =>
        (path, cluster) match {
          case (Some(p), Some(c)) =>
            automaticRouteId(
              method = method.map(_.name),
              path = p,
              cluster = c
            )
          case _ =>
            // normally should never happen due to validation in IdentifyingPathDetails
            throw InvalidInputException(
              "Invalid input. Path and cluster are required if id is not set!")
        }

    }
  }

  protected def buildPartialRouteFromOptions(
    id: Option[String],
    domains: Option[StringSetWrapper],
    authTypes: Option[AuthTypeSetWrapper],
    path: Option[String],
    cluster: Option[String],
    project: Option[String],
    method: Option[TRequestMethod],
    dps: Option[LongSetWrapper],
    userRoles: Option[StringSetWrapper],
    routeFlags: Option[StringSetWrapper],
    featurePermissions: Option[StringSetWrapper],
    subscriptionPermissions: Option[StringSetWrapper],
    routeTags: Option[StringSetWrapper],
    uaTags: Option[StringSetWrapper],
    scopes: Option[StringSetWrapper],
    decider: Option[String],
    ldapOwners: Option[StringSetWrapper],
    priority: Option[Int],
    rateLimit: Option[Int],
    timeoutMs: Option[Int],
    experimentBuckets: Option[ExperimentBucketSetWrapper]
  ): PartialRouteInfo =
    PartialRouteInfo(
      id = id,
      path = path,
      domains = domains.map(_.get()),
      cluster = cluster,
      projectId = project,
      method = method,
      authTypes = authTypes.map(_.get()),
      requiredDps = dps.map(_.get().map(dpId => DataPermissionAnnotation(id = dpId))),
      userRoles = userRoles.map(_.get()),
      routeFlags = routeFlags.map(_.get()),
      featurePermissions = featurePermissions.map(_.get()),
      subscriptionPermissions = subscriptionPermissions.map(_.get()),
      decider = decider,
      priority = priority,
      tags = routeTags.map(_.get()),
      experimentBuckets = experimentBuckets.map(_.get()),
      uaTags = uaTags.map(_.get()),
      rateLimit = rateLimit,
      timeoutMs = timeoutMs,
      ldapOwners = ldapOwners.map(_.get()),
      scopes = scopes.map(_.get())
    )

  protected def buildRouteFromOptions(
    domains: StringSetWrapper,
    authTypes: AuthTypeSetWrapper,
    path: String,
    cluster: String,
    project: String,
    method: Option[TRequestMethod],
    dps: Option[LongSetWrapper],
    userRoles: Option[StringSetWrapper],
    routeFlags: Option[StringSetWrapper],
    featurePermissions: Option[StringSetWrapper],
    subscriptionPermissions: Option[StringSetWrapper],
    routeTags: Option[StringSetWrapper],
    uaTags: Option[StringSetWrapper],
    scopes: Option[StringSetWrapper],
    decider: Option[String],
    ldapOwners: Option[StringSetWrapper],
    priority: Option[Int],
    rateLimit: Option[Int],
    timeoutMs: Option[Int],
    experimentBuckets: Option[ExperimentBucketSetWrapper]
  ): RouteInfo =
    RouteInfo(
      path = path,
      domains = domains.get(),
      cluster = cluster,
      projectId = Some(project),
      method = Some(method.getOrElse(RequestMethod.toThrift(RouteInformation.Defaults.Method))),
      authTypes = Some(authTypes.get()),
      requiredDps = dps.map(_.get().map(dpId => DataPermissionAnnotation(id = dpId))),
      userRoles = userRoles.map(_.get()),
      routeFlags = routeFlags.map(_.get()),
      featurePermissions = featurePermissions.map(_.get()),
      subscriptionPermissions = subscriptionPermissions.map(_.get()),
      decider = decider,
      priority = priority,
      tags = routeTags.map(_.get()),
      experimentBuckets = experimentBuckets.map(_.get()),
      uaTags = uaTags.map(_.get()),
      rateLimit = rateLimit,
      timeoutMs = timeoutMs,
      ldapOwners = ldapOwners.map(_.get()),
      scopes = scopes.map(_.get())
    )
}
