package com.twitter.auth.customerauthtooling.api.models

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RouteInformationTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val publicNgRoute =
    RouteInformation(
      path = "/endpoint",
      domains = Set("api.twitter.com"),
      isNgRoute = true,
      cluster = "test")
  private val privateRoute =
    RouteInformation(
      path = "/endpoint",
      domains = Set("twitter.biz"),
      isNgRoute = false,
      cluster = "test")

  test("test private route isInternal is true") {
    privateRoute.isInternal mustBe true
  }

  test("test public route isInternal is false") {
    publicNgRoute.isInternal mustBe false
  }

}
