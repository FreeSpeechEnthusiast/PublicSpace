package com.twitter.auth.customerauthtooling.cli.commands.converters

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptBooleanConverterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val converter = new OptBooleanConverter()
  test("test OptionalBooleanConverter with empty value") {
    converter.convert("") mustBe None
  }

  test("test OptionalBooleanConverter with incorrect input #1") {
    intercept[InvalidInputException] {
      converter.convert("a")
    }
  }

  test("test OptionalBooleanConverter with 1 input") {
    converter.convert("1") mustBe Some(true)
  }

  test("test OptionalBooleanConverter with true input") {
    converter.convert("true") mustBe Some(true)
  }

  test("test OptionalBooleanConverter with 0 input") {
    converter.convert("0") mustBe Some(false)
  }

  test("test OptionalBooleanConverter with false input") {
    converter.convert("false") mustBe Some(false)
  }
}
