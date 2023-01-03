package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.api.models.RequestMethod.RequestMethod
import com.twitter.tfe.core.routingng.ExperimentBucket
import com.twitter.auth.customerauthtooling.thriftscala.{
  PartialRouteInfo => TPartialRouteInformation
}
import com.twitter.auth.customerauthtooling.thriftscala.{ExperimentBucket => TExperimentBucket}

case class PartialRouteInformation(
  path: Option[String] = None,
  domains: Option[Set[String]] = None,
  isNgRoute: Option[Boolean] = None,
  cluster: Option[String] = None,
  id: Option[String] = None,
  // Kite project where route is located
  projectId: Option[String] = None,
  method: Option[RequestMethod] = None,
  authTypes: Option[List[RouteAuthType]] = None,
  description: Option[String] = None,
  requestCategory: Option[String] = None,
  scopes: Option[Set[String]] = None,
  requiredDps: Option[List[DataPermission]] = None,
  userRoles: Option[Set[String]] = None,
  routeFlags: Option[Set[String]] = None,
  priority: Option[Int] = None,
  decider: Option[String] = None,
  featurePermissions: Option[Set[String]] = None,
  subscriptionPermissions: Option[Set[String]] = None,
  tags: Option[Set[String]] = None,
  experimentBuckets: Option[Set[ExperimentBucket]] = None,
  uaTags: Option[Set[String]] = None,
  rateLimit: Option[Int] = None,
  timeoutMs: Option[Int] = None,
  ldapOwners: Option[Set[String]] = None,
  lifeCycle: Option[String] = None) {

  val isSufficientForNewRoute: Boolean =
    (path, cluster, domains) match {
      case (Some(_), Some(_), Some(_)) => true
      case _ => false
    }

  val providedOrGeneratedRouteId: Option[String] = {
    (id, path, cluster) match {
      case (Some(routeId), _, _) =>
        Some(routeId)
      case (None, Some(p), Some(c)) =>
        Some(
          RouteInformation
            .normalizedRouteId(path = p, cluster = c, method = method.map(_.toString)))
      case _ => None
    }
  }

  /**
   * Prepares an updated route based on PartialRouteInformation and existingRoute
   *
   * @param existingRoute
   * @return
   */
  def toUpdatedRoute(existingRoute: RouteInformation): RouteInformation = {
    RouteInformation(
      path = path.getOrElse(existingRoute.path),
      domains = domains.getOrElse(existingRoute.domains),
      isNgRoute = isNgRoute.getOrElse(existingRoute.isNgRoute),
      cluster = cluster.getOrElse(existingRoute.cluster),
      projectId = projectId match {
        case Some(i) => Some(i)
        case None => existingRoute.projectId
      },
      id = id match {
        case Some(i) => Some(i)
        case None => existingRoute.id
      },
      method = method.getOrElse(existingRoute.method),
      authTypes = authTypes.getOrElse(existingRoute.authTypes),
      description = description.getOrElse(existingRoute.description),
      requestCategory = requestCategory.getOrElse(existingRoute.requestCategory),
      scopes = scopes.getOrElse(existingRoute.scopes),
      requiredDps = requiredDps.getOrElse(existingRoute.requiredDps),
      userRoles = userRoles.getOrElse(existingRoute.userRoles),
      routeFlags = routeFlags.getOrElse(existingRoute.routeFlags),
      priority = priority.getOrElse(existingRoute.priority),
      decider = decider match {
        case Some(d) => Some(d)
        case None => existingRoute.decider
      },
      featurePermissions = featurePermissions.getOrElse(existingRoute.featurePermissions),
      subscriptionPermissions =
        subscriptionPermissions.getOrElse(existingRoute.subscriptionPermissions),
      tags = tags.getOrElse(existingRoute.tags),
      experimentBuckets = experimentBuckets.getOrElse(existingRoute.experimentBuckets),
      uaTags = uaTags.getOrElse(existingRoute.uaTags),
      rateLimit = rateLimit.getOrElse(existingRoute.rateLimit),
      timeoutMs = timeoutMs.getOrElse(existingRoute.timeoutMs),
      ldapOwners = ldapOwners.getOrElse(existingRoute.ldapOwners),
      lifeCycle = lifeCycle.getOrElse(existingRoute.lifeCycle)
    )
  }

  /**
   * Prepares a new route based on PartialRouteInformation or returns None
   *
   * @return
   */
  def toNewRoute: Option[RouteInformation] = {
    (path, cluster, domains, projectId) match {
      case (Some(p), Some(c), Some(d), Some(prId)) =>
        Some(
          RouteInformation(
            path = p,
            domains = d,
            isNgRoute = true,
            cluster = c,
            id = None,
            projectId = Some(prId),
            method = method.getOrElse(RouteInformation.Defaults.Method),
            authTypes = authTypes.getOrElse(RouteInformation.Defaults.AuthTypes),
            description = description.getOrElse(RouteInformation.Defaults.Description),
            requestCategory = requestCategory.getOrElse(RouteInformation.Defaults.RequestCategory),
            scopes = scopes.getOrElse(RouteInformation.Defaults.Scopes),
            requiredDps = requiredDps.getOrElse(RouteInformation.Defaults.RequiredDps),
            userRoles = userRoles.getOrElse(RouteInformation.Defaults.UserRoles),
            routeFlags = routeFlags.getOrElse(RouteInformation.Defaults.RouteFlags),
            priority = priority.getOrElse(RouteInformation.Defaults.Priority),
            decider = decider,
            featurePermissions =
              featurePermissions.getOrElse(RouteInformation.Defaults.FeaturePermissions),
            subscriptionPermissions =
              subscriptionPermissions.getOrElse(RouteInformation.Defaults.SubscriptionPermissions),
            tags = tags.getOrElse(RouteInformation.Defaults.Tags),
            experimentBuckets =
              experimentBuckets.getOrElse(RouteInformation.Defaults.ExperimentBuckets),
            uaTags = uaTags.getOrElse(RouteInformation.Defaults.UaTags),
            rateLimit = rateLimit.getOrElse(RouteInformation.Defaults.RateLimit),
            timeoutMs = timeoutMs.getOrElse(RouteInformation.Defaults.TimeoutMs),
            ldapOwners = ldapOwners.getOrElse(RouteInformation.Defaults.LdapOwners),
            lifeCycle = lifeCycle.getOrElse(RouteInformation.Defaults.LifeCycle),
          ))
      case _ => None
    }
  }

  def toThrift: TPartialRouteInformation = {
    TPartialRouteInformation(
      path = path,
      domains = domains,
      cluster = cluster,
      projectId = projectId,
      method = method.map(m => RequestMethod.toThrift(m)),
      authTypes = authTypes.map(_.map(_.toThrift).toSet),
      description = description,
      requestCategory = requestCategory,
      scopes = scopes,
      requiredDps = requiredDps.map(_.map(_.toThrift).toSet),
      userRoles = userRoles,
      routeFlags = routeFlags,
      featurePermissions = featurePermissions,
      subscriptionPermissions = subscriptionPermissions,
      decider = decider,
      priority = priority,
      tags = tags,
      experimentBuckets =
        experimentBuckets.map(_.map(b => TExperimentBucket(key = b.key, bucket = b.bucket))),
      uaTags = uaTags,
      rateLimit = rateLimit,
      timeoutMs = timeoutMs,
      ldapOwners = ldapOwners,
      id = id,
      lifeCycle = lifeCycle
    )
  }

}

