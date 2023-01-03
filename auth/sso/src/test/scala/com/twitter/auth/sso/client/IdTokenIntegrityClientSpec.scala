package com.twitter.auth.sso.client

import com.twitter.auth.sso.models.IdToken
import com.twitter.auth.sso.models.SsoProvider
import com.twitter.auth.sso.models.SsoProviderInfo
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class IdTokenIntegrityClientSpec extends AnyFunSuite with Matchers {
  val TestSsoProviderClient: SsoProviderClient = new SsoProviderClient {
    override def isIdTokenValid(idToken: IdToken): Future[Boolean] = Future.False
    override def extractSsoProviderInfo(idToken: IdToken): SsoProviderInfo =
      SsoProviderInfo("", "")
  }

  val SsoProviders: Map[SsoProvider, SsoProviderClient] = Map(
    SsoProvider.Test -> TestSsoProviderClient)
  val client = new IdTokenIntegrityClient(SsoProviders)

  test("#getProvider throws MissingProvider exception when provider is not defined") {
    intercept[MissingProvider.type] {
      client.getProvider(SsoProvider.Google)
    }
  }
}
