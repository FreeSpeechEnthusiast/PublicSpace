package com.twitter.auth.policykeeper.api.storage

import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.auth.policykeeper.api.storage.common.PolicyMappingUtils
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Future

/**
 * Class provides automatic endpoint-to-policy mapping based on route tags using static regex template
 *
 * For example:
 * route tag [policy_xxxx] automatically matches a policy with identifier xxxx
 * route tag [policy_yyyy] automatically matches a policy with identifier yyyy
 *
 * @param policyStorage
 * @param statsReceiver
 */
case class StaticPolicyToRouteTagMapping(
  policyStorage: ReadOnlyPolicyStorageInterface,
  statsReceiver: StatsReceiver,
  logger: JsonLogger)
    extends ReadOnlyEndpointAssociationStorageInterface[String, RouteTag] {

  private val Scope = "StaticPolicyToRouteTagMapping"
  private val loggerScope = logger.withScope(Scope)

  private def staticMapping(routeTag: RouteTag): Option[PolicyId] = {
    routeTag.value match {
      case PolicyMappingUtils.StaticMatchPattern(policyId) => Some(PolicyId(policyId))
      case _ =>
        loggerScope.info(
          message = "route tag `" + routeTag.value + "` doesn't match any policy",
          metadata = Some(Map("routeTag" -> routeTag.value))
        )
        None
    }
  }

  override protected[storage] def getAssociatedPoliciesIds(
    associatedData: Seq[RouteTag]
  ): Future[Seq[PolicyId]] = {
    Future.value(
      associatedData
        .map(staticMapping).collect {
          case Some(p) => p
        }
    )
  }
}
