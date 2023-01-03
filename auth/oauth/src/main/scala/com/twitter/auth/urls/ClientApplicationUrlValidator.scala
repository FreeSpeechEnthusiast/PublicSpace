package com.twitter.auth.urls

import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.logging.Logger
import com.twitter.twittertext.{Regex => TwitterRegex}
import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.validator.routines.InetAddressValidator
import org.apache.commons.validator.routines.RegexValidator
import org.apache.commons.validator.routines.UrlValidator

case class ParsedUrl(
  protocol: String,
  domain: String,
  port: Int,
  path: String)

object ClientApplicationUrlValidator {
  // Also monorail allows username:password stuff like https://foo:bar@trololol.net
  val UserPasswordRegex = "[^\\s:@]+(:[^\\s:@]*)?@"

  // these regexes are mostly copied from commons-validator's UrlValidator and used to
  // relax TLD validation until https://issues.apache.org/jira/browse/VALIDATOR-326 ships.
  val AuthorityCharsRegex = "\\p{Alnum}\\-\\."
  val TLDRegex = "\\p{Alpha}{2,}"
  val PortRegex = ":\\d{1,5}"
  val AuthorityRegex = "^(%s)?([%s]+\\.%s)(%s)?$".format(
    UserPasswordRegex,
    AuthorityCharsRegex,
    TLDRegex,
    PortRegex
  )

  val RelaxedValidator = new ThreadLocal[UrlValidator] {
    override def initialValue(): UrlValidator = {
      new UrlValidator(
        Array("http", "https"),
        new RegexValidator(AuthorityRegex),
        0 // options
      )
    }
  }
}

abstract class ClientApplicationUrlValidator {

  def isValid(url: String, clientApplicationId: Option[Long] = None): Boolean =
    parse(url, clientApplicationId).isDefined

  /**
   * @param clientApplicationId only used for scribing purposes
   */
  def parse(url: String, clientApplicationId: Option[Long] = None): Option[ParsedUrl]
}

object DefaultClientApplicationUrlValidator {
  private val logger = Logger.get
}

class DefaultClientApplicationUrlValidator(
  urlValidator: ThreadLocal[UrlValidator],
  secondaryValidator: ThreadLocal[ClientApplicationUrlValidator],
  callbackUrlValidator: ThreadLocal[ClientApplicationUrlValidator],
  useTwitterTextValidation: => Boolean,
  statsReceiver: StatsReceiver)
    extends ClientApplicationUrlValidator {
  import DefaultClientApplicationUrlValidator._

  private[this] val matchCounter = statsReceiver.counter("validation_match")
  private[this] val invalidCounter = statsReceiver.counter("invalid")
  private[this] val badProtocolCounter = statsReceiver.counter("bad_protocol")

  private[this] val mismatchScope = statsReceiver.scope("validation_mismatch")
  private[this] val monorailValid = mismatchScope.counter("monorail_valid")

  private[this] val parsedScope = statsReceiver.scope("parsed")
  private[this] val validatorCounter = parsedScope.counter("validator")
  private[this] val parsedByUrlParser = parsedScope.counter("url_parser")
  private[this] val parseSkipped = parsedScope.counter("invalid_url")

  override def isValid(url: String, clientApplicationId: Option[Long] = None): Boolean = {
    val strictMatch = secondaryValidator.get.isValid(url, clientApplicationId)
    if (useTwitterTextValidation) {
      matchCounter.incr()
      strictMatch
    } else {
      val result = urlValidator.get.isValid(url)
      if (result != strictMatch) {
        // try to see if its because of the protocol
        callbackUrlValidator.get.parse(url) match {
          case Some(parsed) if !strictMatch =>
            badProtocolCounter.incr()
          case _ =>
            logger.debug(
              "Url validation mismatch. Url: %s. Twitter-text: %s. Monorail: %s",
              url,
              strictMatch,
              result
            )
        }

        if (result) {
          monorailValid.incr()
        } else {
          invalidCounter.incr()
        }
      }
      result
    }
  }

  override def parse(url: String, clientApplicationId: Option[Long] = None): Option[ParsedUrl] = {
    if (useTwitterTextValidation) {
      validatorCounter.incr()
      secondaryValidator.get.parse(url)
    } else if (isValid(url, clientApplicationId)) {
      parsedByUrlParser.incr()
      try {
        val u = new URL(url)
        val port = u.getPort match {
          case -1 => u.getDefaultPort // -1 means port not specified
          case p => p
        }
        Some(
          ParsedUrl(
            u.getProtocol,
            u.getHost,
            port,
            u.getPath
          )
        )
      } catch {
        case m: MalformedURLException =>
          None
      }
    } else {
      parseSkipped.incr()
      None
    }
  }
}

