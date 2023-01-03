package com.twitter.auth.customerauthtooling.api.components.dpprovider

import com.twitter.auth.customerauthtooling.api.models.DataPermission
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.util.Future

trait DpProviderInterface {
  def name(): SupportedDpProviders.Value
  def getForEndpoint(endpoint: EndpointInfo): Future[Seq[DataPermission]]
}

object SupportedDpProviders extends Enumeration {
  type SupportedDpProviders = Value
  val Manual, DpRecommender = Value
}
