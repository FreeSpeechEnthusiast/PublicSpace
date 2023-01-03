package com.twitter.auth.authentication.utils

import org.junit.runner.RunWith
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AuthenticationUtilsSpec extends AnyFunSuite with Matchers {

  test("isValidBasicAuthHeader") {
    val invalidAuthHeader1 = ""
    val invalidAuthHeader2 = "abc"
    val invalidAuthHeader3 = "basic aaa"
    val invalidAuthHeader4 = "basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
    val invalidAuthHeader5 = "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ="
    val validAuthHeader = "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="

    AuthenticationUtils.isValidBasicAuthHeader(
      invalidAuthHeader1,
      "Aladdin",
      "open sesame") mustEqual false
    AuthenticationUtils.isValidBasicAuthHeader(
      invalidAuthHeader2,
      "Aladdin",
      "open sesame") mustEqual false
    AuthenticationUtils.isValidBasicAuthHeader(
      invalidAuthHeader3,
      "Aladdin",
      "open sesame") mustEqual false
    AuthenticationUtils.isValidBasicAuthHeader(
      invalidAuthHeader4,
      "Aladdin",
      "open sesame") mustEqual false
    AuthenticationUtils.isValidBasicAuthHeader(
      invalidAuthHeader5,
      "Aladdin",
      "open sesame") mustEqual false
    AuthenticationUtils.isValidBasicAuthHeader(validAuthHeader, "aaaa", "bbb") mustEqual false
    AuthenticationUtils.isValidBasicAuthHeader(
      validAuthHeader,
      "Aladdin",
      "open sesame") mustEqual true
  }

}
