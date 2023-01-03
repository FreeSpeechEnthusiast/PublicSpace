package com.twitter.auth.customerauthtooling.cli.commands.converters

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptIntConverterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val converter = new OptIntConverter()
  test("test OptIntConverter with empty value") {
    converter.convert("") mustBe None
  }

  test("test OptIntConverter with incorrect input #1") {
    intercept[InvalidInputException] {
      converter.convert("a")
    }
  }

  test("test OptIntConverter with incorrect input #2") {
    intercept[InvalidInputException] {
      converter.convert("0.1")
    }
  }

  test("test OptIntConverter with valid input #1") {
    converter.convert("1213") mustBe Some(1213)
  }

  test("test OptIntConverter with valid input #2") {
    converter.convert("6") mustBe Some(6)
  }
}
