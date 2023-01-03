package com.twitter.auth.customerauthtooling.cli.commands.converters

import com.twitter.auth.customerauthtooling.thriftscala.RequestMethod
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptRequestMethodConverterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val converter = new OptRequestMethodConverter()
  test("test OptionalRequestMethodConverter with empty value") {
    converter.convert("") mustBe None
  }

  test("test OptionalRequestMethodConverter with incorrect input") {
    intercept[InvalidInputException] {
      converter.convert("a")
    }
  }

  test("test OptionalRequestMethodConverter with valid input") {
    converter.convert("GET") mustBe Some(RequestMethod.Get)
  }
}
