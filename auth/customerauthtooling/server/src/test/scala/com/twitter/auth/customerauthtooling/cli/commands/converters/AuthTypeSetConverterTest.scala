package com.twitter.auth.customerauthtooling.cli.commands.converters

import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AuthTypeSetConverterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val converter = new AuthTypeSetConverter()

  test("test AuthTypeSetConverter with incorrect input #1") {
    intercept[InvalidInputException] {
      converter.convert(",")
    }
  }

  test("test AuthTypeSetConverter with incorrect input #2") {
    intercept[InvalidInputException] {
      converter.convert("unknownAuth")
    }
  }

  test("test AuthTypeSetConverter with incorrect input #3") {
    intercept[InvalidInputException] {
      converter.convert("")
    }
  }

  test("test AuthTypeSetConverter with 1 input") {
    converter.convert("unknown") mustBe AuthTypeSetWrapper(Set(AuthenticationType.Unknown))
  }

  test("test AuthTypeSetConverter with 2 input") {
    converter.convert("oauth1,session") mustBe
      AuthTypeSetWrapper(Set(AuthenticationType.Oauth1, AuthenticationType.Session))
  }

}