object TwitterTextClientApplicationUrlValidator extends ClientApplicationUrlValidator {
  // This class is thread safe, as it uses java regex Pattern to parse url, and Pattern is thread safe
  def threadLocalValidator = new ThreadLocal[ClientApplicationUrlValidator] {
    override def initialValue(): ClientApplicationUrlValidator =
      TwitterTextClientApplicationUrlValidator
  }

  private val DefaultHttpPort = 80
  private val DefaultHttpsPort = 443

  override def isValid(url: String, clientApplicationId: Option[Long] = None): Boolean = {
    val matcher = TwitterRegex.VALID_URL.matcher(url)
    // If the matcher matches, check for the existence of the protocol if we need to
    matcher.matches && hasProtocol(matcher)
  }

  override def parse(url: String, clientApplicationId: Option[Long] = None): Option[ParsedUrl] = {
    val matcher = TwitterRegex.VALID_URL.matcher(url)

    if (matcher.matches && hasProtocol(matcher)) {
      val protocol = matcher.group(TwitterRegex.VALID_URL_GROUP_PROTOCOL)
      val domain = matcher.group(TwitterRegex.VALID_URL_GROUP_DOMAIN)
      val port = Option(matcher.group(TwitterRegex.VALID_URL_GROUP_PORT)) match {
        case Some(port) => port.toInt
        case None =>
          protocol.toLowerCase() match {
            case "http://" => DefaultHttpPort
            case "https://" => DefaultHttpsPort
            case _ =>
              // TwitterRegex checks for http or https
              throw new IllegalArgumentException("Unexpected protocol " + protocol)
          }
      }
      val path = Option(matcher.group(TwitterRegex.VALID_URL_GROUP_PATH)).getOrElse("")
      Some(ParsedUrl(protocol, domain, port, path))
    } else {
      None
    }
  }

  /**
   * @param matcher a Matcher object returned by twitter-text's VALID_URL regex that is a match
   * @return true if the protocol (scheme) portion of the url exists and is valid
   */
  private[this] def hasProtocol(matcher: Matcher): Boolean = {
    try {
      val protocol = matcher.group(TwitterRegex.VALID_URL_GROUP_PROTOCOL)
      protocol != null && !protocol.isEmpty
    } catch {
      case _: IllegalStateException | _: IllegalArgumentException => false
    }
  }
}

/**
 * Modified version of Apache's UrlValidator.
 *  - allows localhost (and other domains without a tld): https://localhost is fine, as in http://something
 *  - allows ip addresses
 *  - allows user:password (https://user:password@twitter.com)
 */
object ClientApplicationSettingCallbackUrlValidator {
  val webProtocols = Set("http", "https")
}

class ClientApplicationSettingCallbackUrlValidator(
  statsReceiver: StatsReceiver)
    extends CustomProtocolClientApplicationUrlValidator(statsReceiver = statsReceiver) {

  override def parse(url: String, clientApplicationId: Option[Long] = None): Option[ParsedUrl] = {
    super.parse(url) flatMap { parsedUrl =>
      parsedUrl match {
        case ParsedUrl(protocol, _, _, _) =>
          Some(parsedUrl)
        case _ =>
          None
      }
    }
  }
}

/**
 * Modified version of Apache's UrlValidator. Allows mostly all schemes, but prevents certain unsafe schemes from being used.
 *  - allows localhost (and other domains without a tld): https://localhost is fine, as in http://something
 *  - allows ip addresses
 *  - allows custom tlds
 *  - allows user:password (https://user:password@twitter.com)
 */
class CustomProtocolClientApplicationUrlValidator(
  oauthUrlValidator: OAuthUrlValidator = new OAuthUrlValidator(UrlValidator.ALLOW_ALL_SCHEMES),
  statsReceiver: StatsReceiver)
    extends ClientApplicationUrlValidator {

  private val DisallowedProtocols = Set(
    "vbscript",
    "javascript",
    "vbs",
    "data",
    "mocha",
    "keyword",
    "livescript",
    "ftp",
    "file",
    "gopher",
    "acrobat",
    "callto",
    "daap",
    "itpc",
    "itms",
    "firefoxurl",
    "hcp",
    "ldap",
    "mailto",
    "mmst",
    "mmsu",
    "msbd",
    "rtsp",
    "mso-offdap",
    "snews",
    "news",
    "nntp",
    "outlook",
    "stssync",
    "rlogin",
    "telnet",
    "tn3270",
    "shell",
    "sip"
  )

  private[this] lazy val logger = Logger.get(getClass)
  private[this] lazy val badProtocolUrlsCounter = statsReceiver.counter("bad_protocol_urls")

  /**
   * do not allow script protocols, to prevent XSS attacks
   */
  private def isBadProtocol(protocol: String) = {
    DisallowedProtocols.contains(protocol.toLowerCase)
  }

  override def parse(url: String, clientApplicationId: Option[Long] = None): Option[ParsedUrl] = {
    oauthUrlValidator.parse(url) match {
      case Some(parsedUrl) =>
        if (isBadProtocol(parsedUrl.protocol)) {
          badProtocolUrlsCounter.incr()
          // parseable but the protocol is bad. log this
          logger.warning(
            "disallowed protocol: %s\t%s\t%s",
            clientApplicationId.getOrElse(0),
            parsedUrl.protocol,
            StringEscapeUtils.escapeJava(url)
          )
          None
        } else {
          Some(parsedUrl)
        }
      case _ =>
        None
    }
  }
}

