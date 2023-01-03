package com.twitter.auth.customerauthtooling.cli.commands.converters

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptLongSetConverterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val converter = new OptLongSetConverter()
  test("test LongSetConverter with empty value") {
    converter.convert("") mustBe Some(LongSetWrapper(Set()))
  }

  test("test LongSetConverter with one item") {
    converter.convert("123") mustBe Some(LongSetWrapper(Set(123L)))
  }

  test("test LongSetConverter with multiple items") {
    converter.convert("1,2,5,76") mustBe Some(LongSetWrapper(Set(1L, 2L, 5L, 76L)))
  }

  test("test LongSetConverter with incorrect items #1") {
    intercept[InvalidInputException] {
      converter.convert("1,")
    }
  }

  test("test LongSetConverter with incorrect items #2") {
    intercept[InvalidInputException] {
      converter.convert("a")
    }
  }

  test("test LongSetConverter with incorrect items #3") {
    intercept[InvalidInputException] {
      converter.convert("1,b")
    }
  }
}
