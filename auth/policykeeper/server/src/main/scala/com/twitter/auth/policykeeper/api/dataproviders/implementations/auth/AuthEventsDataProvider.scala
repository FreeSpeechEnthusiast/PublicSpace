package com.twitter.auth.policykeeper.api.dataproviders.implementations.auth

import com.twitter.auth.policykeeper.api.dataproviders.DataProviderInterface
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.tsla.authevents.thriftscala.AuthEvent
import com.twitter.tsla.authevents.thriftscala.AuthEventType
import com.twitter.tsla.authevents.thriftscala.AuthEventType.PasswordVerified
import com.twitter.util.Future

case class AuthEventsDataProvider() extends DataProviderInterface {

  /**
   * Returns provided namespace
   *
   * @return
   */
  override def namespace(): String = "auth_events"

  /**
   * Returns list of provided variable names
   *
   * @return
   */
  override def provides(): Seq[String] =
    Seq("lastPasswordVerifiedTimestampMs")

  object AuthEventLatestTimestampOrdering extends Ordering[AuthEvent] {
    def compare(a: AuthEvent, b: AuthEvent): Int = {
      (a._2, b._2) match {
        case (Some(aTimestamp), Some(bTimestamp)) => aTimestamp compare bTimestamp
        case (Some(_), None) => 1
        case (None, Some(_)) => -1
        case _ => 0
      }
    }
  }

  private[auth] def mapToLatestAuthEvents(
    authEvents: Option[Seq[AuthEvent]]
  ): Option[Map[AuthEventType, Option[Long]]] = {
    authEvents match {
      case Some(events) =>
        Some(
          events
          // group by auth event type
            .groupBy(_._1)
            // select the latest event for each type
            .map {
              case (eventType, timestamps) =>
                (eventType, timestamps.max(AuthEventLatestTimestampOrdering)._2)
            })
      case None => None
    }
  }

  private def lastAuthEventTimestamp(
    latestAuthEvents: Option[Map[AuthEventType, Option[Long]]],
    eventType: AuthEventType
  ): Option[Long] = {
    latestAuthEvents match {
      case Some(events) =>
        events.get(eventType) match {
          case Some(maybeTimestamp) => maybeTimestamp
          case None => None
        }
      case None => None
    }
  }

  /**
   * Returns map of variable names with their values
   *
   * @param routeInformation
   * @param authMetadata
   *
   * @return
   */
  override def returnsData(
    routeInformation: Option[RouteInformation],
    authMetadata: Option[AuthMetadata]
  ): Future[Map[String, Any]] = {
    val latestAuthEvents = authMetadata match {
      case Some(m) =>
        mapToLatestAuthEvents(m.authEvents)
      case None => None
    }
    Future.value(
      deleteNoneValues(
        Map(
          "lastPasswordVerifiedTimestampMs" -> lastAuthEventTimestamp(
            latestAuthEvents,
            PasswordVerified),
        ))
    )
  }
}
