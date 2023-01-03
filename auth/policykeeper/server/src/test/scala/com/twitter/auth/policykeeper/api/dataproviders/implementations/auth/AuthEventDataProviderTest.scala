package com.twitter.auth.policykeeper.api.dataproviders.implementations.auth

import com.twitter.tsla.authevents.thriftscala.AuthEvent
import com.twitter.tsla.authevents.thriftscala.AuthEventType
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AuthEventDataProviderTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val authEventsDataProvider = AuthEventsDataProvider()

  test("test mapToLatestAuthEvents without events") {
    authEventsDataProvider.mapToLatestAuthEvents(None) mustBe None
  }

  test("test mapToLatestAuthEvents with one event") {
    authEventsDataProvider.mapToLatestAuthEvents(
      Some(Seq(
        AuthEvent(AuthEventType.PasswordVerified, Some(1200L)),
        AuthEvent(AuthEventType.PasswordVerified, Some(1000L)),
        AuthEvent(AuthEventType.PasswordVerified, Some(1600L))
      ))) mustBe Some(Map(AuthEventType.PasswordVerified -> Some(1600L)))
  }

  test("test mapToLatestAuthEvents with one event and empty timestamp") {
    authEventsDataProvider.mapToLatestAuthEvents(
      Some(Seq(
        AuthEvent(AuthEventType.PasswordVerified, None),
        AuthEvent(AuthEventType.PasswordVerified, Some(1200L)),
        AuthEvent(AuthEventType.PasswordVerified, Some(1000L)),
        AuthEvent(AuthEventType.PasswordVerified, Some(1600L))
      ))) mustBe Some(Map(AuthEventType.PasswordVerified -> Some(1600L)))
  }

  test("test mapToLatestAuthEvents with one event and two empty timestamps") {
    authEventsDataProvider.mapToLatestAuthEvents(
      Some(Seq(
        AuthEvent(AuthEventType.PasswordVerified, None),
        AuthEvent(AuthEventType.PasswordVerified, Some(1200L)),
        AuthEvent(AuthEventType.PasswordVerified, Some(1000L)),
        AuthEvent(AuthEventType.PasswordVerified, Some(1600L)),
        AuthEvent(AuthEventType.PasswordVerified, None),
      ))) mustBe Some(Map(AuthEventType.PasswordVerified -> Some(1600L)))
  }

  test("test mapToLatestAuthEvents with all empty timestamps") {
    authEventsDataProvider.mapToLatestAuthEvents(
      Some(
        Seq(
          AuthEvent(AuthEventType.PasswordVerified, None),
          AuthEvent(AuthEventType.PasswordVerified, None),
        ))) mustBe Some(Map(AuthEventType.PasswordVerified -> None))
  }

  test("test mapToLatestAuthEvents with multiple event") {
    authEventsDataProvider.mapToLatestAuthEvents(
      Some(Seq(
        AuthEvent(AuthEventType.PasswordVerified, Some(1200L)),
        AuthEvent(AuthEventType.EmailOtpAdded, Some(1200L)),
        AuthEvent(AuthEventType.PasswordVerified, None),
        AuthEvent(AuthEventType.PasswordVerified, Some(1700L)),
        AuthEvent(AuthEventType.EmailOtpAdded, Some(1400L)),
        AuthEvent(AuthEventType.PushOtpVerified, None),
        AuthEvent(AuthEventType.PasswordVerified, Some(1200L)),
        AuthEvent(AuthEventType.PushOtpVerified, Some(1000L)),
        AuthEvent(AuthEventType.PasswordVerified, Some(1600L))
      ))) mustBe Some(
      Map(
        AuthEventType.PasswordVerified -> Some(1700L),
        AuthEventType.EmailOtpAdded -> Some(1400L),
        AuthEventType.PushOtpVerified -> Some(1000L)))
  }

}
