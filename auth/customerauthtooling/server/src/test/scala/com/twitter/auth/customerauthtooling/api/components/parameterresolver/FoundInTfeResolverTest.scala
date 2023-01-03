package com.twitter.auth.customerauthtooling.api.components.parameterresolver

import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameterValue
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when

@RunWith(classOf[JUnitRunner])
class FoundInTfeResolverTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter {

  private val routeInformationService = mock[RouteInformationServiceInterface]

  private val resolver = FoundInTfeResolver(routeInformationService = routeInformationService)

  private val testUrl = "/endpoint"

  test("test FoundInTfeChecker with ngroute endpoint") {
    when(routeInformationService.checkEndpoint(EndpointInfo(url = testUrl)))
      .thenReturn(
        Future.value(Some(RouteInformation(testUrl, Set(""), isNgRoute = true, cluster = "test"))))
    Await.result(resolver.checkWithOverride(EndpointInfo(url = testUrl))) mustBe Some(
      AdoptionParameterValue[AdoptionParameter.FoundInTfe](underlying = true))
  }

  test("test FoundInTfeChecker with unknown endpoint") {
    when(routeInformationService.checkEndpoint(EndpointInfo(url = testUrl)))
      .thenReturn(Future.None)
    Await.result(resolver.checkWithOverride(EndpointInfo(url = testUrl))) mustBe Some(
      AdoptionParameterValue[AdoptionParameter.FoundInTfe](underlying = false))
  }

}
