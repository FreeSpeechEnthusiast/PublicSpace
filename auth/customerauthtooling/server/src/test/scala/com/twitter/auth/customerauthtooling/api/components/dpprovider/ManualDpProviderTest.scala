package com.twitter.auth.customerauthtooling.api.components.dpprovider

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
class ManualDpProviderTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val manualDpProvider = ManualDpProvider()

  private val testUrl = "/endpoint"

  private val testDataPermissions = Seq(DataPermission(1), DataPermission(3), DataPermission(32))

  test("test ManualDpProvider without metadata") {
    Await.result(manualDpProvider.getForEndpoint(EndpointInfo(url = testUrl))) mustBe Seq()
  }

  test("test ManualDpProvider with metadata") {
    Await.result(
      manualDpProvider.getForEndpoint(
        EndpointInfo(
          url = testUrl,
          metadata =
            Some(
              EndpointMetadata(suppliedDataPermissions =
                Some(testDataPermissions)))))) mustBe testDataPermissions
  }

}
