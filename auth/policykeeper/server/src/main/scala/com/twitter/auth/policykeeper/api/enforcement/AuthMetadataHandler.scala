package com.twitter.auth.policykeeper.api.enforcement

import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.finagle.http.Request
import com.twitter.passbird.LegacyAuthContext
import com.twitter.tsla.authevents.thriftscala.AuthEvent
import com.twitter.tsla.authevents.thriftscala.AuthEventType
import com.twitter.tsla.thrift.authevents.{AuthEvent => JAuthEvent}
import com.twitter.passbird.thrift.accesstoken.{AccessToken => JAccessToken}
import com.twitter.finatra.gizmoduck.GizmoduckUserContext._
import scala.collection.JavaConverters._

object AuthMetadataHandler {
  private def fromJavaEvents(authEvents: java.util.List[JAuthEvent]): Seq[AuthEvent] = {
    authEvents.asScala
      .map(je =>
        AuthEvent(
          authEventType = AuthEventType(je.authEventType.getValue),
          authTimeMs = if (je.isSetAuthTimeMs) {
            Some(je.authTimeMs)
          } else {
            None
          })).toList
  }

  /**
   * Extracts auth metadata for policykeeper service from TFE request context
   *
   * @param request
   * @return
   */
  def collectAuthMetadataFromRequest(request: Request): Option[AuthMetadata] = {
    val sessionToken = LegacyAuthContext.getSessionTokenFromLocalContexts
    val accessToken = LegacyAuthContext.getAccessTokenFromLocalContexts
    // extract auth events from session token with fallback to access token
    val authEvents = sessionToken.orElse(accessToken) match {
      case Some(token: JAccessToken) =>
        if (token.isSetAuth_events) {
          Some(fromJavaEvents(token.auth_events))
        } else {
          None
        }
      case _ =>
        None
    }
    // extract token information from access token
    val (hasAccessToken, tokenString, tokenKind) = accessToken match {
      case Some(token: JAccessToken) =>
        (
          true,
          if (token.isSetToken) {
            Some(token.token)
          } else {
            None
          },
          if (token.isSetToken_type) {
            Some(token.token_type)
          } else {
            None
          },
        )
      case _ =>
        (false, None, None)
    }
    Some(
      AuthMetadata(
        authEvents = authEvents,
        hasAccessToken = hasAccessToken,
        gizmoduckUserId = request.gizmoduckUser.map(_.id),
        token = tokenString,
        tokenKind = tokenKind
      ))
  }
}
