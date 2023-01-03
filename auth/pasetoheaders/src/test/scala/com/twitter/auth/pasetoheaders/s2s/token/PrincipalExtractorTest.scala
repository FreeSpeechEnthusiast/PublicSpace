package com.twitter.auth.pasetoheaders.s2s.token

import com.twitter.auth.pasetoheaders.finagle.ConfigBusSource
import com.twitter.auth.pasetoheaders.finagle.ServiceBootException
import com.twitter.auth.pasetoheaders.s2s.models.Principals
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.logging.Logger
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PrincipalExtractorTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with BeforeAndAfter {

  protected val testLdapUser: Principals.LdapUser = Principals.LdapUser(name = "twitterUser")

  private val testToken =
    "v2.public.eyJwcmluY2lwYWwiOnsicHJsIjoidXNyIiwibmFtIjoidHdpdHRlclVzZXIifSwiaXNzIjoidW5pdHRlc3QiLCJleHAiOiIyMDMyLTEwLTA0VDE3OjM0OjE4LjY0NTQ5KzAwOjAwIiwiaWF0IjoiMjAyMi0xMC0wN1QxNzozNDoxOC42NDU0OSswMDowMCJ9HC8NBXTCTWr7FVLH-SUpDeRZL0_Uk9KukEEhcP_xitKQH1IXfAUQLAnCfLVZA8psOoU54MWV6zRG25v-pbBuDA.eyJtb2RlbFZlcnNpb24iOjEsImtpZCI6ImxvY2FsOnVuaXR0ZXN0OjEifQ"

  private[this] val statsReceiver = new InMemoryStatsReceiver
  private[this] val logger = Logger.get()

  private[this] val testPublicKeysFolder = "test-principal-tokens"

  before {
    statsReceiver.clear()
  }

  private[this] val extractor = new PrincipalExtractor(
    logger = Some(logger),
    stats = Some(statsReceiver),
    publicKeysSourceDir = ConfigBusSource.resource(testPublicKeysFolder),
  )

  test("test extractor with existing key") {
    val claims = extractor.extractClaims(testToken)
    claims match {
      case Some(c) =>
        assertEquals(classOf[Principals.LdapUser], c.getEnclosedEntity.getClass)
        assertEquals(testLdapUser, c.getEnclosedEntity)
      case None => fail()
    }
  }

  test("test extractor without integrity check") {
    val claims = extractor.extractClaims(testToken, verifyIntegrity = false)
    claims match {
      case Some(c) =>
        assertEquals(classOf[Principals.LdapUser], c.getEnclosedEntity.getClass)
        assertEquals(testLdapUser, c.getEnclosedEntity)
      case None => fail()
    }
  }

  test("test fast failing if config is missing") {
    intercept[ServiceBootException] {
      new PrincipalExtractor(
        logger = Some(logger),
        stats = Some(statsReceiver),
        publicKeysSourceDir = ConfigBusSource.resource(testPublicKeysFolder),
        publicKeyFileName = "missing.json"
      )
    }

    statsReceiver.counters(List("PrincipalExtractor", "config_loading_timeout")) mustEqual 1L
  }

  test("test fast failing if config is invalid") {
    intercept[ServiceBootException] {
      new PrincipalExtractor(
        logger = Some(logger),
        stats = Some(statsReceiver),
        publicKeysSourceDir = ConfigBusSource.resource(testPublicKeysFolder),
        publicKeyFileName = "broken.json"
      )
    }

    statsReceiver.counters(List("PrincipalExtractor", "config_loading_timeout")) mustEqual 1L
  }
}
