package com.twitter.auth.authentication.models

import com.twitter.finagle.http.Request
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class RequestWithAmpEmailTokenSpec
    extends AnyFunSuite
    with MockitoSugar
    with Matchers
    with BeforeAndAfterEach {

  private val testRequest = Request("/test")
  private val testRequestAmpToken = Request("/test?p=1&amp_email_token=tkn")
  private val testRequestAmpToken2 = Request("/test?amp_email_token=tkn&abcd=defg")

  test("RequestWithAmpEmailToken extractor test (no token)") {
    testRequest match {
      case RequestWithAmpEmailToken(_) => fail()
      case _ => succeed
    }
  }

  test("RequestWithAmpEmailToken extractor test (with token)") {
    testRequestAmpToken match {
      case RequestWithAmpEmailToken(tkn) =>
        tkn mustBe "tkn"
      case _ => fail()
    }
  }

  test("RequestWithAmpEmailToken extractor test (with token 2)") {
    testRequestAmpToken2 match {
      case RequestWithAmpEmailToken(tkn) =>
        tkn mustBe "tkn"
      case _ => fail()
    }
  }

  test("redactToken test") {
    RequestWithAmpEmailToken.redactToken(testRequestAmpToken)
    testRequestAmpToken.uri mustBe "/test?p=1&amp_email_token=xxx"
  }

  test("redactToken test (with token 2)") {
    RequestWithAmpEmailToken.redactToken(testRequestAmpToken2)
    testRequestAmpToken2.uri mustBe "/test?abcd=defg&amp_email_token=xxx"
  }
}
