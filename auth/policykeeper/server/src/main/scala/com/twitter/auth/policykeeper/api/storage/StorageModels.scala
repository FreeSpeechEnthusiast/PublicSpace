package com.twitter.auth.policykeeper.api.storage

import com.twitter.auth.policykeeper.thriftscala.RouteInformation

/**
 * Describes how to extract association information from RouteInformation
 * This information is used for policy lookups
 */
trait EndpointAssociationData[T, +Self <: EndpointAssociationData[T, Self]] {
  val value: T

  def fromRouteInformation(
    routeInformation: RouteInformation
  ): Seq[Self]
}

/**
 * RouteTag implementation of EndpointAssociationData
 * describes how to extract route tags from RouteInformation
 * This information is used by concrete implementations
 * of [[ReadOnlyEndpointAssociationStorageInterface]] and [[EndpointAssociationStorageInterface]]
 * for policy lookups
 */
final case class RouteTag(routeTag: String) extends EndpointAssociationData[String, RouteTag] {
  override val value: String = routeTag

  override def fromRouteInformation(
    routeInformation: RouteInformation
  ): Seq[RouteTag] = {
    routeInformation.routeTags match {
      case None => Seq.empty[RouteTag]
      case Some(set) => set.map(RouteTag).toSeq
    }
  }
}

/**
 * Helper class for route information extraction
 */
object RouteTags {
  def fromRouteInformation(routeInformation: RouteInformation): Seq[RouteTag] =
    RouteTag(routeTag = "").fromRouteInformation(routeInformation)
}
