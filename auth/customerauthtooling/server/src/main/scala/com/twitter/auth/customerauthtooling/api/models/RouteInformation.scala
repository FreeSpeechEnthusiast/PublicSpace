package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.api.models.RequestMethod.RequestMethod
import com.twitter.tfe.core.routingng.ExperimentBucket
import com.twitter.tfe.core.routingng.RawDestination
import com.twitter.tfe.core.routingng.{RawRoute => RawNgRoute}
import com.twitter.auth.customerauthtooling.thriftscala.{ExperimentBucket => TExperimentBucket}
import com.twitter.auth.customerauthtooling.thriftscala.{RouteInfo => TRouteInformation}
import com.twitter.auth.customerauthtooling.thriftscala.{RequestMethod => TRequestMethod}
import com.twitter.auth.authenticationtype.thriftscala.{AuthenticationType => TAuthenticationType}
import com.twitter.auth.customerauthtooling.api.models
import com.twitter.auth.customerauthtooling.api.models.RouteInformation.normalizeMethod
import com.twitter.auth.customerauthtooling.api.models.RouteInformation.normalizePath
import com.twitter.tfe.core.routingng.RawRouteWithResourceInformation

final case class RouteInformation(
  path: String,
  domains: Set[String],
  isNgRoute: Boolean,
  cluster: String,
  id: Option[String] = None,
  // Kite project where route is located
  projectId: Option[String] = None,
  method: RequestMethod = RouteInformation.Defaults.Method,
  authTypes: List[RouteAuthType] = RouteInformation.Defaults.AuthTypes,
  description: String = RouteInformation.Defaults.Description,
  requestCategory: String = RouteInformation.Defaults.RequestCategory,
  scopes: Set[String] = RouteInformation.Defaults.Scopes,
  requiredDps: List[DataPermission] = RouteInformation.Defaults.RequiredDps,
  userRoles: Set[String] = RouteInformation.Defaults.UserRoles,
  routeFlags: Set[String] = RouteInformation.Defaults.RouteFlags,
  priority: Int = RouteInformation.Defaults.Priority,
  decider: Option[String] = None,
  featurePermissions: Set[String] = RouteInformation.Defaults.FeaturePermissions,
  subscriptionPermissions: Set[String] = RouteInformation.Defaults.SubscriptionPermissions,
  tags: Set[String] = RouteInformation.Defaults.Tags,
  experimentBuckets: Set[ExperimentBucket] = RouteInformation.Defaults.ExperimentBuckets,
  uaTags: Set[String] = RouteInformation.Defaults.UaTags,
  rateLimit: Int = RouteInformation.Defaults.RateLimit,
  timeoutMs: Int = RouteInformation.Defaults.TimeoutMs,
  ldapOwners: Set[String] = RouteInformation.Defaults.LdapOwners,
  lifeCycle: String = RouteInformation.Defaults.LifeCycle) {

  val normalizedMethod: String = normalizeMethod(Some(method.toString))
  val normalizedPath: String = normalizePath(path)

  /**
   * TODO: fill it out with exact list
   */
  protected val twitterPublicDomains: Set[String] = Set(
    "twitter.com",
    "api.twitter.com"
  )

  lazy val isLegacyRoute: Boolean = !isNgRoute
  lazy val isInternal: Boolean =
    twitterPublicDomains.intersect(domains.map(_.toLowerCase)).isEmpty
  lazy val isPublic: Boolean = !isInternal
  lazy val requiresAuth: Boolean = authTypes.exists(_.requiresAuthentication)
  lazy val usesUserIdentity: Boolean = authTypes.exists(_.hasUserIdentity)
  lazy val supportsScopes: Boolean = authTypes.exists(_.supportsScopes)
  lazy val requiresDps: Boolean = requiredDps.nonEmpty

  lazy val expectedNgRouteId = RouteInformation.normalizedRouteId(
    path = normalizedPath,
    cluster = cluster,
    method = Some(method.toString))

  def toRawNgRoute(): RawNgRoute = {
    RawNgRoute(
      id = id.getOrElse(""),
      vhosts = domains,
      normalizedPath = normalizedPath,
      requestMethod = normalizedMethod,
      userRoles = userRoles,
      decider = decider,
      rateLimitsMap = Map("default" -> Some(rateLimit)),
      dataPermissions = requiredDps.map(_.toRawDataPermissionAnnotation).toSet,
      featurePermissions = featurePermissions,
      subscriptionPermissions = subscriptionPermissions,
      routeAuthTypes = authTypes.map(_.toThrift.originalName).toSet,
      scopes = scopes,
      routeFlags = routeFlags,
      cluster = cluster,
      destination = RawDestination(cluster = Some(cluster)),
      // This is the lifecycle the route could be at in the kite dashboard.
      // draft (will receive no traffic),
      // canary (will be loaded onto 1% of TFE hosts thus you will receive 1% of traffic)
      // or production
      lifeCycle = lifeCycle,
      timeoutMs = Some(timeoutMs),
      ldapOwners = Set("customerauthtooling", "authplatform") ++ ldapOwners,
      description = Some(description),
      tags = tags ++ Set("customerauthtools-generated"),
      requestCategory = requestCategory,
      priority = priority,
      experimentBucket = None,
      experimentBuckets = experimentBuckets,
      uaTags = uaTags
    )
  }

  def toThrift: TRouteInformation = {
    TRouteInformation(
      path = path,
      domains = domains,
      cluster = cluster,
      projectId = projectId,
      method = Some(RequestMethod.toThrift(method)),
      authTypes = Some(authTypes.map(_.toThrift).toSet),
      description = Some(description),
      requestCategory = Some(requestCategory),
      scopes = Some(scopes),
      requiredDps = Some(requiredDps.map(_.toThrift).toSet),
      userRoles = Some(userRoles),
      routeFlags = Some(routeFlags),
      featurePermissions = Some(featurePermissions),
      subscriptionPermissions = Some(subscriptionPermissions),
      decider = decider,
      priority = Some(priority),
      tags = Some(tags),
      experimentBuckets =
        Some(experimentBuckets.map(b => TExperimentBucket(key = b.key, bucket = b.bucket))),
      uaTags = Some(uaTags),
      rateLimit = Some(rateLimit),
      timeoutMs = Some(timeoutMs),
      ldapOwners = Some(ldapOwners),
      id = id
    )
  }

}

