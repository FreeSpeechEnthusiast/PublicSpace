package com.twitter.auth.pasetoheaders.finagle

import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.auth.pasetoheaders.javahelpers.MapConv._
import com.twitter.finagle.stats.InMemoryStatsReceiver
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, MustMatchers, OneInstancePerTest}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StatsConnectorTest
    extends AnyFunSuite
    with OneInstancePerTest
    with MustMatchers
    with BeforeAndAfter {

  private[this] val statsReceiver = new InMemoryStatsReceiver
  private[this] val statsConnector = FinagleStatsProxy(statsReceiver)

  before {}

  after {
    statsReceiver.clear()
  }

  test("test stats connector through interface (signing_requested)") {
    statsConnector.counter("signing_service", "signing_requested", None).incr(1L)
    statsReceiver.counters(List("paseto", "signingservice", "signing_requested")) mustEqual 1L
  }

  test("test stats connector through interface (private_key_not_found)") {
    statsConnector.counter("signing_service", "private_key_not_found", None).incr(1L)
    statsReceiver.counters(List("paseto", "signingservice", "private_key_not_found")) mustEqual 1L
  }

  test("test stats connector through interface (signing_completed)") {
    statsConnector.counter("signing_service", "signing_completed", None).incr(1L)
    statsReceiver.counters(List("paseto", "signingservice", "signing_completed")) mustEqual 1L
  }

  test("test stats connector through interface (token_size)") {
    statsConnector.counter("signing_service", "token_size", None).incr(33L)
    statsReceiver.counters(List("paseto", "signingservice", "token_size")) mustEqual 33L
  }

  test("test stats connector through interface (extraction_requested)") {
    statsConnector.counter("claim_service", "extraction_requested", None).incr(1L)
    statsReceiver.counters(List("paseto", "claimservice", "extraction_requested")) mustEqual 1L
  }

  test("test stats connector through interface (extraction_succeeded)") {
    statsConnector.counter("claim_service", "extraction_succeeded", None).incr(1L)
    statsReceiver.counters(List("paseto", "claimservice", "extraction_succeeded")) mustEqual 1L
  }

  test("test stats connector through interface (extraction_failed)") {
    statsConnector.counter("claim_service", "extraction_failed", None).incr(1L)
    statsReceiver.counters(List("paseto", "claimservice", "extraction_failed")) mustEqual 1L
  }

  test("test stats connector through interface (unverified_extraction_requested)") {
    statsConnector.counter("claim_service", "unverified_extraction_requested", None).incr(1L)
    statsReceiver.counters(
      List("paseto", "claimservice", "unverified_extraction_requested")) mustEqual 1L
  }

  test("test stats connector through interface (unverified_extraction_succeeded)") {
    statsConnector.counter("claim_service", "unverified_extraction_succeeded", None).incr(1L)
    statsReceiver.counters(
      List("paseto", "claimservice", "unverified_extraction_succeeded")) mustEqual 1L
  }

  test("test stats connector through interface (unverified_extraction_failed)") {
    statsConnector.counter("claim_service", "unverified_extraction_failed", None).incr(1L)
    statsReceiver.counters(
      List("paseto", "claimservice", "unverified_extraction_failed")) mustEqual 1L
  }

  test("test stats connector through interface (wrong_key_identifier)") {
    statsConnector.counter("claim_service", "wrong_key_identifier", None).incr(1L)
    statsReceiver.counters(List("paseto", "claimservice", "wrong_key_identifier")) mustEqual 1L
  }

  test("test stats connector through interface (public_key_not_found)") {
    statsConnector.counter("claim_service", "public_key_not_found", None).incr(1L)
    statsReceiver.counters(List("paseto", "claimservice", "public_key_not_found")) mustEqual 1L
  }

  test("test stats connector through interface (unknown counter)") {
    statsConnector.counter("claim_service", "unknown", None).incr(1L)
    // should not throw an exception
  }

  test("test stats connector through interface with metadata") {
    statsConnector
      .counter("claim_service", "public_key_not_found", Some(Map("key" -> "val"))).incr(1L)
    statsReceiver.counters(List("paseto", "claimservice", "public_key_not_found")) mustEqual 1L
  }

}
