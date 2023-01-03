package com.twitter.auth.customerauthtooling.cli.commands.converters

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptStringSetConverterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val converter = new OptStringSetConverter()
  test("test OptStringSetConverter with empty value") {
    converter.convert("") mustBe Some(StringSetWrapper(Set()))
  }

  test("test OptStringSetConverter with one item") {
    converter.convert("abcd") mustBe Some(StringSetWrapper(Set("abcd")))
  }

  test("test OptStringSetConverter with multiple items") {
    converter.convert("a.b,b,c/,d") mustBe Some(StringSetWrapper(Set("a.b", "b", "c/", "d")))
  }

  test("test OptStringSetConverter with incorrect items #1") {
    intercept[InvalidInputException] {
      converter.convert("1,")
    }
  }

  test("test OptStringSetConverter with incorrect items #2") {
    intercept[InvalidInputException] {
      converter.convert("#")
    }
  }

  test("test OptStringSetConverter with incorrect items #3") {
    intercept[InvalidInputException] {
      converter.convert("1,?")
    }
  }
}
