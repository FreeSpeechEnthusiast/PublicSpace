package com.twitter.auth.customerauthtooling.api.components

import com.twitter.auth.customerauthtooling.api.components.dpprovider.ManualDpProvider
import com.twitter.auth.customerauthtooling.api.components.dpprovider.SupportedDpProviders
import com.twitter.auth.customerauthtooling.api.models.DataPermission
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

@RunWith(classOf[JUnitRunner])
class DpProviderServiceTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val dpproviderservice =
    DpProviderService(namedDpProviders = Map(SupportedDpProviders.Manual -> ManualDpProvider()))

  private val testUrl = "/endpoint"

  private val testDataPermissions = Seq(DataPermission(1), DataPermission(3), DataPermission(32))

  test("test DpProviderService using manual provider") {
    Await.result(
      dpproviderservice.getDataPermissionsForEndpoint(
        dpProviderName = SupportedDpProviders.Manual,
        endpoint = EndpointInfo(
          url = testUrl,
          metadata = Some(EndpointMetadata(suppliedDataPermissions = Some(testDataPermissions))))
      )) mustBe testDataPermissions
  }

}