object PartialRouteInformation {
  def fromThrift(thrift: TPartialRouteInformation): PartialRouteInformation = {
    PartialRouteInformation(
      path = thrift.path,
      domains = thrift.domains.map(_.toSet),
      cluster = thrift.cluster,
      id = thrift.id,
      projectId = thrift.projectId,
      method = thrift.method.map(RequestMethod.fromThrift),
      authTypes = thrift.authTypes
        .map(_.map(v => RouteAuthType.fromThrift(v)).toList),
      description = thrift.description,
      requestCategory = thrift.requestCategory,
      scopes = thrift.scopes.map(_.toSet),
      requiredDps = thrift.requiredDps
        .map(_.map(v => DataPermission(v.id)).toList),
      userRoles = thrift.userRoles.map(_.toSet),
      routeFlags = thrift.routeFlags.map(_.toSet),
      priority = thrift.priority,
      decider = thrift.decider,
      featurePermissions = thrift.featurePermissions.map(_.toSet),
      subscriptionPermissions = thrift.subscriptionPermissions.map(_.toSet),
      tags = thrift.tags.map(_.toSet),
      experimentBuckets = thrift.experimentBuckets
        .map(_.map(thrift => ExperimentBucket(key = thrift.key, bucket = thrift.bucket)).toSet),
      uaTags = thrift.uaTags.map(_.toSet),
      rateLimit = thrift.rateLimit,
      timeoutMs = thrift.timeoutMs,
      ldapOwners = thrift.ldapOwners.map(_.toSet),
      lifeCycle = thrift.lifeCycle
    )
  }
}
