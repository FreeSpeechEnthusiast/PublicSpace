package com.twitter.auth.authentication.unpacker

import com.google.common.primitives.UnsignedLongs
import com.twitter.auth.authentication.models.ActAsUserParams
import com.twitter.auth.authentication.models.AmpEmailRequestParams
import com.twitter.auth.authentication.models.AuthRequest
import com.twitter.auth.authentication.models.BadRequestParams
import com.twitter.auth.authentication.models.GuestAuthRequestParams
import com.twitter.auth.authentication.models.OAuth1RequestParams
import com.twitter.auth.authentication.models.OAuth1ThreeLeggedRequestParams
import com.twitter.auth.authentication.models.OAuth1TwoLeggedRequestParams
import com.twitter.auth.authentication.models.OAuth2ClientCredentialRequestParams
import com.twitter.auth.authentication.models.OAuth2RequestParams
import com.twitter.auth.authentication.models.OAuth2SessionRequestParams
import com.twitter.auth.authentication.models.RequestParams
import com.twitter.auth.authentication.models.SessionRequestParams
import com.twitter.auth.authentication.models.TiaAuthRequestParams
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode
import com.twitter.finagle.stats.Stat
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.tfe.HttpHeaderNames.X_TFE_TRANSACTION_ID
import com.twitter.joauth.OAuthParams.OAuth1Params
import com.twitter.joauth.OAuthParams.OAuthParamsBuilder
import com.twitter.joauth.OAuthParams.OAuthParamsHelper
import com.twitter.joauth.Request.Pair
import com.twitter.joauth.Request.ParsedRequest
import com.twitter.joauth.Unpacker.CustomizableUnpacker
import com.twitter.joauth.Unpacker.KeyValueCallback
import com.twitter.joauth.Unpacker.OAuth2Checker
import com.twitter.joauth.keyvalue.KeyValueHandler.KeyTransformingKeyValueHandler
import com.twitter.joauth.keyvalue.KeyValueHandler.TransformingKeyValueHandler
import com.twitter.joauth.keyvalue.KeyValueHandler.TrimmingKeyValueHandler
import com.twitter.joauth.keyvalue.KeyValueHandler.UrlEncodingNormalizingKeyValueHandler
import com.twitter.joauth.keyvalue.KeyValueHandler
import com.twitter.joauth.keyvalue.KeyValueParser
import com.twitter.joauth.keyvalue.Transformer
import com.twitter.joauth.Normalizer
import com.twitter.joauth.Request
import com.twitter.joauth.UrlCodec
import com.twitter.logging.Logger
import com.twitter.auth.authentication.utils.ActAsUserUtils.createActAsUserParams
import com.twitter.auth.authentication.utils.ActAsUserUtils.isActAsUserRequest
import com.twitter.auth.authentication.unpacker.UnpackerLibrary._
import com.twitter.auth.authentication.utils.AuthenticationConfig
import com.twitter.auth.authentication.utils.AuthenticationUtils
import com.twitter.auth.utils.OAuth2CredentialType
import com.twitter.auth.utils.OAuth2CredentialUtils
import com.twitter.decider.Feature
import com.twitter.joauth.keyvalue.KeyValueParser.HeaderKeyValueParser
import com.twitter.joauth.keyvalue.KeyValueParser.QueryKeyValueParser
import com.twitter.util.Return
import com.twitter.util.Throw
import java.util
import java.util.ArrayList
import java.util.StringTokenizer
import scala.collection.JavaConverters._

/**
 * IMPORTANT: This unpacker does NOT support SpecialClients, and should not be applied to Passbird.
 */
object TwitterRequestUnpacker {
  private val allowFloatingPointTimestamps = true
  private val helper = UnpackerHelper(allowFloatingPointTimestamps)
  // TODO: Passbird relies on SpecialClients, FlightAuth should no longer relies on SpecialClients
  // Option 1: hard code client app ids
  // Option 2: rely on Feature Permissions
  private val shouldBeLenientForOAuth1: (AuthRequest, OAuth1Params) => Boolean = (_, _) => false

  def apply(
    statsReceiver: StatsReceiver,
    useNewRequestBodyParser: Feature
  ) = new StandardTwitterUnpacker(
    helper = helper,
    normalizer = Normalizer.getStandardNormalizer,
    queryParser = QueryKeyValueParser,
    headerParser = HeaderKeyValueParser,
    shouldBeLenientForOAuth1 = shouldBeLenientForOAuth1,
    shouldAllowOAuth2IfNetworkLocal = true,
    shouldAllowOAuth2SessionWithoutAuthTypeHeader = true,
    shouldAllowWebAuthMultiUserIdHeader = true,
    useNewRequestBodyParser = useNewRequestBodyParser,
    statsReceiver = statsReceiver
  )
}

class PassbirdOAuth2Checker(val shouldAllowIfNetworkLocal: Boolean) extends OAuth2Checker {
  def shouldAllowOAuth2(
    request: Request,
    parsed: ParsedRequest
  ): Boolean = {
    parsed.scheme == CustomizableUnpacker.HTTPS || shouldAllowIfNetworkLocal
  }
}

