package com.twitter.auth.authentication.authenticator

import com.twitter.auth.authentication.unpacker.UnpackerLibrary
import com.twitter.auth.authenforcement.thriftscala.AuthenticatedUserPrincipal
import com.twitter.auth.authenforcement.thriftscala.ClientApplicationPrincipal
import com.twitter.auth.authenforcement.thriftscala.EmployeePrincipal
import com.twitter.auth.authenforcement.thriftscala.GuestPrincipal
import com.twitter.auth.authenforcement.thriftscala.Principal
import com.twitter.auth.authenforcement.thriftscala.ServiceClientPrincipal
import com.twitter.auth.authenforcement.thriftscala.SessionPrincipal
import com.twitter.auth.authenforcement.thriftscala.UserPrincipal
import com.twitter.auth.authentication.models.AuthResponse
import com.twitter.auth.authentication.models.BadRequestParams
import com.twitter.auth.authentication.models.RequestParams
import com.twitter.auth.authentication.utils.AuthenticationUtils
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType.Unknown
import com.twitter.auth.models.ClientApplication
import com.twitter.auth.passporttype.thriftscala.PassportType
import com.twitter.finagle.mtls.authentication.ServiceIdentifier
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.transport.Transport

object AuthenticatorLibrary {

  private[auth] val authenticateResponseUnknownAuthType: AuthResponse = AuthResponse()

  /**
   * TiaTokenFields (TIA) is normally the request header X-TFE-Transaction-ID, and is used for
   * generating user assertion signature header (X-TFE-User-Assertion-Signature) for TIA requests.
   *
   * TiaTokenFields (TIA) is used for all APIs against
   *   - getAccessTokenByToken
   *   - getWebTokenByToken
   */
  def createTiaTokenFields(params: RequestParams): Option[Seq[String]] = {
    if (params.passportId == UnpackerLibrary.Unknown)
      None
    else
      Some(Seq(params.passportId))
  }

  // Delay client application principal generation to be done after verifying partner
  // application. Since if there exists active partner app. The original app will be
  // replaced by partner application.
  def createPrincipals(
    tokenHashOpt: Option[String],
    userIdOpt: Option[Long],
    scopes: Option[Set[String]],
    actAsUserIdOpt: Option[Long],
    guestToken: Option[Long],
    ldapOpt: Option[String],
    clientId: Option[String],
    statsReceiver: StatsReceiver
  ): Set[Principal] = {
    val authenticatedUserPrincipal = createAuthenticatedUserPrincipal(userIdOpt)
    val userPrincipal = (userIdOpt, actAsUserIdOpt) match {
      // if both userId, actAsUserId are present AND they are different, we set actAsUserId to UserPrincipal
      case (Some(userId), Some(actAsUserId)) if userId != actAsUserId =>
        createUserPrincipal(Some(actAsUserId))
      case (Some(userId), _) =>
        createUserPrincipal(Some(userId))
      case _ => None
    }
    val guestPrincipal = createGuestPrincipal(guestToken)
    val serviceClientPrincipal = createServiceClientPrincipal(clientId)
    if (authenticatedUserPrincipal.isEmpty && userPrincipal.isDefined) {
      // should never happen
      statsReceiver.scope("principals").counter("miss_authenticated_user_principal").incr()
    }
    if (AuthenticationUtils.isDeveloperPassport(
        ServiceIdentifier.fromCertificate(Transport.peerCertificate))) {
      val employeePrincipal = createEmployeePrincipal(ldapOpt)
      Set(
        createSessionPrincipal(tokenHashOpt, scopes),
        userPrincipal,
        authenticatedUserPrincipal,
        employeePrincipal,
        guestPrincipal,
        serviceClientPrincipal
      ).flatten
    } else {
      Set(
        createSessionPrincipal(tokenHashOpt, scopes),
        userPrincipal,
        authenticatedUserPrincipal,
        guestPrincipal,
        serviceClientPrincipal
      ).flatten
    }
  }

  private[this] def createSessionPrincipal(
    tokenHashOpt: Option[String],
    scopesOpt: Option[Set[String]]
  ): Option[Principal] = {
    scopesOpt match {
      case Some(scopes) => // always populate the session principal if scopes is defined
        Some(Principal.SessionPrincipal(SessionPrincipal(tokenHashOpt.getOrElse(""), Some(scopes))))
      case None if tokenHashOpt.isDefined =>
        Some(Principal.SessionPrincipal(SessionPrincipal(tokenHashOpt.get, None)))
      case _ =>
        None
    }
  }

  private[this] def createClientApplicationPrincipal(
    clientAppOpt: Option[ClientApplication]
  ): Option[Principal] = {
    clientAppOpt.map(clientApp =>
      Principal.ClientApplicationPrincipal(ClientApplicationPrincipal(clientApp.id)))
  }

  private[this] def createUserPrincipal(userIdOpt: Option[Long]): Option[Principal] = {
    userIdOpt.map(userId => Principal.UserPrincipal(UserPrincipal(userId)))
  }

  private[this] def createAuthenticatedUserPrincipal(userIdOpt: Option[Long]): Option[Principal] = {
    userIdOpt.map(userId =>
      Principal.AuthenticatedUserPrincipal(AuthenticatedUserPrincipal(userId)))
  }

  private[this] def createEmployeePrincipal(ldapOpt: Option[String]): Option[Principal] = {
    ldapOpt.map(ldap => Principal.EmployeePrincipal(EmployeePrincipal(Some(ldap))))
  }

  private[this] def createServiceClientPrincipal(clientIdOpt: Option[String]): Option[Principal] = {
    clientIdOpt.map(clientId => Principal.ServiceClientPrincipal(ServiceClientPrincipal(clientId)))
  }

  private[this] def createGuestPrincipal(guestToken: Option[Long]): Option[Principal] = {
    guestToken.map(tokenId => Principal.GuestPrincipal(GuestPrincipal(tokenId)))
  }

  private[auth] def passportType(serviceIdentifier: ServiceIdentifier): Option[PassportType] = {
    val isDeveloper = AuthenticationUtils.isDeveloperPassport(serviceIdentifier)
    if (isDeveloper) Some(PassportType.EmployeeDeveloper) else Some(PassportType.UserRequest)
  }

  private[auth] def createLdap(serviceIdentifier: ServiceIdentifier): Option[String] =
    Some(serviceIdentifier.role)

  def createAuthenticateResponseInternalServerError(
    useNewAuthNFilter: Boolean
  ): AuthResponse = {
    createAuthResponse(AuthResultCode.InternalServerError, useNewAuthNFilter, Unknown)
  }

  def createAuthenticateResponseUnknownAuthType(
    useNewAuthNFilter: Boolean
  ): AuthResponse = {
    createAuthResponse(AuthResultCode.UnknownAuthType, useNewAuthNFilter, Unknown)
  }

  def createAuthenticateResponseFromBadParams(
    p: BadRequestParams,
    useNewAuthNFilter: Boolean,
    authType: AuthenticationType
  ): AuthResponse = {
    createAuthResponse(p.authResult, useNewAuthNFilter, authType)
  }

  private[auth] def createAuthResponse(
    authResultCode: AuthResultCode,
    useNewAuthNFilter: Boolean,
    authType: AuthenticationType
  ): AuthResponse = {
    AuthResponse(
      passport = None,
      authResultCode = Some(authResultCode),
      externalAuthResultCode = Some(AuthenticationUtils.getExternalAuthResultCode(authResultCode)),
      useNewAuthNFilter = Some(useNewAuthNFilter),
      authenticationType = Some(authType),
      userAssertionSignature = None
    )
  }
}
