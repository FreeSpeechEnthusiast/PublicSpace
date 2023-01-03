package com.twitter.auth.policykeeper.api.dataproviders.implementations.auth

import com.twitter.auth.policykeeper.api.context.LocalContext
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AuthDataProviderTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val authDataProvider = AuthDataProvider()

  test("test AuthDataProvider with full context") {
    LocalContext.writeToLocalContexts(Some(123L), Some(456L), Some("sss"), Some(1L), Some(888L)) {
      Await.result(authDataProvider.getData(None, None)) mustBe Map(
        "clientApplicationId" -> 123L,
        "encodedSessionHash" -> "sss",
        "sessionHash" -> "sss",
        "userId" -> 456L,
        "guestToken" -> 1L,
        "authenticatedUserId" -> 888L
      )
    }
  }

  test("test AuthDataProvider with partial context") {
    LocalContext.writeToLocalContexts(Some(123L), None, Some("sss")) {
      Await.result(authDataProvider.getData(None, None)) mustBe Map(
        "clientApplicationId" -> 123L,
        "encodedSessionHash" -> "sss",
        "sessionHash" -> "sss",
      )
    }
  }

}
