package com.twitter.auth.customerauthtooling.cli.commands.converters

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptStringConverterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val converter = new OptStringConverter()
  test("test OptStringConverter with empty value") {
    converter.convert("") mustBe None
  }

  test("test OptStringConverter with incorrect input #1") {
    intercept[InvalidInputException] {
      converter.convert("as dsd")
    }
  }

  test("test OptStringConverter with incorrect input #2") {
    intercept[InvalidInputException] {
      converter.convert("?@9#01")
    }
  }

  test("test OptStringConverter with correct input #1") {
    converter.convert("1") mustBe Some("1")
  }

  test("test OptStringConverter with correct input #2") {
    converter.convert("true") mustBe Some("true")
  }

  test("test OptStringConverter with correct input #3") {
    converter.convert("http://abcd") mustBe Some("http://abcd")
  }

  test("test OptStringConverter with correct input #4") {
    converter.convert(
      "GET/tfetestservice/customerauthtooling->cluster:tfe_test_service_mtls") mustBe Some(
      "GET/tfetestservice/customerauthtooling->cluster:tfe_test_service_mtls")
  }

}
