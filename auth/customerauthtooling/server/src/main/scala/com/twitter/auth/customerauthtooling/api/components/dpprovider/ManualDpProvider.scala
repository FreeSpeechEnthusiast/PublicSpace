package com.twitter.auth.customerauthtooling.api.components.dpprovider

import com.twitter.auth.customerauthtooling.api.models.DataPermission
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.util.Future

final case class ManualDpProvider() extends DpProviderInterface {
  def name(): SupportedDpProviders.Value = SupportedDpProviders.Manual
  def getForEndpoint(endpoint: EndpointInfo): Future[Seq[DataPermission]] = {
    Future.value(endpoint.metadata match {
      case Some(m) => m.suppliedDataPermissions.getOrElse(Seq())
      case None => Seq()
    })
  }
}