/**
 * Twitter-specific implementation of joauth's Unpacker
 */
class TwitterRequestUnpacker(
  helper: OAuthParamsHelper,
  normalizer: Normalizer,
  queryParser: KeyValueParser,
  headerParser: KeyValueParser,
  queryParamTransformer: KeyValueCallback,
  bodyParamTransformer: KeyValueCallback,
  headerTransformer: KeyValueCallback,
  shouldBeLenient: (AuthRequest, OAuth1Params) => Boolean,
  shouldAllowOAuth2WithoutSslInNetworkLocal: Boolean,
  shouldAllowOAuth2SessionWithoutAuthTypeHeader: Boolean,
  shouldAllowWebAuthMultiUserIdHeader: Boolean,
  useNewRequestBodyParser: Feature,
  statsReceiver: StatsReceiver)
    extends CustomizableUnpacker(
      helper,
      normalizer,
      queryParser,
      headerParser,
      queryParamTransformer,
      bodyParamTransformer,
      headerTransformer,
      new PassbirdOAuth2Checker(shouldAllowOAuth2WithoutSslInNetworkLocal)) {

  /**
   * For monorail compatibility
   */
  protected[this] def monorailParamKeyValueHandler(
    kvHandler: KeyValueHandler
  ): KeyTransformingKeyValueHandler = {
    new KeyTransformingKeyValueHandler(
      new TrimmingKeyValueHandler(new UrlDecodeThenEncodeKeyValueHandler(kvHandler)),
      new Transformer() {
        def transform(input: String): String = helper.processKey(input)
      }
    )
  }

  private[this] val logger = Logger.get(getClass)
  private[this] val AuthorizationHeaderKey = "authorization"

  private[this] val ssr = statsReceiver.scope("passbird_request_unpacker")
  private[this] val clientTokenCookieCounter = ssr.counter("client_token_cookie")
  private[this] val unknownRequestCounter = ssr.counter("unknown_request")

  private[this] val unpackAmpEmailScope = ssr.scope("amp_email")
  private[this] val unpackAmpEmailSuccess = unpackAmpEmailScope.counter("success")

  private[this] val unpackSessionScope = ssr.scope("session")
  private[this] val unpackSessionSuccess = unpackSessionScope.counter("success")
  private[this] val unpackSessionClientTokenCookieCounter =
    unpackSessionScope.counter("client_token_cookie")

  private[this] val unpackSessionNoAuthTokenFailure = unpackSessionScope.counter("failure")

  private[this] val unpackOAuth2SessionScope = ssr.scope("oauth2_session")
  private[this] val unpackOAuth2SessionSuccess = unpackOAuth2SessionScope.counter("success")
  private[this] val unpackOAuth2SessionFailure = unpackOAuth2SessionScope.counter("failure")

  private[this] val unpackOAuth2Scope = ssr.scope("oauth2")
  private[this] val unpackOAuth2Success = unpackOAuth2Scope.counter("success")
  private[this] val unpackOAuth2Failure = unpackOAuth2Scope.counter("failure")

  private[this] val unpackOAuth2ClientCredentialScope = ssr.scope("oauth2_client_credential")
  private[this] val unpackOAuth2ClientCredentialSuccess =
    unpackOAuth2ClientCredentialScope.counter("success")

  private[this] val unpackOAuth2OrSessionScopeOauth2 =
    ssr.scope("oauth2_or_session_probably_oauth2")
  private[this] val unpackOAuth2OrSessionScopeSession =
    ssr.scope("oauth2_or_session_probably_session")

  private[this] val unpackOAuth2OrSessionFailureOauth2 =
    unpackOAuth2OrSessionScopeOauth2.counter("failure")
  private[this] val unpackOAuth2OrSessionFailureSession =
    unpackOAuth2OrSessionScopeSession.counter("failure")

  private[this] val unpackOAuth1ThreeLeggedScope = ssr.scope("oauth1_three_legged")
  private[this] val unpackOAuth1ThreeLeggedSuccess = unpackOAuth1ThreeLeggedScope.counter("success")
  private[this] val unpackOAuth1ThreeLeggedFailure = unpackOAuth1ThreeLeggedScope.counter("failure")

  private[this] val unpackOAuth1TwoLeggedScope = ssr.scope("oauth1_two_legged")
  private[this] val unpackOAuth1TwoLeggedSuccess = unpackOAuth1TwoLeggedScope.counter("success")
  private[this] val unpackOAuth1TwoLeggedFailure = unpackOAuth1TwoLeggedScope.counter("failure")

  private[this] val unpackGuestScope = ssr.scope("guest")
  private[this] val unpackGuestSuccess = unpackGuestScope.counter("success")
  private[this] val unpackGuestFailure = unpackGuestScope.counter("failure")

  private[this] val unpackTiaScope = ssr.scope("tia")
  private[this] val unpackTiaSuccess = unpackTiaScope.counter("success")

  private[this] val actAsUserIdCookieUnexpectedCounter = ssr.counter("act_as_user_id_unexpected")
  private[this] val unpackSessionForSpecialEndpoints =
    ssr.counter("unpack_session_special_endpoints")

  private[this] val actAsUserIdHeaderCounter = ssr.counter("act_as_user_id_header")
  private[this] val legacyShouldBeLenientCounter = ssr.counter("legacy_should_be_lenient")

  private[this] val parseRequestBodyCounter = ssr.counter("parse_request_body")

  // these counters will allow us to gain stats on why Bad Request Params (BRP) are being created
  private[this] val brpAuthMultiCookieError = ssr.counter("brp_auth_multi_cookie_error")
  private[this] val brpAuthCookieParserThrow = ssr.counter("brp_auth_cookie_parser_throw")
  private[this] val brpUnpackOAuth2SessionFailureBadResult =
    ssr.counter("brp_unpack_oauth2_session_failure_bad_result")
  private[this] val brpUnpackOAuth2SessionFailureUnknown =
    ssr.counter("brp_unpack_oauth2_session_failure_unknown")
  private[this] val brpActAsUserIdCookieUnverified =
    ssr.counter("brp_act_as_user_id_cookie_unverified")
  private[this] val brpCalledUnpackUnknown = ssr.counter("brp_called_unpack_unknown")
  private[this] val brpOAuth2SessionAuthHeaderMismatch =
    ssr.counter("brp_oauth2_session_auth_header_mismatch")
  private[this] val brpOAuth2SessionAuthMultiCookieErrorThrow =
    ssr.counter("brp_oauth2_session_auth_multi_cookie_error_throw")
  private[this] val brpOAuth2SessionAuthMultiCookieErrorUnknown =
    ssr.counter("brp_oauth2_session_auth_multi_cookie_error_unknown")

  def fromRequest(request: AuthRequest): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("unpack_request")) {
      val actAsUserParamsOpt =
        if (isActAsUserRequest(request)) createActAsUserParams(request) else None
      val userId = UnpackerLibrary.getUserIdFromHeader(request)
      // Header X_TFE_USER_ASSERTION_SIGNATURE only exists for TIA requests, and instead
      // OAuth/Session auth response suppose to generate that header.
      val assertionSignature = UnpackerLibrary.getAssertionSignatureFromHeader(request)
      // Client Token from cookie
      val clientTokenCookie = AuthCookieParser.getClientTokenCookie(request.cookies)
      val authToken = AuthCookieParser.getAuthTokenCookie(request.cookies)
      if (clientTokenCookie.isDefined) {
        clientTokenCookieCounter.incr()
      }
      // TIA
      if (userId.isDefined && assertionSignature.isDefined) {
        val clientAppId = UnpackerLibrary.getClientAppIdFromHeader(request)
        val transactionId = UnpackerLibrary.getTransactionIdFromHeader(request) match {
          case Some(id) =>
            Seq(id)
          case _ =>
            Seq()
        }
        unpackTiaRequest(request, userId.get, assertionSignature.get, clientAppId, transactionId)
      }

      /**
       * fix for http://go/j/acctsec-4351, requests to oauth/authorize and oauth/authenticate
       * endpoints should be treated as AuthRequests instead of OAuthRequest
       */
      // Session
      else if (request.url.isDefined
        && AuthenticationConfig.OauthAuthRequests.contains(
          UnpackerLibrary.getPath(request.url.get))) {
        logger.debug("Unpacking session for endpoint %s", UnpackerLibrary.getPath(request.url.get))
        unpackSessionForSpecialEndpoints.incr()
        val oAuthParamsBuilder = parseSpecialRequest(request, List[KeyValueHandler]())

        authToken match {
          case None =>
            unpackSessionNoAuthTokenFailure.incr()
            unpackUnknownRequest(request)
          case Some(authToken) =>
            unpackSessionRequest(
              request,
              actAsUserParamsOpt,
              clientTokenCookie,
              oAuthParamsBuilder.otherParams(),
              authToken)
        }
      }
      // OAuth or Session
      else {
        val oAuthParamsBuilder = this.parseAuthRequest(request, new ArrayList[KeyValueHandler])
        if (oAuthParamsBuilder.isOAuth1 || oAuthParamsBuilder.isOAuth1TwoLegged ||
          UnpackerLibrary.isValidOAuth2Req(request, oAuthParamsBuilder)) {
          // Only need to validate the oauth2session auth type header when the request is OAuth request
          precheckOauth2SessionAuthTypeHeader(request, oAuthParamsBuilder) match {
            case Some(badRequestParams: BadRequestParams) =>
              Some(badRequestParams)
            case _ =>
              if (oAuthParamsBuilder.isOAuth1) {
                unpackOAuth1ThreeLeggedRequest(request, actAsUserParamsOpt, oAuthParamsBuilder)
              } else if (oAuthParamsBuilder.isOAuth1TwoLegged) {
                unpackOAuth1TwoLeggedRequest(request, actAsUserParamsOpt, oAuthParamsBuilder)
              } else {
                unpackOAuth2Request(
                  request,
                  oAuthParamsBuilder.v2Token,
                  actAsUserParamsOpt,
                  oAuthParamsBuilder.otherParams()
                )
              }
          }
        } else if (oAuthParamsBuilder.isOAuth2 && !UnpackerLibrary.isOAuth2Allowed(request)) {
          unpackOAuth2OrSessionFailureOauth2.incr()
          unpackUnknownRequest(request)
        } else { // Session auth, amp email token auth or unknown
          (authToken, request.ampEmailToken) match {
            // prioritize regular authentication over amp email auth
            case (Some(authToken), _) =>
              unpackSessionRequest(
                request,
                actAsUserParamsOpt,
                clientTokenCookie,
                oAuthParamsBuilder
                  .otherParams(),
                authToken)
            case (None, Some(ampEmailToken)) =>
              unpackAmpEmailRequest(request = request, ampEmailToken = ampEmailToken)
            case (_, _) =>
              unpackOAuth2OrSessionFailureSession.incr()
              unpackUnknownRequest(request)
          }
        }
      }
    }
  }

  private[this] def unpackOAuth2Request(
    request: AuthRequest,
    bearerToken: String,
    actAsUserParamsOpt: Option[ActAsUserParams],
    otherParams: java.util.List[Pair]
  ): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("unpack_oauth2_request")) {
      // OAuth2 might be URL-encoded (e.g. xxx%3dxxx -> xxx=xxx)
      val decodedBearerToken = UrlCodec.decode(bearerToken)
      // try fetching cookies "auth_token" or "auth_multi"
      // get the multi-cookie value (if present) and override regular cookie token if needed
      // OAuth2 app-only doesn't have any token from cookie
      AuthCookieParser.apply(
        request.headerMap,
        request.cookies,
        shouldAllowWebAuthMultiUserIdHeader,
        statsReceiver
      ) match {
        // Unpack OAuth2 + Session
        case Return(Some(authToken: String)) =>
          unpackOAuth2SessionRequest(
            request,
            decodedBearerToken,
            Some(authToken),
            actAsUserParamsOpt,
            shouldAllowOAuth2SessionWithoutAuthTypeHeader,
            otherParams,
            statsReceiver
          )
        // Unpack OAuth2/OAuth2GuestAuth
        case Return(None) =>
          val guestToken = UnpackerLibrary.getGuestTokenFromHeader(request)
          if (guestToken.isDefined) {
            unpackGuestRequest(request, decodedBearerToken, guestToken.get, otherParams)
          } else {
            unpackOAuth2Request(request, decodedBearerToken, otherParams)
          }
        case Throw(e: AuthMultiCookieError) =>
          Some(createBadRequestParams(request.headerMap, e.code, brpAuthMultiCookieError))
        case Throw(_) =>
          Some(
            createBadRequestParams(
              request.headerMap,
              AuthResultCode.AuthCookieParserUnexpected,
              brpAuthCookieParserThrow))
      }
    }
  }

  private[this] def unpackOAuth2SessionRequest(
    request: AuthRequest,
    decodedBearerToken: String,
    authTokenFromCookie: Option[String],
    actAsUserParamsOpt: Option[ActAsUserParams],
    shouldAllowOAuth2SessionWithoutAuthTypeHeader: Boolean,
    otherParams: java.util.List[Pair],
    statsReceiver: StatsReceiver
  ): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("unpack_oauth2session_request")) {
      request.url match {
        case Some(uri) =>
          isXTwitterAuthTypeOAuth2Session(
            request.headerMap,
            decodedBearerToken,
            authTokenFromCookie,
            shouldAllowOAuth2SessionWithoutAuthTypeHeader,
            statsReceiver.scope("oAuth2Session")
          ) match {
            case AuthResultCode.Ok =>
              authTokenFromCookie match {
                case Some(authToken: String) =>
                  unpackOAuth2SessionSuccess.incr()
                  val guestToken = UnpackerLibrary.getGuestTokenFromHeader(request)
                  Some(
                    OAuth2SessionRequestParams(
                      createPassportId(request.headerMap),
                      actAsUserParamsOpt,
                      decodedBearerToken,
                      authToken,
                      AuthenticationUtils.paramsToMap(otherParams),
                      UnpackerLibrary.getPath(uri),
                      UnpackerLibrary.getIPFromHeader(request),
                      UnpackerLibrary.getClientHeader(request),
                      UnpackerLibrary.decodeGuestToken(guestToken)
                    ))
                case _ =>
                  unpackOAuth2SessionFailure.incr()
                  None
              }
            case badAuthResultCode: AuthResultCode =>
              unpackOAuth2SessionFailure.incr()
              Some(
                createBadRequestParams(
                  request.headerMap,
                  badAuthResultCode,
                  brpUnpackOAuth2SessionFailureBadResult))
            case _ =>
              unpackOAuth2SessionFailure.incr()
              Some(
                createBadRequestParams(
                  request.headerMap,
                  AuthResultCode.InternalServerError,
                  brpUnpackOAuth2SessionFailureUnknown))
          }
        case _ =>
          unpackOAuth2SessionFailure.incr()
          unpackUnknownRequest(request)
      }
    }
  }

  private[this] def unpackOAuth2Request(
    request: AuthRequest,
    decodedBearerToken: String,
    otherParams: java.util.List[Pair]
  ): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("unpack_oauth2_request")) {
      val tokenType = OAuth2CredentialUtils.decodeTokenType(decodedBearerToken)
      (request.url, tokenType) match {
        case (Some(uri), OAuth2CredentialType.CLIENT_ACCESS_TOKEN) =>
          unpackOAuth2ClientCredentialSuccess.incr()
          Some(
            OAuth2ClientCredentialRequestParams(
              passportId = createPassportId(request.headerMap),
              clientAccessToken = decodedBearerToken,
              otherParams = AuthenticationUtils.paramsToMap(otherParams),
              path = UnpackerLibrary.getPath(uri),
              ip = UnpackerLibrary.getIPFromHeader(request),
              twitterClient = UnpackerLibrary.getClientHeader(request)
            )
          )
        case (Some(uri), _) =>
          unpackOAuth2Success.incr()
          Some(
            OAuth2RequestParams(
              createPassportId(request.headerMap),
              decodedBearerToken,
              AuthenticationUtils.paramsToMap(otherParams),
              UnpackerLibrary.getPath(uri),
              UnpackerLibrary.getIPFromHeader(request),
              UnpackerLibrary.getClientHeader(request)
            )
          )
        case _ =>
          unpackOAuth2Failure.incr()
          unpackUnknownRequest(request)
      }
    }
  }

  private[this] def unpackSessionRequest(
    request: AuthRequest,
    actAsUserParamsOpt: Option[ActAsUserParams],
    clientTokenCookie: Option[String],
    otherParams: java.util.List[Pair],
    authToken: String
  ): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("unpack_session_request")) {
      if (clientTokenCookie.isDefined) {
        unpackSessionClientTokenCookieCounter.incr()
      }
      unpackSessionSuccess.incr()
      Some(
        SessionRequestParams(
          request.headerMap.getOrElse(X_TFE_TRANSACTION_ID, Unknown),
          actAsUserParamsOpt,
          authToken,
          clientTokenCookie,
          AuthenticationUtils.paramsToMap(otherParams)
        )
      )
    }
  }

  private[this] def unpackAmpEmailRequest(
    request: AuthRequest,
    ampEmailToken: String
  ): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("unpack_amp_email_request")) {
      unpackAmpEmailSuccess.incr()
      Some(
        AmpEmailRequestParams(
          passportId = request.headerMap.getOrElse(X_TFE_TRANSACTION_ID, Unknown),
          ampEmailToken = ampEmailToken
        )
      )
    }
  }

  private[this] def unpackOAuth1ThreeLeggedRequest(
    request: AuthRequest,
    actAsUserParamsOpt: Option[ActAsUserParams],
    oAuthParamsBuilder: OAuthParamsBuilder
  ): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("unpack_oauth1threelegged_request")) {
      if (!actAsUserIdVerified(request)) {
        actAsUserIdCookieUnexpectedCounter.incr()
        unpackOAuth1ThreeLeggedFailure.incr()
        Some(
          createBadRequestParams(
            request.headerMap,
            AuthResultCode.ContributorsIndicatorUnexpected,
            brpActAsUserIdCookieUnverified))
      } else {
        if (actAsUserParamsOpt.isDefined && actAsUserParamsOpt.get.actAsUserIdHeader.isDefined) {
          actAsUserIdHeaderCounter.incr()
        }
        request.url match {
          case Some(uri) =>
            val oAuth1RequestParams = Some(
              OAuth1ThreeLeggedRequestParams(
                createPassportId(request.headerMap),
                actAsUserParamsOpt,
                oAuthParamsBuilder.oAuth1Params(),
                Some(oAuthParamsBuilder.otherParams),
                UnpackerLibrary.getScheme(request),
                UnpackerLibrary.getHost(request),
                UnpackerLibrary.getPort(request),
                UnpackerLibrary.getVerb(request),
                UnpackerLibrary.getPath(uri),
                UnpackerLibrary.getIPFromHeader(request),
                UnpackerLibrary.getClientHeader(request)
              )
            )
            // verify if OAuth1-three-legged request is valid
            isValidOAuth1Req(oAuth1RequestParams.get) match {
              case true =>
                unpackOAuth1ThreeLeggedSuccess.incr()
                oAuth1RequestParams
              case _ =>
                unpackOAuth1ThreeLeggedFailure.incr()
                unpackUnknownRequest(request)
            }
          case _ =>
            unpackOAuth1ThreeLeggedFailure.incr()
            unpackUnknownRequest(request)
        }
      }
    }
  }

  private[this] def unpackOAuth1TwoLeggedRequest(
    request: AuthRequest,
    actAsUserParamsOpt: Option[ActAsUserParams],
    oAuthParamsBuilder: OAuthParamsBuilder
  ): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("unpack_oauth1twolegged_request")) {
      request.url match {
        case Some(uri) =>
          val oAuth1RequestParams = Some(
            OAuth1TwoLeggedRequestParams(
              createPassportId(request.headerMap),
              actAsUserParamsOpt,
              oAuthParamsBuilder.oAuth1Params(),
              Some(oAuthParamsBuilder.otherParams),
              UnpackerLibrary.getScheme(request),
              UnpackerLibrary.getHost(request),
              UnpackerLibrary.getPort(request),
              UnpackerLibrary.getVerb(request),
              UnpackerLibrary.getPath(uri),
              UnpackerLibrary.getIPFromHeader(request),
              UnpackerLibrary.getClientHeader(request)
            )
          )
          // verify if OAuth1-two-legged request is valid
          isValidOAuth1Req(oAuth1RequestParams.get) match {
            case true =>
              unpackOAuth1TwoLeggedSuccess.incr()
              oAuth1RequestParams
            case _ =>
              unpackOAuth1TwoLeggedFailure.incr()
              unpackUnknownRequest(request)
          }
        case _ =>
          unpackOAuth1TwoLeggedFailure.incr()
          unpackUnknownRequest(request)
      }
    }
  }

  private[this] def unpackGuestRequest(
    request: AuthRequest,
    decodedBearerToken: String,
    guestToken: String,
    otherParams: java.util.List[Pair]
  ): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("unpack_guest_request")) {
      request.url match {
        case Some(uri) =>
          Some(
            GuestAuthRequestParams(
              createPassportId(request.headerMap),
              decodedBearerToken,
              AuthenticationUtils.paramsToMap(otherParams),
              UnpackerLibrary.getPath(uri),
              UnpackerLibrary.getIPFromHeader(request),
              UnpackerLibrary.getClientHeader(request),
              try {
                val guestId = UnsignedLongs.decode(guestToken)
                unpackGuestSuccess.incr()
                Some(guestId)
              } catch {
                case e: NumberFormatException =>
                  unpackGuestFailure.incr()
                  None
              }
            )
          )
        case _ =>
          unpackGuestFailure.incr()
          unpackUnknownRequest(request)
      }
    }
  }

  private[this] def unpackTiaRequest(
    request: AuthRequest,
    userId: String,
    signature: String,
    clientAppId: Option[String],
    additionalFields: Seq[String]
  ): Option[TiaAuthRequestParams] = {
    Stat.time(statsReceiver.stat("unpack_tia_request")) {
      val userIdOption =
        try {
          Some(userId.toLong)
        } catch {
          case e: NumberFormatException =>
            None
        }
      unpackTiaSuccess.incr()
      Some(
        TiaAuthRequestParams(
          createPassportId(request.headerMap),
          userIdOption,
          signature,
          clientAppId,
          additionalFields
        )
      )
    }
  }

  private[this] def unpackUnknownRequest(
    request: AuthRequest
  ): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("unpack_unknown_request")) {
      unknownRequestCounter.incr()
      Some(
        createBadRequestParams(
          request.headerMap,
          AuthResultCode.UnknownAuthType,
          brpCalledUnpackUnknown))
    }
  }

  /**
   * Mostly copied from joauth, modified to work with Twitter for Android clients
   */
  private[this] def parseSpecialRequest(
    request: AuthRequest,
    kvHandlers: scala.collection.immutable.List[KeyValueHandler]
  ): OAuthParamsBuilder = {
    Stat.time(statsReceiver.stat("parse_special_request")) {
      // use an oAuthParamsBuilder instance to accumulate key/values from
      // the query string, the POST (if the appropriate Content-Type), and
      // the Authorization header, if any.
      val oAuthParamsBuilder = new OAuthParamsBuilder(helper)

      var bodyParamHandlers: Seq[KeyValueHandler] = Seq()
      var queryHandler: KeyValueHandler = null
      parseHeader(
        request.headerMap.getOrElse(AuthorizationHeaderKey, null),
        oAuthParamsBuilder.headerHandler)

      /** For monorail compatibility: This is activated for our whitelisted consumerKeys if this is a /1/ endpoint */
      if (shouldBeLenient(request, oAuthParamsBuilder.oAuth1Params)) {
        // A v1 request from one of the "acceptable" apps
        legacyShouldBeLenientCounter.incr()
        queryHandler = monorailParamKeyValueHandler(oAuthParamsBuilder.queryHandler)
        bodyParamHandlers =
          monorailParamKeyValueHandler(oAuthParamsBuilder.queryHandler) +: kvHandlers
      } else {
        // normal path
        queryHandler = queryParamKeyValueHandler(oAuthParamsBuilder.queryHandler)
        bodyParamHandlers = bodyParamKeyValueHandler(oAuthParamsBuilder.queryHandler) +: kvHandlers
      }

      // add our handlers to the passed-in handlers, to which
      // we'll only send non-oauth key/values.
      val queryHandlers: Seq[KeyValueHandler] = queryHandler +: kvHandlers

      // parse the GET query string
      // we use the normal queryHandlers because query params ARE properly encoded
      UnpackerLibrary.getQueryString(request).map { query =>
        queryParser.parse(query, queryHandlers.asJava)
      }

      // parse the request body if the Content-Type is appropriate. Use the
      // same set of KeyValueHandlers that we used to parse the query string.

      val contentType = UnpackerLibrary.getContentType(request)
      if (contentType.isDefined &&
        contentType.get.startsWith(
          CustomizableUnpacker.WWW_FORM_URLENCODED) && request.body.isDefined) {
        Stat.time(statsReceiver.stat("parse_request_body_request")) {
          // we use our special query handlers here because body params are NOT properly encoded
          parseRequestBodyCounter.incr()
          // use the new request body parser when useNewRequestBodyParser decider is enabled
          if (useNewRequestBodyParser.isAvailable)
            parseRequestBody(request.body.get, bodyParamHandlers.asJava)
          else
            queryParser.parse(request.body.get, bodyParamHandlers.asJava)
        }
      }

      // now we just return the accumulated parameters and OAuthParams
      oAuthParamsBuilder
    }
  }

  /**
   * Since some of OAuth parameters might be embedded in the query string instead
   * of being present in the header, for each one of the request we need to parse
   * both the header map and query string if it is defined to extract all the auth
   * parameters sent by client
   */
  private[this] def parseAuthRequest(
    request: AuthRequest,
    kvHandlers: ArrayList[KeyValueHandler]
  ): OAuthParamsBuilder = {
    Stat.time(statsReceiver.stat("parse_auth_request")) {
      val oAuthParamsBuilder: OAuthParamsBuilder = new OAuthParamsBuilder(helper)
      parseHeader(
        request.headerMap.getOrElse(AuthorizationHeaderKey, null),
        oAuthParamsBuilder.headerHandler)

      /**
       * If the request is OAuth2, we don't need to do further parsing. Because OAuth2
       * only requires the bearer token.
       */
      if (!oAuthParamsBuilder.isOAuth2) {
        val queryHandlers: ArrayList[KeyValueHandler] =
          new ArrayList[KeyValueHandler](kvHandlers.size + 1)
        queryHandlers.add(this.queryParamKeyValueHandler(oAuthParamsBuilder.queryHandler))
        queryHandlers.addAll(kvHandlers)
        val bodyParamHandlers: ArrayList[KeyValueHandler] =
          new ArrayList[KeyValueHandler](kvHandlers.size + 1)
        bodyParamHandlers.add(this.bodyParamKeyValueHandler(oAuthParamsBuilder.queryHandler))
        bodyParamHandlers.addAll(kvHandlers)
        UnpackerLibrary.getQueryString(request).map { query =>
          queryParser.parse(query, queryHandlers)
        }
        val contentType = UnpackerLibrary.getContentType(request)
        if (contentType.isDefined &&
          contentType.get.startsWith(
            CustomizableUnpacker.WWW_FORM_URLENCODED) && request.body.isDefined) {
          Stat.time(statsReceiver.stat("parse_request_body_request")) {
            // we use our special query handlers here because body params are NOT properly encoded
            parseRequestBodyCounter.incr()
            // use the new request body parser when useNewRequestBodyParser decider is enabled
            if (useNewRequestBodyParser.isAvailable)
              parseRequestBody(request.body.get, bodyParamHandlers)
            else
              queryParser.parse(request.body.get, bodyParamHandlers)
          }
        }
      }
      oAuthParamsBuilder
    }
  }

  /**
   * Precheck for auth type header when it is set to oauth2session.
   * If auth type header is oauth2session, but the request auth type
   * is not oauth2 + session which means, the request is either
   * missing bearer token or session token. Here are the possbile
   * auth types for such requests: OAuth1 three legged, OAuth1 two
   * legged, OAuth2 app only or OAuth2 guest auth. In this case,
   * auth type header mismatch result code should be returned.
   */
  private[this] def precheckOauth2SessionAuthTypeHeader(
    request: AuthRequest,
    oAuthParamsBuilder: OAuthParamsBuilder
  ): Option[RequestParams] = {
    Stat.time(statsReceiver.stat("pre_check_oauth2session_auth_type_header")) {
      AuthCookieParser.apply(
        request.headerMap,
        request.cookies,
        shouldAllowWebAuthMultiUserIdHeader,
        statsReceiver
      ) match {
        case Return(Some(authToken: String)) if oAuthParamsBuilder.isOAuth2 =>
          None
        case Return(_) =>
          UnpackerLibrary.getAuthTypeHeader(request).map { _.toLowerCase } match {
            case Some("oauth2session") =>
              // When auth type header is set to oauth2session check if the req auth
              // type is oauth2session. If not, return auth type header mismatch result code
              Some(
                createBadRequestParams(
                  request.headerMap,
                  AuthResultCode.AuthTypeHeaderMismatch,
                  brpOAuth2SessionAuthHeaderMismatch))
            case _ =>
              None
          }
        case Throw(e: AuthMultiCookieError) =>
          Some(
            createBadRequestParams(
              request.headerMap,
              e.code,
              brpOAuth2SessionAuthMultiCookieErrorThrow))
        case Throw(_) =>
          Some(
            createBadRequestParams(
              request.headerMap,
              AuthResultCode.AuthCookieParserUnexpected,
              brpOAuth2SessionAuthMultiCookieErrorUnknown))
        case _ =>
          None
      }
    }
  }

  // Need to verify if the OAuth1 requests are valid or not
  private[this] def isValidOAuth1Req(oAuth1RequestParams: OAuth1RequestParams): Boolean = {
    Stat.time(statsReceiver.stat("validate_oauth1_req")) {
      if (oAuth1RequestParams.scheme == None) false
      else if (oAuth1RequestParams.host == None) false
      else if (oAuth1RequestParams.port == None) false
      else if (oAuth1RequestParams.verb == None) false
      else if (oAuth1RequestParams.path == None) false
      else {
        val oAuth1Params = oAuth1RequestParams.oauthParams
        if (oAuth1Params.signatureMethod == null || !(oAuth1Params.signatureMethod == "HMAC-SHA1") && !(oAuth1Params.signatureMethod == "HMAC-SHA256"))
          false
        if (oAuth1Params.version != null && !(oAuth1Params.version == "1.0") && !(oAuth1Params.version.toLowerCase == "1.0a"))
          false
        if (oAuth1Params.token != null && (oAuth1Params.token.indexOf(
            32) > 0 || oAuth1Params.token.length > 50)) false
        else
          true
      }
    }
  }

  /**
   * Use StringTokenizer instead of String.split because String.split creates a delimiting regular
   * expression which is not be efficient especially when the request body is large and regex
   * expression is not necessary. Use StringTokenizer, indexOf, and substring which are more
   * efficient than the split function.
   * */
  private[this] def parseRequestBody(body: String, handlers: util.List[KeyValueHandler]): Unit = {
    val tokens = new StringTokenizer(body, "&")
    while (tokens.hasMoreTokens) {
      val token = tokens.nextToken()
      val splitIndex = token.indexOf("=")
      splitIndex match {
        case -1 if Option.apply(token).isDefined =>
          handlers.forEach(_.handle(token, ""))
        case index if index > 0 && index <= token.length - 1 =>
          val key = token.substring(0, index)
          if (Option.apply(key).isDefined) {
            index match {
              case length if length == token.length - 1 =>
                handlers.forEach(_.handle(key, ""))
              case _ =>
                val value = token.substring(index + 1, token.length)
                handlers.forEach(_.handle(key, value))
            }
          }
        case _ =>
      }
    }
  }
}