class OAuthUrlValidator(options: Long) extends UrlValidator(options) {

  /**
   * This expression derived/taken from the BNF for URI (RFC2396).
   * This is also copied straight from Apache's UrlValidator
   */
  private[this] val UrlRegex: String = "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?"
  private[this] val UrlPattern: Pattern = Pattern.compile(UrlRegex)

  /**
   * Schema/Protocol (ie. http:, ftp:, file:, etc).
   */
  private[this] val ParseUrlScheme: Int = 2

  /**
   * Includes hostname/ip and port number.
   */
  private[this] val ParseUrlAuthority: Int = 4
  private[this] val ParseUrlPath: Int = 5
  private[this] val ParseUrlQuery: Int = 7
  private[this] val ParseUrlFragment: Int = 9

  override def isValid(url: String) = parse(url).isDefined

  def parse(url: String): Option[ParsedUrl] = {
    val matcher = UrlPattern.matcher(url)
    if (matcher.matches()) {
      val rawScheme = matcher.group(ParseUrlScheme)
      val rawAuthority = matcher.group(ParseUrlAuthority)
      val rawPath = matcher.group(ParseUrlPath)
      val rawQuery = matcher.group(ParseUrlQuery)
      val rawFragment = matcher.group(ParseUrlFragment)

      if (rawScheme == null || rawAuthority == null || rawPath == null) {
        // cannot be null
        return None
      }

      // validate scheme
      // checks to see the characters are correct, etc
      if (!isValidScheme(rawScheme)) {
        return None
      }

      val scheme = rawScheme.toLowerCase
      // parses out the host and port from the authority
      getHostAndPort(rawAuthority, scheme) match {
        case None => None
        case Some((host, port)) =>
          if (isValidPath(rawPath) && isValidQuery(rawQuery) && isValidFragment(rawFragment)) {
            Some(ParsedUrl(scheme, host, port, rawPath))
          } else {
            None
          }
      }
    } else {
      None
    }
  }

  private[this] val UserPasswordRegex = "[^\\s:@]+(:[^\\s:@]*)?@"
  private[this] val AuthorityCharsRegex = "\\p{Alnum}\\-\\."
  // user portion @ host portion : port
  private[this] val AuthorityRegex =
    "^(%s)?([%s]*)(:(\\d{1,5}))?$".format(UserPasswordRegex, AuthorityCharsRegex)
  private[this] val AuthorityPattern = Pattern.compile(AuthorityRegex)

  private[this] val ParseAuthorityHost = 3
  private[this] val ParseAuthorityPort = 5

  private[this] val DomainSegmentRegex = "\\p{Alnum}(?>[\\p{Alnum}-]*\\p{Alnum})*"
  private[this] val TLDRegex = "\\p{Alpha}{2,}"
  private[this] val HostRegex = "^(?:%s\\.)+(%s)$".format(DomainSegmentRegex, TLDRegex)

  // hosts can be either blah.blah.tld or just blah, like http://localhost or twitter://oauth
  private[this] val HostValidator = new RegexValidator(Array(DomainSegmentRegex, HostRegex))

  def getHostAndPort(authority: String, scheme: String): Option[(String, Int)] = {
    if (authority == null) {
      return None
    }

    if (authority == "") {
      if (scheme == "http" || scheme == "https") {
        // unacceptable (https://)
        return None
      } else {
        return Some((authority, -1))
      }
    }

    val matcher = AuthorityPattern.matcher(authority)
    if (matcher.matches()) {
      // NOTE: www.twitter.com and twitter.com parses out differently
      val host = matcher.group(ParseAuthorityHost)
      val portOpt = Option(matcher.group(ParseAuthorityPort))

      val port = portOpt match {
        case Some(p) => p.toInt
        case None =>
          if (scheme == "http") {
            80
          } else if (scheme == "https") {
            443
          } else {
            -1
          }
      }

      // validate host
      if (!InetAddressValidator.getInstance.isValid(host)) {
        // not an IP? make sure its a valid host
        if (HostValidator.isValid(host)) {
          Some((host, port))
        } else {
          None
        }
      } else {
        Some((host, port))
      }
    } else {
      None
    }
  }
}
