package com.twitter.auth.customerauthtooling.api.components.pacmanngroutestorage

import com.twitter.cim.pacman.plugin.tferoute.models.CreateOrUpdateRouteRequest
import com.twitter.cim.pacman.plugin.tferoute.models.DeleteRouteRequest
import com.twitter.cim.pacman.plugin.tferoute.models.ProvisioningResponse
import com.twitter.tfe.core.routingng.RawRouteWithResourceInformation
import com.twitter.util.Future

trait PacmanNgRouteStorageServiceInterface {

  def getRoutesByProjects(projects: Set[String]): Future[Seq[RawRouteWithResourceInformation]]

  def getRoutesByIds(routeIds: Set[String]): Future[Seq[RawRouteWithResourceInformation]]

  def createRoute(request: CreateOrUpdateRouteRequest): Future[ProvisioningResponse]

  def updateRoute(request: CreateOrUpdateRouteRequest): Future[ProvisioningResponse]

  def deleteRoute(request: DeleteRouteRequest): Future[ProvisioningResponse]

}
