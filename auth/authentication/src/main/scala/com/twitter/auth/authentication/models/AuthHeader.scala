package com.twitter.auth.authentication.models

import com.google.common.annotations.VisibleForTesting
import com.twitter.auth.authentication.models.AuthHeaderType.AuthHeaderType
import com.twitter.finagle.stats.DefaultStatsReceiver
import com.twitter.joauth.OAuthParams.StandardOAuthParamsHelperImpl
import com.twitter.joauth.keyvalue.KeyValueHandler
import com.twitter.joauth.keyvalue.KeyValueParser.HeaderKeyValueParser
import com.twitter.joauth.OAuthParams
import com.twitter.joauth.UrlCodec
import java.util
import java.util.Collections

object AuthHeader {
  private[this] lazy val statsReceiver = DefaultStatsReceiver
  private[this] lazy val authHeaderParserScope = statsReceiver.scope("auth_header_parser")
  private[this] lazy val noAuthHeaderCounter = authHeaderParserScope.counter("no_auth_header")
  private[this] lazy val invalidAuthHeaderCounter =
    authHeaderParserScope.counter("invalid_auth_header")
  private[this] lazy val oauth1AuthHeaderCounter =
    authHeaderParserScope.counter("oauth1_auth_header")
  private[this] lazy val oauth2AuthHeaderCounter =
    authHeaderParserScope.counter("oauth2_auth_header")
  private[this] lazy val unknownAuthHeaderCounter =
    authHeaderParserScope.counter("unknown_auth_header")

  def apply(authHeaderString: String): Option[AuthHeader] = {
    parseHeaderTypeAndValue(authHeaderString) match {
      case Some(parsedTypeAndValue) =>
        if (isOAuth1HeaderType(parsedTypeAndValue._1)) {
          oauth1AuthHeaderCounter.incr()
          // OAuth1 auth header value need NO decoding during signature verification process
          val authHeaderValue = parsedTypeAndValue._2
          Some(new AuthHeader(authHeaderValue, AuthHeaderType.OAuth1))
        } else if (isOAuth2HeaderType(parsedTypeAndValue._1)) {
          oauth2AuthHeaderCounter.incr()
          val authHeaderValue = UrlCodec.decode(parsedTypeAndValue._2)
          // OAuth2 bearer token must be decoded to be fetched
          Some(new AuthHeader(authHeaderValue, AuthHeaderType.OAuth2))
        } else {
          unknownAuthHeaderCounter.incr()
          None
        }
      case None =>
        // stats taken care of in the parser method
        None
    }
  }

  @VisibleForTesting
  private def parseHeaderTypeAndValue(authHeader: String): Option[(String, String)] = {
    // check for OAuth credentials in the header. OAuth 1.0a and 2.0 have
    // different header schemes, so match first on the auth scheme.
    if (authHeader == null || authHeader.length == 0) {
      noAuthHeaderCounter.incr()
      None
    } else {
      // TODO: make sure oauth1 and oauth2 check is correct.
      val spaceIndex: Int = authHeader.indexOf(' ')
      if (spaceIndex != -1 && spaceIndex != 0 && spaceIndex + 1 < authHeader.length) {
        val output: Option[(String, String)] = Some(
          (
            authHeader.substring(0, spaceIndex),
            authHeader.substring(spaceIndex + 1, authHeader.length)))
        output
      } else {
        invalidAuthHeaderCounter.incr()
        None
      }
    }
  }

  private def isOAuth1HeaderType(input: String): Boolean = {
    input.equalsIgnoreCase(OAuthParams.OAUTH1_HEADER_AUTHTYPE)
  }
  private def isOAuth2HeaderType(input: String): Boolean = {
    input.equalsIgnoreCase(OAuthParams.OAUTH2_HEADER_AUTHTYPE)
  }
}

case class AuthHeader(authHeaderValue: String, authHeaderType: AuthHeaderType) {

  /***
   * Generate OAuth1 params object
   * Method requires for the header to be OAuth1
   * @return OAuth1Params object
   */
  @VisibleForTesting
  def generateOAuth1Params(): OAuthParams.OAuth1Params = {
    require(AuthHeaderType.OAuth1.equals(authHeaderType))

    lazy val oAuthParamsBuilder = new OAuthParams.OAuthParamsBuilder(
      new StandardOAuthParamsHelperImpl()
    )
    // trim, normalize encodings
    lazy val handler: KeyValueHandler = oAuthParamsBuilder.headerHandler

    // if we were able match an appropriate auth header,
    // we'll wrap that handler with a MaybeQuotedValueKeyValueHandler,
    // which will strip quotes from quoted values before passing
    // to the underlying handler
    lazy val quotedHandler: KeyValueHandler =
      new KeyValueHandler.MaybeQuotedValueKeyValueHandler(handler)
    // now we'll pass the handler to the headerParser,
    // which splits on commas rather than ampersands,
    // and is more forgiving with whitespace
    lazy val handlers: util.List[KeyValueHandler] = Collections.singletonList(quotedHandler)
    HeaderKeyValueParser.parse(this.authHeaderValue, handlers)

    oAuthParamsBuilder.oAuth1Params()
  }

  /***
   * Generate OAuth2 params (right now, simply the header value)
   * Method requires that the header be OAuth2 header
   * @return
   */
  @VisibleForTesting
  def generateOAuth2Params(): String = {
    require(AuthHeaderType.OAuth2.equals(authHeaderType))
    this.authHeaderValue
  }
}
