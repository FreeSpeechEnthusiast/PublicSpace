package com.twitter.auth.urls

import com.twitter.appsec.sanitization.URLSafety.sanitizeUrl
import com.twitter.auth.models.ClientApplication
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.passbird.bitfield.clientprivileges.thriftscala.{Constants => ClientAppPrivileges}
import scala.collection.Set
import java.util.BitSet

object OAuth2RedirectUriValidator {
  val MaxUrlLength = 255
}

/**
 * OAuth2 Redirect URI validator, which may NOT fit other use cases (e.g. client app creation, OAuth1).
 * Validates redirect URIs for creating OAuth2 Authorization Code, and only exact match is allowed.
 *
 * Validate OOB
 * Validate URI length
 * Validate URI format
 * Validate XSS
 * Lock down exemption
 * Validate Exact Match
 *
 * Special URIs such as "oob" and "callback url exempt" are not allowed.
 */
class OAuth2RedirectUriValidator(
  urlValidator: ThreadLocal[ClientApplicationUrlValidator],
  clientAppIdCounter: Boolean,
  statsReceiver: StatsReceiver) {

  import OAuth2RedirectUriValidator._

  // TODO: Deprecate legacy default value "oob" once OAuth2 client app has dedicated
  //  redirect_url field
  private[this] val OutOfBand = "oob"

  // These are strings so that we can scope metrics to client application IDs
  private[this] val total = "total"
  private[this] val outOfBand = "out_of_band"
  private[this] val uriTooLong = "uri_too_long"
  private[this] val invalidFormat = "invalid_format"
  private[this] val xss = "xss"
  private[this] val backslashUri = "backslash_uri"
  private[this] val malformedUri = "malformed_uri"
  private[this] val lockRedirectExempt = "exempt"
  private[this] val specialExemptPass = "special_exempt_pass"
  private[this] val appWithoutRedirectUri = "app_without_redirect_uri"
  private[this] val appNotFound = "app_not_found"
  private[this] val invalidLockedRedirectUri = "invalid_locked_redirect_uri"
  private[this] val originUriWithProtocolOnly = "origin_uri_with_protocol_only"
  private[this] val bothMalformed = "both_malformed"
  private[this] val mismatch = "mismatch"

  def validateRedirectUri(
    redirectUri: String,
    clientAppOpt: Option[ClientApplication],
  ): Option[UrlErrors.Value] = {
    incrCounterWithAppId(total, clientAppOpt)
    redirectUri match {
      case OutOfBand =>
        incrCounterWithAppId(outOfBand, clientAppOpt)
        // OAuth2 does not allow OOB redirect URL
        Some(UrlErrors.OOB_NOT_ALLOWED)
      case url =>
        clientAppOpt foreach { clientApp =>
          if (hasPrivilege(clientApp, ClientAppPrivileges.LOCK_CALLBACK_URL_EXEMPT)) {
            // Count the requests with an exemption from the locked redirect uris
            incrCounterWithAppId(lockRedirectExempt, clientAppOpt)
          }
        }
        // TODO: Investigate if OAuth2 supports multiple redirect URIs
        val allRedirectUris = clientAppOpt
          .flatMap(_.additionalCallbackUrls)
          .getOrElse(Set.empty[String]) ++
          clientAppOpt.flatMap(_.callbackUrl)

        allRedirectUris match {
          // the app's redirect uri is empty: all requests must be oob
          case uris if clientAppOpt.isDefined && (uris.isEmpty || uris == Set("")) =>
            incrCounterWithAppId(appWithoutRedirectUri, clientAppOpt)
            Some(UrlErrors.APP_WITHOUT_REDIRECT_URI)
          // validate length
          case uris if uriLengthTooLong(redirectUri) =>
            incrCounterWithAppId(uriTooLong, clientAppOpt)
            Some(UrlErrors.URI_TOO_LONG)
          // validate xss
          case uris if uriContainsXss(redirectUri) =>
            incrCounterWithAppId(xss, clientAppOpt)
            Some(UrlErrors.XSS)
          // This is to prevent potentially malicious activity (see ACCTSEC-4147)
          // We will now ban all back-slashes from the redirect url. See (ACCTSEC-5067)
          // url.contains("\\")
          case uris if uriHasEscapedAuthority(redirectUri) =>
            incrCounterWithAppId(backslashUri, clientAppOpt)
            Some(UrlErrors.INVALID_REDIRECT_URI)
          // validate format
          case uris if uriFormatInvalid(redirectUri) =>
            incrCounterWithAppId(invalidFormat, clientAppOpt)
            Some(UrlErrors.INVALID_URI_FORMAT)
          // set of redirect urls has a match with the client's redirect url
          // OAuth2 Redirect URI allows exact match only
          case uris
              if clientAppOpt.isDefined && uris.exists(
                isRedirectUriAMatch(_, redirectUri, clientAppOpt)) =>
            None
          // redirect uri not in set of the client app's redirect uris
          // the following cases should be all failed in OAuth2, we keep them for tracking purposes
          case _ =>
            clientAppOpt match {
              case Some(clientApp) =>
                // If URL lockdown is in effect and client does not have an exemption, return lockdown error
                if (shouldEnforceLockedRedirectUri(clientApp)) {
                  incrCounterWithAppId(
                    invalidLockedRedirectUri,
                    clientAppOpt
                  )
                  Some(UrlErrors.REDIRECT_URI_LOCKED)
                } else if (!urlValidator.get.isValid(url, Some(clientApp.id))) {
                  // If URL is invalid, return error
                  incrCounterWithAppId(malformedUri, clientAppOpt)
                  Some(UrlErrors.INVALID_REDIRECT_URI)
                } else {
                  // client app has special exemption and url is valid
                  incrCounterWithAppId(specialExemptPass, clientAppOpt)
                  // Special Except Pass should be dedicated to TOO apps, which does not
                  // support OAuth2 yet.
                  Some(UrlErrors.LOCK_DOWN_EXEMPT)
                }
              // no client app provided but URL is valid
              case None if urlValidator.get.isValid(url, None) =>
                incrCounterWithAppId(appNotFound, clientAppOpt)
                Some(UrlErrors.APP_NOT_FOUND)
              // fall through - no client app provided, URL is invalid, and we are validating URLs
              case _ =>
                Some(UrlErrors.INVALID_REDIRECT_URI)
            }
        }
    }
  }

  def isValid(redirectUri: String): Boolean = {
    urlValidator.get.isValid(redirectUri)
  }

  def parse(url: String): Option[ParsedUrl] = {
    urlValidator.get.parse(url)
  }

  private[this] def isRedirectUriAMatch(
    originalRedirectUri: String,
    redirectUri: String,
    clientAppOpt: Option[ClientApplication]
  ): Boolean = {
    val validator = urlValidator.get
    (validator.parse(originalRedirectUri), validator.parse(redirectUri)) match {
      // TODO: both are malformed, see if the strings match
      case (None, None) =>
        incrCounterWithAppId(bothMalformed, clientAppOpt)
        false
      // exact match
      case (Some(parsedUrl), Some(parsedUrl2)) =>
        parsedUrl == parsedUrl2
      // mismatch
      case _ =>
        incrCounterWithAppId(mismatch, clientAppOpt)
        false
    }
  }

  private[this] def shouldEnforceLockedRedirectUri(
    clientApplication: ClientApplication,
  ): Boolean = {
    !hasPrivilege(clientApplication, ClientAppPrivileges.LOCK_CALLBACK_URL_EXEMPT)
  }

  private[this] def hasPrivilege(client: ClientApplication, privilege: Int): Boolean = {
    client.appPrivileges match {
      case Some(privileges) => BitSet.valueOf(privileges).get(privilege)
      case None => false
    }
  }

  private[this] def uriLengthTooLong(
    uri: String
  ): Boolean = {
    uri != null && uri.length > MaxUrlLength
  }

  private[this] def uriFormatInvalid(
    uri: String
  ): Boolean = {
    !urlValidator.get.isValid(uri)
  }

  private def uriContainsXss(redirectUri: String): Boolean = {
    !redirectUri.equals(sanitizeUrl(redirectUri))
  }

  private[this] def uriHasEscapedAuthority(url: String): Boolean = {
    // We will now ban all back-slashes from the redirect uri. See (ACCTSEC-5067)
    url.contains("\\")
  }

  private[this] def incrCounterWithAppId(
    counterName: String,
    clientAppOpt: Option[ClientApplication]
  ): Unit = {
    statsReceiver.counter(counterName).incr()
    if (clientAppIdCounter) {
      statsReceiver
        .scope(clientAppOpt.map(_.id.toString).getOrElse("unknown"))
        .counter(counterName)
        .incr()
    }
  }
}
