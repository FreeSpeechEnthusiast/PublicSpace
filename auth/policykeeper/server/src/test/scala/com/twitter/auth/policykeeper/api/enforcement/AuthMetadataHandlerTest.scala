package com.twitter.auth.policykeeper.api.enforcement

import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.finagle.context.Contexts
import com.twitter.finagle.http.Request
import com.twitter.passbird.LegacyAuthContext
import com.twitter.passbird.LegacyAuthContextKey
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import com.twitter.passbird.thrift.accesstoken.{AccessToken => JAccessToken}
import com.twitter.tsla.authevents.thriftscala.AuthEvent
import com.twitter.tsla.authevents.thriftscala.AuthEventType
import com.twitter.tsla.thrift.authevents.{AuthEventType => JAuthEventType}
import com.twitter.tsla.thrift.authevents.{AuthEvent => JAuthEvent}
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class AuthMetadataHandlerTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  val TestAccessToken = new JAccessToken()
    .setId(1)
    .setUser_id(1)
    .setClient_application_id(1)
    .setToken("1-foo")
    .setSecret("bar")
    .setCreated_at(1)
    .setUpdated_at(2)
    .setAuthorized_at(3)
    .setIs_writable(true)
    .setToken_type(1)

  test("test collectAuthMetadataFromRequest with session token and access token") {
    Contexts.local
      .let(
        LegacyAuthContextKey,
        LegacyAuthContext(
          accessToken = Some(TestAccessToken),
          clientApplication = None,
          sessionToken = Some(
            TestAccessToken.setAuth_events(
              List(new JAuthEvent(JAuthEventType.EMAIL_OTP_ADDED)).asJava)),
          requestToken = None,
          guestContext = None,
          tiaContext = None,
          contributorId = None,
          scopes = None
        )
      ) {
        AuthMetadataHandler.collectAuthMetadataFromRequest(Request("/test")) mustBe Some(
          AuthMetadata(
            authEvents = Some(Seq(AuthEvent(AuthEventType.EmailOtpAdded))),
            hasAccessToken = true,
            gizmoduckUserId = None,
            token = Some("1-foo"),
            tokenKind = Some(1)))
      }
  }

  test("test collectAuthMetadataFromRequest with token and auth events") {
    Contexts.local
      .let(
        LegacyAuthContextKey,
        LegacyAuthContext(
          accessToken = Some(
            TestAccessToken.setAuth_events(
              List(new JAuthEvent(JAuthEventType.EMAIL_OTP_ADDED)).asJava)),
          clientApplication = None,
          sessionToken = None,
          requestToken = None,
          guestContext = None,
          tiaContext = None,
          contributorId = None,
          scopes = None
        )
      ) {
        AuthMetadataHandler.collectAuthMetadataFromRequest(Request("/test")) mustBe Some(
          AuthMetadata(
            hasAccessToken = true,
            authEvents = Some(Seq(AuthEvent(AuthEventType.EmailOtpAdded))),
            gizmoduckUserId = None,
            token = Some("1-foo"),
            tokenKind = Some(1)))
      }
  }

  test("test collectAuthMetadataFromRequest with token") {
    Contexts.local
      .let(
        LegacyAuthContextKey,
        LegacyAuthContext(
          accessToken = Some(TestAccessToken),
          clientApplication = None,
          sessionToken = None,
          requestToken = None,
          guestContext = None,
          tiaContext = None,
          contributorId = None,
          scopes = None
        )
      ) {
        AuthMetadataHandler.collectAuthMetadataFromRequest(Request("/test")) mustBe Some(
          AuthMetadata(
            hasAccessToken = true,
            authEvents = None,
            gizmoduckUserId = None,
            token = Some("1-foo"),
            tokenKind = Some(1)))
      }
  }

  test("test collectAuthMetadataFromRequest without token") {
    AuthMetadataHandler.collectAuthMetadataFromRequest(Request("/test")) mustBe Some(
      AuthMetadata(
        hasAccessToken = false,
        authEvents = None,
        gizmoduckUserId = None,
        token = None,
        tokenKind = None))
  }

}
