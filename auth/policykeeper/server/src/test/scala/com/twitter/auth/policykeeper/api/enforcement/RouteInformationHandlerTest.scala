package com.twitter.auth.policykeeper.api.enforcement

import com.twitter.auth.policykeeper.thriftscala.RequestInformation
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.finagle.http.MediaType
import com.twitter.finagle.http.Request
import com.twitter.tfe.core.routingng.NgRoute
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import com.twitter.finagle.http.Method

@RunWith(classOf[JUnitRunner])
class RouteInformationHandlerTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val requestPath = "/test"
  private val routeWithPolicyTags: NgRoute =
    NgRoute.testRoute(
      normalizedPath = requestPath,
      routeFlags = Set(),
      tags = Set("policy_p1", "policy_p2", "somethingelse"))
  private val routeNoTags: NgRoute =
    NgRoute.testRoute(normalizedPath = requestPath, routeFlags = Set(), tags = Set())
  private val routeWithTags: NgRoute =
    NgRoute.testRoute(
      normalizedPath = requestPath,
      routeFlags = Set(),
      tags = Set("sometag", "anothertag", "policy_*invalid*"))

  private val testDefaultRequestInfo =
    RequestInformation(
      path = requestPath,
      host = None,
      method = "GET",
      queryParams = Some(Map()),
      bodyParams = Some(Map()))

  test("test collectRouteInformationFromRequest with non ng route") {
    RouteInformationHandler.collectRouteInformationFromRequest(
      Request(requestPath),
      ngRoute = None) mustBe
      RouteInformation(
        isNgRoute = false,
        routeTags = None,
        requestInformation = Some(testDefaultRequestInfo))
  }

  test("test collectRouteInformationFromRequest with ng route with policy tags") {
    RouteInformationHandler.collectRouteInformationFromRequest(
      Request(requestPath),
      ngRoute = Some(routeWithPolicyTags)) mustBe
      RouteInformation(
        isNgRoute = true,
        routeTags = Some(Set("policy_p1", "policy_p2")),
        requestInformation = Some(testDefaultRequestInfo))
  }

  test("test collectRouteInformationFromRequest with ng route without policy tags") {
    RouteInformationHandler.collectRouteInformationFromRequest(
      Request(requestPath),
      ngRoute = Some(routeWithTags)) mustBe
      RouteInformation(
        isNgRoute = true,
        routeTags = None,
        requestInformation = Some(testDefaultRequestInfo))
  }

  test("test collectRouteInformationFromRequest with ng route without tags") {
    RouteInformationHandler.collectRouteInformationFromRequest(
      Request(requestPath),
      ngRoute = Some(routeNoTags)) mustBe
      RouteInformation(
        isNgRoute = true,
        routeTags = None,
        requestInformation = Some(testDefaultRequestInfo))
  }

  test("test collectRouteInformationFromRequest with ng route with parameters (GET request)") {
    RouteInformationHandler.collectRouteInformationFromRequest(
      Request(requestPath, "foo" -> "bar", "foo2" -> "bar2"),
      ngRoute = Some(routeNoTags)) mustBe
      RouteInformation(
        isNgRoute = true,
        routeTags = None,
        requestInformation = Some(
          testDefaultRequestInfo.copy(
            queryParams = Some(Map("foo" -> "bar", "foo2" -> "bar2")),
            bodyParams = Some(Map("foo" -> "bar", "foo2" -> "bar2"))))
      )
  }

  test("test collectRouteInformationFromRequest with ng route with parameters (POST request)") {
    val r = Request(Method.Post, requestPath)
    r.mediaType = MediaType.WwwForm
    r.contentString = "foo=bar&foo2=bar2"
    RouteInformationHandler.collectRouteInformationFromRequest(
      request = r,
      ngRoute = Some(routeNoTags)) mustBe
      RouteInformation(
        isNgRoute = true,
        routeTags = None,
        requestInformation = Some(
          testDefaultRequestInfo
            .copy(
              method = "POST",
              queryParams = Some(Map()),
              bodyParams = Some(Map("foo" -> "bar", "foo2" -> "bar2"))))
      )
  }

}
