package com.twitter.auth.policykeeper.api.dataproviders.implementations.input

import com.twitter.auth.policykeeper.thriftscala.RequestInformation
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class InputDataProviderTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val inputDataProvider = InputDataProvider()

  test("test InputDataProvider with empty request information") {
    Await.result(
      inputDataProvider.getData(Some(RouteInformation(false, None, None)), None)
    ) mustBe Map.empty[String, String]
  }

  test("test InputDataProvider with some request information and empty bodyParams") {
    Await.result(
      inputDataProvider
        .getData(
          Some(
            RouteInformation(
              isNgRoute = true,
              routeTags = None,
              requestInformation = Some(
                RequestInformation(
                  path = "/",
                  host = None,
                  method = "GET",
                  queryParams = None,
                  bodyParams = None
                )
              )
            )),
          None
        )) mustBe Map.empty[String, String]
  }

  test("test InputDataProvider with some request information and bodyParams") {
    Await.result(
      inputDataProvider
        .getData(
          Some(
            RouteInformation(
              isNgRoute = true,
              routeTags = None,
              requestInformation = Some(
                RequestInformation(
                  path = "/",
                  host = None,
                  method = "GET",
                  queryParams = None,
                  bodyParams = Some(Map(
                    "current_password" -> "qwerty"
                  ))))
            )),
          None
        )
    ) mustBe Map("current_password" -> "qwerty", "redirect_after_verify" -> "/")
  }

}
