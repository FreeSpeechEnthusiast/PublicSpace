package com.twitter.auth.filters.ampemail

import com.google.inject.Inject
import com.google.inject.Singleton
import com.twitter.finagle.Service
import com.twitter.finagle.SimpleFilter
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import com.twitter.finatra.http.exceptions.ForbiddenException
import scala.util.matching.Regex

/**
 * The filter adds support of CORS for Email headers (version 2)
 * See more information at https://amp.dev/documentation/guides-and-tutorials/learn/cors-in-email/
 * @param statsReceiver
 */
@Singleton
class AmpEmailCorsForEmailVersion2Filter[R <: Request] @Inject() (statsReceiver: StatsReceiver)
    extends SimpleFilter[R, Response]
    with Logging {

  private[this] val Scope = statsReceiver.scope(this.getClass.getSimpleName)
  private[this] val AmpEmailHeaderPresent = Scope.counter("amp_email_header_present")
  private[this] val AmpEmailHeaderNotPresent = Scope.counter("amp_email_header_not_present")
  private[this] val AmpEmailHeaderSenderAuthorized =
    Scope.counter("amp_email_header_sender_authorized")
  private[this] val AmpEmailHeaderSenderNotAuthorized =
    Scope.counter("amp_email_header_sender_not_authorized")

  /**
   * In order to support email changes without redeployment,
   * we are temporary allowing all twitter emails.
   * Attention! An email address should be pre-approved in Google
   * to be allowed to be used as a sender for AMP emails
   */
  private val TwitterPattern: Regex = "^(.+)@twitter.com$".r("Username")

  private[this] def ampEmailAuthorized(email: String): Boolean = {
    email match {
      case TwitterPattern(_) => true
      case _ => false
    }
  }

  private[this] def setResponseHeaders(response: Response, authorizedSender: String): Unit = {
    response.headerMap.setUnsafe("AMP-Email-Allow-Sender", authorizedSender)
  }

  private[this] def ampEmailQualifiedRequest(request: R): Option[AmpEmailSender] = {
    request.headerMap.get("AMP-Email-Sender") match {
      case Some(email) if ampEmailAuthorized(email) =>
        AmpEmailHeaderPresent.incr()
        Some(AuthorizedAmpEmailSender(email))
      case Some(email) =>
        AmpEmailHeaderPresent.incr()
        Some(NotAuthorizedAmpEmailSender(email))
      case _ =>
        AmpEmailHeaderNotPresent.incr()
        None
    }
  }

  def apply(request: R, service: Service[R, Response]): Future[Response] = {
    ampEmailQualifiedRequest(request) match {
      case Some(AuthorizedAmpEmailSender(sender)) =>
        AmpEmailHeaderSenderAuthorized.incr()
        for (response <- service(request)) yield {
          setResponseHeaders(response, authorizedSender = sender)
          response
        }
      case Some(NotAuthorizedAmpEmailSender(_)) =>
        AmpEmailHeaderSenderNotAuthorized.incr()
        Future.exception(ForbiddenException("Sender is not authorized"))
      case _ => service(request)
    }
  }

}
