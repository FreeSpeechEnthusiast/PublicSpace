package com.twitter.auth.customerauthtooling.api.components.parameterresolver

import com.twitter.auth.customerauthtooling.api.components.routeinfoservice.RouteInformationServiceInterface
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameterValue
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.auth.customerauthtooling.api.models.RouteAuthType
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
import com.twitter.auth.authenticationtype.thriftscala.{AuthenticationType => TAuthenticationType}

@RunWith(classOf[JUnitRunner])
class RequiresAuthResolverTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter {

  private val routeInformationService = mock[RouteInformationServiceInterface]

  private val resolver = RequiresAuthResolver(routeInformationService = routeInformationService)

  private val testUrl = "/endpoint"

  test("test RequiresAuthChecker with unknown endpoint") {
    when(routeInformationService.checkEndpoint(EndpointInfo(url = testUrl)))
      .thenReturn(Future.None)
    Await.result(resolver.checkWithOverride(EndpointInfo(url = testUrl))) mustBe None
  }

  test("test RequiresAuthChecker with auth endpoint") {
    when(routeInformationService.checkEndpoint(EndpointInfo(url = testUrl)))
      .thenReturn(
        Future.value(
          Some(
            RouteInformation(
              testUrl,
              isNgRoute = true,
              domains = Set("api.twitter.biz"),
              authTypes = List(TAuthenticationType.Oauth2).map(RouteAuthType.fromThrift),
              cluster = "test"))))
    Await.result(resolver.checkWithOverride(EndpointInfo(url = testUrl))) mustBe Some(
      AdoptionParameterValue[AdoptionParameter.RequiresAuth](underlying = true))
  }

  test("test RequiresAuthChecker without auth endpoint") {
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
      AdoptionParameterValue[AdoptionParameter.RequiresAuth](underlying = false))
  }

  test("test RequiresAuthChecker with NoAuthentication auth endpoint") {
    when(routeInformationService.checkEndpoint(EndpointInfo(url = testUrl)))
      .thenReturn(Future.value(Some(RouteInformation(
        testUrl,
        isNgRoute = true,
        domains = Set("api.twitter.com"),
        authTypes = List(TAuthenticationType.NoAuthentication).map(RouteAuthType.fromThrift),
        cluster = "test"
      ))))
    Await.result(resolver.checkWithOverride(EndpointInfo(url = testUrl))) mustBe Some(
      AdoptionParameterValue[AdoptionParameter.RequiresAuth](underlying = false))
  }

}
