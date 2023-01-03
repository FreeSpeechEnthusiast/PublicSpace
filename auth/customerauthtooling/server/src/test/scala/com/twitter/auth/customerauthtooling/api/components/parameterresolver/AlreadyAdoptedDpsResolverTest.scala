package com.twitter.auth.customerauthtooling.api.components.parameterresolver

import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameterValue
import com.twitter.auth.customerauthtooling.api.models.DataPermission
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class AlreadyAdoptedDpsResolverTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter {

  private val routeInformationService = mock[RouteInformationServiceInterface]

  private val resolver =
    AlreadyAdoptedDpsResolver(routeInformationService = routeInformationService)

  private val testUrl = "/endpoint"

  test("test AlreadyAdoptedDpsResolver, unknown endpoint") {
    when(routeInformationService.checkEndpoint(EndpointInfo(url = testUrl)))
      .thenReturn(Future.None)
    Await.result(resolver.checkWithOverride(EndpointInfo(url = testUrl))) mustBe None
  }

  test("test AlreadyAdoptedDpsResolver, endpoint with dps") {
    when(routeInformationService.checkEndpoint(EndpointInfo(url = testUrl)))
      .thenReturn(
        Future.value(
          Some(
            RouteInformation(
              testUrl,
              isNgRoute = true,
              domains = Set("api.twitter.biz"),
              requiredDps = List(1L).map(dpId => DataPermission(dataPermissionId = dpId)),
              cluster = "test"))))
    Await.result(resolver.checkWithOverride(EndpointInfo(url = testUrl))) mustBe Some(
      AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps](underlying = true))
  }

  test("test AlreadyAdoptedDpsResolver, endpoint without dps") {
    when(routeInformationService.checkEndpoint(EndpointInfo(url = testUrl)))
      .thenReturn(
        Future.value(
          Some(
            RouteInformation(
              testUrl,
              isNgRoute = true,
              domains = Set("api.twitter.com"),
              cluster = "test"))))
    Await.result(resolver.checkWithOverride(EndpointInfo(url = testUrl))) mustBe Some(
      AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps](underlying = false))
  }

}
