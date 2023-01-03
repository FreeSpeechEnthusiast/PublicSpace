package com.twitter.auth.customerauthtooling.api.components

import com.twitter.auth.customerauthtooling.api.components.dpprovider.DpProviderInterface
import com.twitter.auth.customerauthtooling.api.components.dpprovider.SupportedDpProviders
import com.twitter.auth.customerauthtooling.api.models.DataPermission
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.util.Future
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
final case class DpProviderService @Inject() (
  namedDpProviders: Map[SupportedDpProviders.Value, DpProviderInterface]) {
  def getDataPermissionsForEndpoint(
    dpProviderName: SupportedDpProviders.Value,
    endpoint: EndpointInfo
  ): Future[Seq[DataPermission]] = {
    namedDpProviders(dpProviderName).getForEndpoint(endpoint)
  }
}
