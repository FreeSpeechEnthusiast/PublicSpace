package com.twitter.auth.customerauthtooling.cli.commands.converters

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StringSetConverterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val converter = new StringSetConverter()

  test("test StringSetConverter with one item") {
    converter.convert("abcd") mustBe StringSetWrapper(Set("abcd"))
  }

  test("test StringSetConverter with multiple items") {
    converter.convert("a.b,b,c/,d") mustBe StringSetWrapper(Set("a.b", "b", "c/", "d"))
  }

  test("test StringSetConverter with incorrect items #1") {
    intercept[InvalidInputException] {
      converter.convert("")
    }
  }

  test("test StringSetConverter with incorrect items #2") {
    intercept[InvalidInputException] {
      converter.convert("#")
    }
  }

  test("test StringSetConverter with incorrect items #3") {
    intercept[InvalidInputException] {
      converter.convert("1,?")
    }
  }
}
