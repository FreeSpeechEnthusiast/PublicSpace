package com.twitter.auth.pasetoheaders.s2s.token

import com.twitter.auth.pasetoheaders.finagle.ConfigBusSource
import com.twitter.auth.pasetoheaders.finagle.ServiceBootException
import com.twitter.auth.pasetoheaders.s2s.models.Principals
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.logging.Logger
import org.hamcrest.MatcherAssert
import org.hamcrest.core.StringContains
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PrincipalSignerTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with BeforeAndAfter {

  protected val testEnv = "local"
  protected val testIssuer = "unittest"
  protected val testVersion = 1

  private[this] val statsReceiver = new InMemoryStatsReceiver
  private[this] val logger = Logger.get()

  private[this] val testPrivateKeysFolder = "test-principal-tokens"

  private[this] val signer = new PrincipalSigner(
    logger = Some(logger),
    stats = Some(statsReceiver),
    brokerServiceEnv = testEnv,
    brokerServiceName = testIssuer,
    privateKeysSourceDir = ConfigBusSource.resource(testPrivateKeysFolder)
  )

  protected val ldapUser: Principals.LdapUser = Principals.LdapUser(name = "twitterUser")

  before {
    statsReceiver.clear()
  }

  test("test signer with existing key with default filename") {
    val token = signer.signToken(principal = ldapUser)
    println(token)
    token match {
      case Some(t) =>
        MatcherAssert.assertThat(t, StringContains.containsString("v2.public."))
      case None => fail()
    }

    assert(signer.getEnvironment == testEnv)
    assert(signer.getIssuer == testIssuer)
  }

  test("test signer with existing key with default filename using configured key version") {
    val token = signer.signTokenUsingConfiguredKeyVersion(principal = ldapUser)
    println(token)
    token match {
      case Some(t) =>
        MatcherAssert.assertThat(t, StringContains.containsString("v2.public."))
      case None => fail()
    }

    assert(signer.getEnvironment == testEnv)
    assert(signer.getIssuer == testIssuer)
  }

  test("test signer with missing config file") {
    intercept[ServiceBootException] {
      new PrincipalSigner(
        logger = Some(logger),
        stats = Some(statsReceiver),
        brokerServiceEnv = testEnv,
        brokerServiceName = testIssuer,
        privateKeysSourceDir = ConfigBusSource.resource("wrong-path")
      )
    }

    statsReceiver.counters(List("PrincipalSigner", "config_loading_timeout")) mustEqual 1L
  }

  test("test signer with invalid config file") {
    intercept[ServiceBootException] {
      new PrincipalSigner(
        logger = Some(logger),
        stats = Some(statsReceiver),
        brokerServiceEnv = testEnv,
        brokerServiceName = testIssuer,
        privateKeysSourceDir = ConfigBusSource.resource(testPrivateKeysFolder),
        privateKeyFileName = "broken.json"
      )
    }

    statsReceiver.counters(List("PrincipalSigner", "config_loading_timeout")) mustEqual 1L
  }

}