object RouteInformation {
  object Defaults {
    val Method: models.RequestMethod.Value = RequestMethod.Get
    val AuthTypes = List(RouteAuthType.fromThrift(TAuthenticationType.Unknown))
    val Description = ""
    val RequestCategory = "API"
    val Scopes = Set.empty[String]
    val RequiredDps = List.empty[DataPermission]
    val UserRoles = Set.empty[String]
    val RouteFlags = Set.empty[String]
    val Priority = 0
    val FeaturePermissions = Set.empty[String]
    val SubscriptionPermissions = Set.empty[String]
    val Tags = Set.empty[String]
    val ExperimentBuckets = Set.empty[ExperimentBucket]
    val UaTags = Set.empty[String]
    val TimeoutMs = 5000
    val LdapOwners = Set.empty[String]
    val RateLimit = 0 // 0 means unlimited
    val LifeCycle = "draft"
  }

  def fromRawNgRouteWithResourceInfo(
    rawRouteWithResourceInformation: RawRouteWithResourceInformation
  ): RouteInformation = {
    RouteInformation(
      path = rawRouteWithResourceInformation.routePackage.route.normalizedPath,
      domains = rawRouteWithResourceInformation.routePackage.route.vhosts,
      cluster = rawRouteWithResourceInformation.routePackage.route.cluster,
      id = Some(rawRouteWithResourceInformation.routePackage.route.id),
      projectId = rawRouteWithResourceInformation.projectName,
      method = TRequestMethod
        .valueOf(rawRouteWithResourceInformation.routePackage.route.requestMethod).map(
          RequestMethod.fromThrift).getOrElse(RouteInformation.Defaults.Method),
      // remove auth types that are not supported
      authTypes = rawRouteWithResourceInformation.routePackage.route.routeAuthTypes
        .map(RouteAuthType.AuthTypeOriginalNameToValueMap.get).collect {
          case Some(intAuthType) => RouteAuthType(intAuthType)
        } match {
        // if set is empty (due to unsupported types removal) replace it with default auth types
        case set if set.isEmpty => RouteInformation.Defaults.AuthTypes
        case set => set.toList
      },
      description = rawRouteWithResourceInformation.routePackage.route.description
        .getOrElse(RouteInformation.Defaults.Description),
      requestCategory = rawRouteWithResourceInformation.routePackage.route.requestCategory,
      scopes = rawRouteWithResourceInformation.routePackage.route.scopes,
      requiredDps = rawRouteWithResourceInformation.routePackage.route.dataPermissions
        .map(d => DataPermission(dataPermissionId = d.id, state = Some(d.state))).toList,
      userRoles = rawRouteWithResourceInformation.routePackage.route.userRoles,
      routeFlags = rawRouteWithResourceInformation.routePackage.route.routeFlags,
      priority = rawRouteWithResourceInformation.routePackage.route.priority,
      decider = rawRouteWithResourceInformation.routePackage.route.decider,
      featurePermissions = rawRouteWithResourceInformation.routePackage.route.featurePermissions,
      subscriptionPermissions =
        rawRouteWithResourceInformation.routePackage.route.subscriptionPermissions,
      tags = rawRouteWithResourceInformation.routePackage.route.tags,
      experimentBuckets = rawRouteWithResourceInformation.routePackage.route.experimentBuckets
        .map(b => ExperimentBucket(key = b.key, bucket = b.bucket)),
      uaTags = rawRouteWithResourceInformation.routePackage.route.uaTags,
      rateLimit = rawRouteWithResourceInformation.routePackage.route
        .rateLimitsMap("default").getOrElse(RouteInformation.Defaults.RateLimit) match {

        /**
         * Kite API automatically replaces rate limit to -1 when 0 passed
         * to properly detect route changes we have to normalize it
         */
        case v if v < 0 => 0
        case v => v
      },
      timeoutMs = rawRouteWithResourceInformation.routePackage.route.timeoutMs
        .getOrElse(RouteInformation.Defaults.TimeoutMs),
      ldapOwners = rawRouteWithResourceInformation.routePackage.route.ldapOwners,
      isNgRoute = true,
      // use original lifecycle, otherwise PACMAN won't let us to make a draft
      lifeCycle = rawRouteWithResourceInformation.routePackage.route.lifeCycle,
    )
  }