object UrlDecodeThenEncodeTransformer extends Transformer {
  override def transform(s: String): String =
    try {
      UrlCodec.encode(UrlCodec.decode(s))
    } catch {
      case _: Exception =>
        // OOPS! probably something like half a utf-8 character or a dangling % or something malformed
        // pass this along for now... auth will probably fail anyway though
        s
    }
}

class UrlDecodeThenEncodeKeyValueHandler(kvHandler: KeyValueHandler)
    extends TransformingKeyValueHandler(
      kvHandler,
      UrlDecodeThenEncodeTransformer,
      UrlDecodeThenEncodeTransformer
    )

object PassbirdKeyValueCallback extends KeyValueCallback {
  def invoke(kvHandler: KeyValueHandler) = new UrlEncodingNormalizingKeyValueHandler(kvHandler)
}

class StandardTwitterUnpacker(
  helper: OAuthParamsHelper,
  normalizer: Normalizer,
  queryParser: KeyValueParser,
  headerParser: KeyValueParser,
  shouldBeLenientForOAuth1: (AuthRequest, OAuth1Params) => Boolean,
  shouldAllowOAuth2IfNetworkLocal: Boolean,
  shouldAllowOAuth2SessionWithoutAuthTypeHeader: Boolean,
  shouldAllowWebAuthMultiUserIdHeader: Boolean,
  useNewRequestBodyParser: Feature,
  statsReceiver: StatsReceiver)
    extends TwitterRequestUnpacker(
      helper,
      normalizer,
      queryParser,
      headerParser,
      PassbirdKeyValueCallback,
      PassbirdKeyValueCallback,
      PassbirdKeyValueCallback,
      shouldBeLenientForOAuth1,
      shouldAllowOAuth2IfNetworkLocal,
      shouldAllowOAuth2SessionWithoutAuthTypeHeader,
      shouldAllowWebAuthMultiUserIdHeader,
      useNewRequestBodyParser,
      statsReceiver
    )
