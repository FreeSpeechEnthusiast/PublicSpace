package com.twitter.auth.sso.client

import com.google.api.client.googleapis.auth.oauth2.{GoogleIdToken, GoogleIdTokenVerifier}
import com.twitter.auth.sso.models.{Email, IdToken, SsoId, SsoProvider, SsoProviderInfo}
import com.twitter.util.Future

case object MissingProvider extends Exception

abstract class TokenParsingException(message: String) extends Exception(message)

case class InvalidTokenException(token: IdToken)
    extends TokenParsingException(s"Failed to parse and verify $token")
case class MissingPayload(token: IdToken)
    extends TokenParsingException(s"Missing payload for token $token")
case class MissingKeyId(key: String) extends TokenParsingException(s"Missing key id $key")
case class TokenSecurityException(token: IdToken, msg: String)
    extends TokenParsingException(s"Found security exception: $msg while parsing $token")
case class FailedToExtractFromPayload(field: String)
    extends TokenParsingException(s"Failed to extract $field")

/**
 * The SsoProviderClient is abstracts the per-provider sso info/responsibilities.
 * e.g. it will perform any bespoke id token verification (including any that would be
 * deferred to a 3rd party library). If necessary, it will also be responsible for
 * retrieving and maintaining the public/private key pairs that are associated with the
 * provider.
 */
trait SsoProviderClient {
  def isIdTokenValid(idToken: IdToken): Future[Boolean]
  def extractSsoProviderInfo(idToken: IdToken): SsoProviderInfo
  protected def wrapPayload[F, T](raw: F, field: String): T = {
    Option(raw).getOrElse(throw FailedToExtractFromPayload(field)).asInstanceOf[T]
  }
}

class GoogleSsoProviderClient(verifier: GoogleIdTokenVerifier) extends SsoProviderClient {
  private def getGoogleIdToken(idToken: IdToken): GoogleIdToken = verifier.verify(idToken)

  override def isIdTokenValid(idToken: IdToken): Future[Boolean] =
    Future.value(verifier.verify(getGoogleIdToken(idToken)))

  override def extractSsoProviderInfo(idToken: IdToken): SsoProviderInfo = {
    val googleIdToken =
      Option(getGoogleIdToken(idToken)).getOrElse(throw InvalidTokenException(s"$idToken"))

    val payload = Option(googleIdToken.getPayload).getOrElse(throw MissingPayload(idToken))
    val ssoId: SsoId = wrapPayload[String, SsoId](payload.getSubject, "subject")
    val email: Email = wrapPayload[String, Email](payload.getEmail, "email")
    val emailVerified: Option[Boolean] =
      Option(payload.getEmailVerified).map(_.asInstanceOf[Boolean])

    // While SSO Lib returns optional name, for Google, it should always be set.
    val displayName: String = wrapPayload[Object, String](payload.get("name"), "displayName")
    val pictureUrl: Option[String] = Option(payload.get("picture")).map(_.asInstanceOf[String])
    SsoProviderInfo(
      providerSsoId = ssoId,
      emailAddress = email,
      emailVerified = emailVerified,
      displayName = Some(displayName),
      avatarImageUrl = pictureUrl)
  }
}

class IdTokenIntegrityClient(ssoProviders: Map[SsoProvider, SsoProviderClient]) {

  private[client] def getProvider(ssoProvider: SsoProvider): SsoProviderClient =
    ssoProviders.getOrElse(ssoProvider, throw MissingProvider)

  def isIdTokenValid(idToken: IdToken, ssoProvider: SsoProvider): Future[Boolean] = {
    getProvider(ssoProvider).isIdTokenValid(idToken)
  }

  def extractSsoProviderInfo(ssoProvider: SsoProvider, idToken: IdToken): SsoProviderInfo = {
    getProvider(ssoProvider).extractSsoProviderInfo(idToken)
  }
}
