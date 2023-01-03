package com.twitter.auth.authentication.authenticator

import com.twitter.auth.authenforcement.thriftscala.ClientApplicationPrincipal
import com.twitter.auth.authenforcement.thriftscala.LegacyMetadata
import com.twitter.auth.authenforcement.thriftscala.Passport
import com.twitter.auth.authenforcement.thriftscala.Principal
import com.twitter.auth.authentication.models._
import com.twitter.auth.authentication.utils.AccessTokenUtils
import com.twitter.auth.authentication.utils.AuthenticationUtils
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType._
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode
import com.twitter.auth.models._
import com.twitter.auth.passportsigning.CryptoUtils
import com.twitter.auth.passporttype.thriftscala.PassportType
import com.twitter.decider.Feature
import com.twitter.finagle.mtls.authentication.ServiceIdentifier
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.transport.Transport
import com.twitter.util.Future
import java.nio.ByteBuffer
import java.util

object AuthenticateResponseHandler {
  val AppOnlyScope = "AppOnly"
  val SessionScope = "Session"
  val GuestAuthScope = "Guest"
  val TiaScope = "Tia"
  val appOnlyUserId = 0L

  def badResponse(
    authResultCode: AuthResultCode,
    useNewAuthNFilter: Boolean,
    authenticationType: AuthenticationType
  ): AuthResponse = {
    AuthResponse(
      passport = None,
      legacyMetadata = None,
      authResultCode = Some(authResultCode),
      externalAuthResultCode = Some(AuthenticationUtils.getExternalAuthResultCode(authResultCode)),
      useNewAuthNFilter = Some(useNewAuthNFilter),
      authenticationType = Some(authenticationType)
    )
  }