  def fromThrift(thrift: TRouteInformation): RouteInformation = {
    RouteInformation(
      path = thrift.path,
      domains = thrift.domains.toSet,
      cluster = thrift.cluster,
      id = thrift.id,
      projectId = thrift.projectId,
      method =
        thrift.method.map(RequestMethod.fromThrift).getOrElse(RouteInformation.Defaults.Method),
      authTypes = thrift.authTypes
        .map(_.map(v => RouteAuthType.fromThrift(v)).toList).getOrElse(
          RouteInformation.Defaults.AuthTypes),
      description = thrift.description.getOrElse(RouteInformation.Defaults.Description),
      requestCategory = thrift.requestCategory.getOrElse(RouteInformation.Defaults.RequestCategory),
      scopes = thrift.scopes.getOrElse(RouteInformation.Defaults.Scopes).toSet,
      requiredDps = thrift.requiredDps
        .map(_.map(v => DataPermission(v.id)).toList).getOrElse(
          RouteInformation.Defaults.RequiredDps),
      userRoles = thrift.userRoles.getOrElse(RouteInformation.Defaults.UserRoles).toSet,
      routeFlags = thrift.routeFlags.getOrElse(RouteInformation.Defaults.RouteFlags).toSet,
      priority = thrift.priority.getOrElse(RouteInformation.Defaults.Priority),
      decider = thrift.decider,
      featurePermissions =
        thrift.featurePermissions.getOrElse(RouteInformation.Defaults.FeaturePermissions).toSet,
      subscriptionPermissions = thrift.subscriptionPermissions
        .getOrElse(RouteInformation.Defaults.SubscriptionPermissions).toSet,
      tags = thrift.tags.getOrElse(RouteInformation.Defaults.Tags).toSet,
      experimentBuckets = thrift.experimentBuckets
        .map(_.map(thrift => ExperimentBucket(key = thrift.key, bucket = thrift.bucket)).toSet)
        .getOrElse(RouteInformation.Defaults.ExperimentBuckets),
      uaTags = thrift.uaTags.map(_.toSet).getOrElse(RouteInformation.Defaults.UaTags),
      rateLimit = thrift.rateLimit.getOrElse(RouteInformation.Defaults.RateLimit),
      timeoutMs = thrift.timeoutMs.getOrElse(RouteInformation.Defaults.TimeoutMs),
      ldapOwners = thrift.ldapOwners.map(_.toSet).getOrElse(RouteInformation.Defaults.LdapOwners),
      isNgRoute = true,
      lifeCycle = thrift.lifeCycle.getOrElse(RouteInformation.Defaults.LifeCycle)
    )
  }

  /**
   * Convert a path with format /a/:b/c to /a/{b}/c
   * @param path
   * @return
   */
  def normalizePath(path: String): String = {
    path
      .split('/')
      .map(segment =>
        if (segment.startsWith(":")) {
          s"{${segment.slice(1, segment.length)}}"
        } else {
          segment
        }).mkString("/")
  }

  /**
   * Normalize provided route method format or default method
   * @param method
   * @return
   */
  def normalizeMethod(method: Option[String]): String = {
    method.getOrElse(Defaults.Method.toString).toUpperCase
  }

  /**
   * Creates normalized route id for KITE based on path, cluster and method
   *
   * @param path
   * @param cluster
   * @param method
   * @return
   */
  def normalizedRouteId(path: String, cluster: String, method: Option[String]): String = {
    s"${normalizeMethod(method)}${normalizePath(path)}->cluster:$cluster"
  }
}
