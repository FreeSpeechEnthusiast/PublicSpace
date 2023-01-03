package com.twitter.auth.customerauthtooling.api.components

import com.twitter.auth.customerauthtooling.api.components.dpprovider.DpProviderInterface
import com.twitter.auth.customerauthtooling.api.components.dpprovider.SupportedDpProviders
import com.twitter.auth.customerauthtooling.api.components.pacmanngroutestorage.PacmanNgRouteStorageServiceInterface
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.auth.customerauthtooling.api.models.EndpointMetadata
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class RouteInformationServiceTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter {

  private val ngRouteStorage = mock[PacmanNgRouteStorageServiceInterface]
  private val namedDpProvidersStorage = Map.empty[SupportedDpProviders.Value, DpProviderInterface]

  private val routeinformationservice =
    RouteInformationService(
      ngRouteStorage = ngRouteStorage,
      namedDpProviders = namedDpProvidersStorage)

  private val testUrl = "/endpoint"

  private val testMetadata = EndpointMetadata()

  test("test routeinformationservice with test endpoint") {
    Await.result(
      routeinformationservice.checkEndpoint(endpoint =
        EndpointInfo(url = testUrl, metadata = Some(testMetadata)))) mustBe None
  }

}
