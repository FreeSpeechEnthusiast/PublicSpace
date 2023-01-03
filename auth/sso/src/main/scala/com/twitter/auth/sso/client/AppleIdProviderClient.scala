package com.twitter.auth.sso.client

import com.google.api.client.auth.openidconnect.IdToken
import com.google.api.client.auth.openidconnect.IdTokenVerifier
import com.google.api.client.json.jackson2.JacksonFactory
import com.twitter.conversions.DurationOps._
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.auth.sso.models.Email
import com.twitter.auth.sso.models.SsoId
import com.twitter.auth.sso.models.SsoProviderInfo
import com.twitter.auth.sso.models.{IdToken => RawIdToken}
import com.twitter.cache.Refresh
import com.twitter.finatra.http.marshalling.mapper._
import com.twitter.finagle.http.RequestBuilder
import com.twitter.finagle.Service
import com.twitter.finagle.http
import com.twitter.util.Future
import com.twitter.util.Try
import java.math.BigInteger
import java.security.PublicKey
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

class AppleIdProviderClient(
  httpService: Service[http.Request, http.Response],
  scalaObjectMapper: ScalaObjectMapper,
  idTokenVerifier: IdTokenVerifier)
    extends SsoProviderClient {

  import AppleIdProviderClient._

  // Unlike Google, Apple does not have a plug and play library, nor does its API tell us when
  // their keys expire so we refresh every hour.
  private val keys = Refresh.every(KeyRefreshFrequency) { getKeys }

  override def isIdTokenValid(idToken: RawIdToken): Future[Boolean] = {
    keys().map { keys =>
      val parsedIdToken = IdToken.parse(JacksonFactory.getDefaultInstance(), idToken)
      val keyId = parsedIdToken.getHeader.getKeyId
      val key = keys.getOrElse(keyId, throw MissingKeyId(keyId))
      idTokenVerifier.verify(parsedIdToken) && parsedIdToken.verifySignature(key)
    }
  }

  override def extractSsoProviderInfo(idToken: RawIdToken): SsoProviderInfo = {
    val parsedIdToken = IdToken.parse(JacksonFactory.getDefaultInstance(), idToken)
    val payload = Option(parsedIdToken.getPayload).getOrElse(throw MissingPayload(idToken))
    val ssoId: SsoId = wrapPayload[String, SsoId](payload.getSubject, "subject")
    val email: Email = wrapPayload[Object, Email](payload.get("email"), "email")
    val emailVerified: Option[Boolean] = Try {
      payload.get("email_verified") match {
        case bool: java.lang.Boolean => bool.booleanValue()
        case obj => obj.asInstanceOf[String].toBoolean
      }
    }.toOption
    SsoProviderInfo(ssoId, email, emailVerified)
  }

  private def getKeys(): Future[Map[String, PublicKey]] = {
    httpService(RequestBuilder().url(KeyRefreshUrl).buildGet()).map { response =>
      scalaObjectMapper
        .parseMessageBody[AppleKeySet](response).keys.map { key =>
          key.kid -> key.toPublicKey
        }.toMap
    }
  }
}

object AppleIdProviderClient {
  final val KeyRefreshUrl = "https://appleid.apple.com/auth/keys"
  // https://developer.apple.com/documentation/sign_in_with_apple/sign_in_with_apple_rest_api/authenticating_users_with_sign_in_with_apple#3383773
  final val KeyRefreshFrequency = 1.hour
}

// https://developer.apple.com/documentation/sign_in_with_apple/jwkset/keys
case class AppleKey(alg: String, e: String, kid: String, kty: String, n: String, use: String) {
  def toPublicKey: PublicKey = {
    val number = new BigInteger(1, Base64.getUrlDecoder().decode(n))
    val exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e))
    val keyFactory = KeyFactory.getInstance(kty)
    val publicKeySpec = new RSAPublicKeySpec(number, exponent)
    keyFactory.generatePublic(publicKeySpec)
  }
}

// https://developer.apple.com/documentation/sign_in_with_apple/jwkset
case class AppleKeySet(keys: Seq[AppleKey])
