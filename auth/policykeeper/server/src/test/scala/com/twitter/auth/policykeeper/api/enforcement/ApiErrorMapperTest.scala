package com.twitter.auth.policykeeper.api.enforcement

import com.twitter.finatra.api11.ApiError
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ApiErrorMapperTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  val apiErrorMapper = ApiErrorMapper()

  test("test apiErrorMapper, code 131") {
    apiErrorMapper.getApiErrorByCode(131) mustBe Some(ApiError.InternalError)
  }

  test("test apiErrorMapper, code 214") {
    apiErrorMapper.getApiErrorByCode(214) mustBe Some(ApiError.GenericBadRequest)
  }

  test("test apiErrorMapper, code 410") {
    apiErrorMapper.getApiErrorByCode(410) mustBe Some(ApiError.PasswordVerificationRequired)
  }

  test("test apiErrorMapper, code 0") {
    apiErrorMapper.getApiErrorByCode(0) mustBe Some(ApiError.DefaultApiError)
  }

  test("test apiErrorMapper, code 100000") {
    apiErrorMapper.getApiErrorByCode(100000) mustBe None
  }

}
