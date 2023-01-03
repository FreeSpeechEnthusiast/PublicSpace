package com.twitter.auth.customerauthtooling.api.components

import com.twitter.cim.pacman.plugin.tferoute.models.CreateOrUpdateRouteRequest
import com.twitter.cim.pacman.plugin.tferoute.models.DeleteRouteRequest
import com.twitter.cim.pacman.plugin.tferoute.models.ProvisioningResponse
import com.twitter.cim.pacman.plugin.tferoute.models.SearchRoutesRequest
import com.twitter.auth.customerauthtooling.api.components.pacmanngroutestorage.PacmanNgRouteStorageServiceInterface
import com.twitter.kite.clients.KiteClient
import com.twitter.util.Duration
import com.twitter.util.Future
import javax.inject.Inject
import javax.inject.Singleton
import com.twitter.conversions.DurationOps._
import com.twitter.tfe.core.routingng.RawRouteWithResourceInformation

@Singleton
private[api] case class PacmanNgRouteStorageService @Inject() (
  private val kiteClient: KiteClient,
  maxTimeoutSeconds: Duration = 15.seconds)
    extends PacmanNgRouteStorageServiceInterface {

  override def createRoute(request: CreateOrUpdateRouteRequest): Future[ProvisioningResponse] = {
    PacmanExponentialBackoffCall(
      kiteClient.routestore.createRoute,
      maxTimeoutSeconds = maxTimeoutSeconds)(request)
  }

  override def updateRoute(request: CreateOrUpdateRouteRequest): Future[ProvisioningResponse] = {
    PacmanExponentialBackoffCall(
      kiteClient.routestore.updateRoute,
      maxTimeoutSeconds = maxTimeoutSeconds)(request)
  }

  override def deleteRoute(request: DeleteRouteRequest): Future[ProvisioningResponse] = {
    PacmanExponentialBackoffCall(
      kiteClient.routestore.deleteRoute,
      maxTimeoutSeconds = maxTimeoutSeconds)(request)
  }

  override def getRoutesByProjects(
    projects: Set[String]
  ): Future[Seq[RawRouteWithResourceInformation]] = {
    kiteClient.routestore.searchRoutesWithResourceInformation(
      SearchRoutesRequest(projects = Some(projects.toSeq)))
  }

  override def getRoutesByIds(
    routeIds: Set[String]
  ): Future[Seq[RawRouteWithResourceInformation]] = {
    kiteClient.routestore.searchRoutesWithResourceInformation(
      SearchRoutesRequest(routeIds = Some(routeIds)))
  }
}
