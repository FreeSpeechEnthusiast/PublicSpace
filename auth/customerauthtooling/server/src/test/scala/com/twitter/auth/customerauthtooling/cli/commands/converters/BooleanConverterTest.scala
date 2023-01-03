package com.twitter.auth.customerauthtooling.cli.commands.converters

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BooleanConverterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val converter = new BooleanConverter()

  test("test BooleanConverter with incorrect input #1") {
    intercept[InvalidInputException] {
      converter.convert("")
    }
  }

  test("test BooleanConverter with 1 input") {
    converter.convert("1") mustBe true
  }

  test("test BooleanConverter with true input") {
    converter.convert("true") mustBe true
  }

  test("test BooleanConverter with 0 input") {
    converter.convert("0") mustBe false
  }

  test("test BooleanConverter with false input") {
    converter.convert("false") mustBe false
  }
}