  def oAuth1ThreeLeggedWithAccessToken(
    aToken: OAuth1AccessToken,
    app: ClientApplication,
    params: OAuth1ThreeLeggedRequestParams,
    partnerAppParams: Option[PartnerAppParams],
    actAsUserId: Option[Long],
    legacyMetadata: Option[LegacyMetadata],
    useNewAuthNFilter: Boolean,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = Some(aToken.tokenHash),
      userIdOpt = Some(aToken.userId),
      scopes = bytesToScopes(aToken.privileges),
      actAsUserIdOpt = actAsUserId,
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = None,
      statsReceiver = statsReceiver.scope("AuthenticatorLibrary")
    )
    val authResultCode = RequestCredentialVerifier
      .verifyOAuth1ThreeLeggedRequestWithAccessToken(aToken, app, params)
    // only grant Passport to authenticated request
    authResultCode match {
      case AuthResultCode.Ok =>
        val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)
        createFinalAuthenticateResponse(
          params.passportId,
          principals,
          Some(aToken),
          app,
          None,
          None,
          partnerAppParams,
          legacyMetadata,
          useNewAuthNFilter,
          passportType,
          Oauth1,
          cryptoUtils,
          passportPasetoSignatureEnabled
        )
      case _ =>
        badResponse(authResultCode, useNewAuthNFilter, Oauth1)
    }
  }

  /**
   * This auth type is only used by '/oauth/access_token' API:
   * https://developer.twitter.com/en/docs/basics/authentication/api-reference/access_token
   * and shouldn't need extra scope/privilege
   */
  def oAuth1ThreeLeggedWithRequestToken(
    requestToken: OAuth1RequestToken,
    app: ClientApplication,
    params: OAuth1ThreeLeggedRequestParams,
    legacyMetadata: Option[LegacyMetadata],
    useNewAuthNFilter: Boolean,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = None,
      userIdOpt = Some(appOnlyUserId),
      // We don't grant any scope since there's only one whitelisted API which needs no DP
      scopes = None,
      actAsUserIdOpt = None,
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = None,
      statsReceiver = statsReceiver.scope("AuthenticatorLibrary"),
    )
    val authResultCode = RequestCredentialVerifier.verifyOAuth1ThreeLeggedRequestWithRequestToken(
      requestToken,
      app,
      params
    )
    authResultCode match {
      case AuthResultCode.Ok =>
        val updatedPrincipal = updateClientAppIdInPrincipal(Some(app), principals)
        val passport = Passport(
          passportId = params.passportId,
          principals = updatedPrincipal,
          passportType = AuthenticatorLibrary.passportType(serviceIdentifier)
        )
        AuthResponse(
          passport = Some(passport),
          legacyMetadata = legacyMetadata,
          authResultCode = Some(authResultCode),
          externalAuthResultCode =
            Some(AuthenticationUtils.getExternalAuthResultCode(authResultCode)),
          useNewAuthNFilter = Some(useNewAuthNFilter),
          authenticationType = Some(Oauth1RequestToken)
        )
      case _ =>
        badResponse(authResultCode, useNewAuthNFilter, Oauth1RequestToken)
    }
  }

  def oAuth1TwoLegged(
    app: ClientApplication,
    params: OAuth1TwoLeggedRequestParams,
    partnerAppParams: Option[PartnerAppParams],
    legacyMetadata: Option[LegacyMetadata],
    useNewAuthNFilter: Boolean,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    // need to check whether OAuth1-two-legged request contains special headers
    // which makes the request become Xauth request
    val isXauth = AuthenticationUtils.isXauth(params.otherParams)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = None,
      userIdOpt = Some(appOnlyUserId), //Set the user ID as 0 to make it consistent with legacy code
      scopes = Some(Set[String](AppOnlyScope)),
      actAsUserIdOpt = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      guestToken = None,
      clientId = None,
      statsReceiver = statsReceiver
    )
    val authResultCode = RequestCredentialVerifier.verifyOAuth1TwoLeggedRequest(app, params)
    // only grant Passport to authenticated request
    authResultCode match {
      case AuthResultCode.Ok =>
        val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)
        createFinalAuthenticateResponse(
          params.passportId,
          principals,
          None,
          app,
          None,
          None,
          partnerAppParams,
          legacyMetadata,
          useNewAuthNFilter,
          passportType,
          if (isXauth) Oauth1Xauth else Oauth1TwoLegged,
          cryptoUtils,
          passportPasetoSignatureEnabled
        )
      case _ =>
        badResponse(
          authResultCode,
          useNewAuthNFilter,
          if (isXauth) Oauth1Xauth else Oauth1TwoLegged)
    }
  }

  def oauth2ClientCredential(
    bearerToken: OAuth2ClientAccessToken,
    serviceClientOpt: Option[OAuth2ServiceClient],
    appOpt: Option[ClientApplication],
    params: OAuth2ClientCredentialRequestParams,
    legacyMetadata: Option[LegacyMetadata],
    useNewAuthNFilter: Boolean,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val clientId: Option[String] = serviceClientOpt.map(t => t.clientId)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = Some(bearerToken.tokenHash),
      userIdOpt = None,
      scopes = Some(bearerToken.scopes),
      actAsUserIdOpt = None,
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = clientId,
      statsReceiver
    )
    val updatedPrincipals = appOpt match {
      case Some(clientApplication) =>
        updateClientAppIdInPrincipal(Some(clientApplication), principals)
      case _ =>
        principals
    }
    val clientPassportType = AuthenticatorLibrary.passportType(serviceIdentifier)
    createOkAuthenticateResponse(
      passportId = params.passportId,
      principals = updatedPrincipals,
      useNewAuthNFilter = useNewAuthNFilter,
      passportType = clientPassportType,
      legacyMetadata = legacyMetadata,
      tiaToken = None,
      authenticationType = Oauth2ClientCredential,
      cryptoUtils = cryptoUtils,
      passportPasetoSignatureEnabled = passportPasetoSignatureEnabled
    )
  }

  def oauth2Session(
    app: ClientApplication,
    sessionToken: SessionToken,
    bearerToken: OAuth2AppOnlyToken,
    actAsUserId: Option[Long],
    params: OAuth2SessionRequestParams,
    partnerAppParams: Option[PartnerAppParams],
    authResultCode: AuthResultCode,
    legacyMetadata: Option[LegacyMetadata],
    useNewAuthNFilter: Boolean,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = Some(sessionToken.tokenHash),
      userIdOpt = Some(sessionToken.userId),
      scopes = Some(Set[String](SessionScope)),
      actAsUserIdOpt = actAsUserId,
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = None,
      statsReceiver = statsReceiver
    )
    val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)
    // TODO: Here aligned with legacy auth flow creating user assertion signature against bearer token
    createFinalAuthenticateResponse(
      passportId = params.passportId,
      principals = principals,
      accessToken = Some(bearerToken),
      clientApp = app,
      sessionToken = Some(sessionToken),
      requestToken = None,
      partnerAppParams = partnerAppParams,
      legacyMetadata = legacyMetadata,
      useNewAuthNFilter = useNewAuthNFilter,
      passportType = passportType,
      authenticationType = Oauth2SessionAuth,
      cryptoUtils = cryptoUtils,
      passportPasetoSignatureEnabled = passportPasetoSignatureEnabled
    )
  }

  def session(
    token: SessionToken,
    actAsUserId: Option[Long],
    params: SessionRequestParams,
    legacyMetadata: Option[LegacyMetadata],
    useNewAuthNFilter: Boolean,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = Some(token.tokenHash),
      //clientAppOpt = None,
      userIdOpt = Some(token.userId),
      scopes = Some(Set[String](SessionScope)),
      actAsUserIdOpt = actAsUserId,
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = None,
      statsReceiver = statsReceiver
    )
    val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)
    createOkAuthenticateResponse(
      params.passportId,
      principals,
      useNewAuthNFilter,
      passportType,
      legacyMetadata,
      token.tiaToken,
      Session,
      cryptoUtils,
      passportPasetoSignatureEnabled)
  }

  def restrictedSession(
    sessionToken: SessionToken,
    scopes: scala.collection.Set[String],
    actAsUserId: Option[Long],
    passportId: String,
    legacyMetadata: Option[LegacyMetadata],
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = Some(sessionToken.tokenHash),
      userIdOpt = Some(sessionToken.userId),
      scopes = Some(scopes.toSet),
      actAsUserIdOpt = actAsUserId,
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = None,
      statsReceiver
    )
    val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)

    createOkAuthenticateResponse(
      passportId = passportId,
      principals = principals,
      // we always wants to use new authN filter for restrictedSession as this is new auth type
      // and it does not exist in TFE's passbird client so we always wants to use the new authN
      // filter for this new auth type
      useNewAuthNFilter = true,
      passportType = passportType,
      legacyMetadata = legacyMetadata,
      tiaToken = sessionToken.tiaToken,
      authenticationType = RestrictedSession,
      cryptoUtils = cryptoUtils,
      passportPasetoSignatureEnabled = passportPasetoSignatureEnabled
    )
  }

  def restrictedOAuth2Session(
    app: ClientApplication,
    sessionToken: SessionToken,
    scopes: scala.collection.Set[String],
    partnerAppParams: Option[PartnerAppParams],
    bearerToken: OAuth2AppOnlyToken,
    actAsUserId: Option[Long],
    passportId: String,
    legacyMetadata: Option[LegacyMetadata],
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = Some(sessionToken.tokenHash),
      userIdOpt = Some(sessionToken.userId),
      scopes = Some(scopes.toSet),
      actAsUserIdOpt = actAsUserId,
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = None,
      statsReceiver
    )
    val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)

    createFinalAuthenticateResponse(
      passportId = passportId,
      principals = principals,
      accessToken = Some(bearerToken),
      clientApp = app,
      sessionToken = Some(sessionToken),
      requestToken = None,
      partnerAppParams = partnerAppParams,
      legacyMetadata = legacyMetadata,
      //useNewAuthNFilter field will always be true since the new RestrictedSession auth type only
      //exists in the new flow
      useNewAuthNFilter = true,
      passportType = passportType,
      authenticationType = RestrictedOauth2Session,
      cryptoUtils = cryptoUtils,
      passportPasetoSignatureEnabled = passportPasetoSignatureEnabled
    )
  }

  def oauth2AppOnly(
    params: OAuth2RequestParams,
    partnerAppParams: Option[PartnerAppParams],
    app: ClientApplication,
    bearerToken: AccessToken,
    legacyMetadata: Option[LegacyMetadata],
    useNewAuthNFilter: Boolean,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      // We don't expect session hash exist for app-only requests: AUTHPLT-24
      tokenHashOpt = None,
      userIdOpt = Some(appOnlyUserId),
      scopes = Some(Set[String](AppOnlyScope)),
      actAsUserIdOpt = None,
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = None,
      statsReceiver
    )
    val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)
    // TODO: Here aligned with legacy auth flow creating user assertion signature against bearer token
    createFinalAuthenticateResponse(
      params.passportId,
      principals,
      Some(bearerToken),
      app,
      None,
      None,
      partnerAppParams,
      legacyMetadata,
      useNewAuthNFilter,
      passportType,
      Oauth2AppOnly,
      cryptoUtils,
      passportPasetoSignatureEnabled
    )
  }

  def oauth2User(
    params: OAuth2RequestParams,
    partnerAppParams: Option[PartnerAppParams],
    app: ClientApplication,
    bearerToken: OAuth2AccessToken,
    legacyMetadata: Option[LegacyMetadata],
    useNewAuthNFilter: Boolean,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = Some(bearerToken.tokenHash),
      userIdOpt = Some(bearerToken.userId),
      scopes = Some(bearerToken.scopes),
      actAsUserIdOpt = None, // TODO Act-As for OAuth2
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = None,
      statsReceiver
    )
    val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)
    createFinalAuthenticateResponse(
      passportId = params.passportId,
      principals = principals,
      accessToken = Some(bearerToken),
      clientApp = app,
      sessionToken = None,
      requestToken = None,
      partnerAppParams = partnerAppParams,
      legacyMetadata = legacyMetadata,
      useNewAuthNFilter = useNewAuthNFilter,
      passportType = passportType,
      authenticationType = Oauth2,
      cryptoUtils = cryptoUtils,
      passportPasetoSignatureEnabled = passportPasetoSignatureEnabled
    )
  }

  def ampEmailUser(
    params: AmpEmailRequestParams,
    partnerAppParams: Option[PartnerAppParams],
    app: ClientApplication,
    bearerToken: OAuth2AccessToken,
    legacyMetadata: Option[LegacyMetadata],
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = Some(bearerToken.tokenHash),
      userIdOpt = Some(bearerToken.userId),
      scopes = Some(bearerToken.scopes),
      actAsUserIdOpt = None,
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = None,
      statsReceiver
    )
    val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)
    createFinalAuthenticateResponse(
      passportId = params.passportId,
      principals = principals,
      accessToken = Some(bearerToken),
      clientApp = app,
      sessionToken = None,
      requestToken = None,
      partnerAppParams = partnerAppParams,
      legacyMetadata = legacyMetadata,
      useNewAuthNFilter = true,
      passportType = passportType,
      authenticationType = Oauth2AmpEmail,
      cryptoUtils = cryptoUtils,
      passportPasetoSignatureEnabled = passportPasetoSignatureEnabled
    )
  }

  def guestAuth(
    app: ClientApplication,
    params: GuestAuthRequestParams,
    partnerAppParams: Option[PartnerAppParams],
    bearerToken: AccessToken,
    legacyMetadata: Option[LegacyMetadata],
    verifyGuestToken: Boolean,
    useNewAuthNFilter: Boolean,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      // We don't expect session hash exist for app-only requests: AUTHPLT-24
      tokenHashOpt = None,
      userIdOpt = Some(appOnlyUserId),
      scopes = Some(Set[String](GuestAuthScope)),
      actAsUserIdOpt = None,
      guestToken = params.guestToken,
      ldapOpt = None,
      clientId = None,
      statsReceiver = statsReceiver
    )
    // Generate auth response for guest auth request
    verifyGuestToken match {
      case true =>
        statsReceiver.counter("valid_guest_token").incr()
        val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)
        createFinalAuthenticateResponse(
          params.passportId,
          principals,
          Some(bearerToken),
          app,
          None,
          None,
          partnerAppParams,
          legacyMetadata,
          useNewAuthNFilter,
          passportType,
          Oauth2GuestAuth,
          cryptoUtils,
          passportPasetoSignatureEnabled
        )
      case false =>
        statsReceiver.counter("invalid_guest_token").incr()
        badResponse(AuthResultCode.BadGuestToken, useNewAuthNFilter, Oauth2GuestAuth)
    }
  }

  def tia(
    app: Option[ClientApplication],
    params: TiaAuthRequestParams,
    tiaToken: TiaToken,
    legacyMetadata: Option[LegacyMetadata],
    useNewAuthNFilter: Boolean,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
    statsReceiver: StatsReceiver
  ): AuthResponse = {
    val serviceIdentifier = ServiceIdentifier.fromCertificate(Transport.peerCertificate)
    val principals = AuthenticatorLibrary.createPrincipals(
      tokenHashOpt = None,
      userIdOpt = params.userId,
      scopes = Some(Set[String](TiaScope)),
      actAsUserIdOpt = None,
      guestToken = None,
      ldapOpt = AuthenticatorLibrary.createLdap(serviceIdentifier),
      clientId = None,
      statsReceiver
    )
    val passportType = AuthenticatorLibrary.passportType(serviceIdentifier)
    val updatedPrincipal = updateClientAppIdInPrincipal(app, principals)
    createOkAuthenticateResponse(
      params.passportId,
      updatedPrincipal,
      useNewAuthNFilter,
      passportType,
      legacyMetadata,
      None,
      AuthenticationType.Tia,
      cryptoUtils,
      passportPasetoSignatureEnabled
    )
  }

  def traceAuthenticateResponse(authenticateResponseFuture: Future[AuthResponse]): Unit = {
    authenticateResponseFuture
      .onSuccess { authenticateResponse =>
        // Masking legacy metadata as it contains sensitive information
        Trace.recordBinary(
          "authenticated.response",
          authenticateResponse
            .copy(legacyMetadata = None)
            .toString)
      }
      .onFailure(e => Trace.recordBinary("authenticated.response", e.getMessage))
  }

  private[this] def bytesToScopes(bytes: Option[ByteBuffer]): Option[Set[String]] = {
    bytes match {
      case None => Some(Set[String]().empty)
      case Some(bytes) =>
        Some(AccessTokenUtils.privilegesToScopes(util.BitSet.valueOf(bytes)).toSet)
    }
  }

  // User Assertion Signature for setting header X-TFE-User-Assertion-Signature
  // signature comes from getAccessTokenByToken/getWebAccessTokenByToken API when
  // provides "passbirdToken" field
  private[this] def userAssertionSignature(tiaToken: Option[TiaToken]): Option[String] =
    tiaToken.map(_.signatureBase64)

  private[this] def createOkAuthenticateResponse(
    passportId: String,
    principals: Set[Principal],
    useNewAuthNFilter: Boolean,
    passportType: Option[PassportType],
    legacyMetadata: Option[LegacyMetadata],
    tiaToken: Option[TiaToken],
    authenticationType: AuthenticationType,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
  ): AuthResponse = {
    val passport = Passport(
      passportId = passportId,
      principals = principals,
      passportType = passportType
    )
    AuthResponse(
      passport = Some(passport),
      authResultCode = Some(AuthResultCode.Ok),
      externalAuthResultCode =
        Some(AuthenticationUtils.getExternalAuthResultCode(AuthResultCode.Ok)),
      useNewAuthNFilter = Some(useNewAuthNFilter),
      authenticationType = Some(authenticationType),
      legacyMetadata = legacyMetadata,
      userAssertionSignature = userAssertionSignature(tiaToken)
    )
  }

  private[this] def createFinalAuthenticateResponse(
    passportId: String,
    principals: Set[Principal],
    accessToken: Option[AccessToken],
    clientApp: ClientApplication,
    sessionToken: Option[SessionToken],
    requestToken: Option[OAuth1RequestToken],
    partnerAppParams: Option[PartnerAppParams],
    legacyMetadata: Option[LegacyMetadata],
    useNewAuthNFilter: Boolean,
    passportType: Option[PassportType],
    authenticationType: AuthenticationType,
    cryptoUtils: CryptoUtils,
    passportPasetoSignatureEnabled: Feature,
  ): AuthResponse = {
    partnerAppParams match {
      case Some(partnerAppParams: PartnerAppParams) =>
        partnerAppParams.authResultCode match {
          case AuthResultCode.Ok =>
            val authToken = partnerAppParams.partnerToken match {
              // If found valid partner app token, replace the
              // original access token with partner app token
              case Some(token) =>
                Some(token)
              // If no partner app token is found, pass original
              // access token in legacy metadata
              case _ =>
                accessToken
            }
            val clientApplication = partnerAppParams.partnerApp.getOrElse(clientApp)
            val aToken =
              getAuthTokenForAssertionSignatureHeader(authenticationType, authToken, sessionToken)
            val updatedPincipals = updateClientAppIdInPrincipal(Some(clientApplication), principals)
            createOkAuthenticateResponse(
              passportId = passportId,
              principals = updatedPincipals,
              useNewAuthNFilter = useNewAuthNFilter,
              passportType = passportType,
              legacyMetadata = legacyMetadata,
              tiaToken = aToken,
              authenticationType = authenticationType,
              cryptoUtils = cryptoUtils,
              passportPasetoSignatureEnabled = passportPasetoSignatureEnabled
            )
          case _ =>
            // If any error detected while validating the partner app(either invalid token or inactive app)
            // return the bad auth result code
            badResponse(partnerAppParams.authResultCode, useNewAuthNFilter, authenticationType)
        }
      case None =>
        val updatedPrincipals = updateClientAppIdInPrincipal(Some(clientApp), principals)
        val aToken =
          getAuthTokenForAssertionSignatureHeader(authenticationType, accessToken, sessionToken)
        createOkAuthenticateResponse(
          passportId = passportId,
          principals = updatedPrincipals,
          useNewAuthNFilter = useNewAuthNFilter,
          passportType = passportType,
          legacyMetadata = legacyMetadata,
          tiaToken = aToken,
          authenticationType = authenticationType,
          cryptoUtils = cryptoUtils,
          passportPasetoSignatureEnabled = passportPasetoSignatureEnabled
        )
    }
  }

  // Update the client application principal after verifying partner application
  // If there exists active partner app, it will replace the original client app
  def updateClientAppIdInPrincipal(
    clientApp: Option[ClientApplication],
    principals: Set[Principal]
  ): Set[Principal] = {
    clientApp match {
      case Some(app) =>
        principals.union(
          Set(Principal.ClientApplicationPrincipal(ClientApplicationPrincipal(app.id))))
      case _ =>
        principals
    }
  }

  def getAuthTokenForAssertionSignatureHeader(
    authenticationType: AuthenticationType,
    accessToken: Option[AccessToken],
    sessionToken: Option[SessionToken]
  ): Option[TiaToken] = {
    authenticationType match {
      // for oauth2session req, session token signature should be set
      // as the value of X-TFE-User-Assertion-Signature
      case Oauth2SessionAuth | RestrictedOauth2Session => sessionToken.flatMap(_.tiaToken)
      case _ => accessToken.flatMap(_.tiaToken)
    }
  }
}
