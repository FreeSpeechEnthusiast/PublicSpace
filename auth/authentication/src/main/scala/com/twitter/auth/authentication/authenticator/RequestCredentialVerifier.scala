package com.twitter.auth.authentication.authenticator

import com.twitter.auth.authentication.models.GuestAuthRequestParams
import com.twitter.auth.authentication.models.OAuth1RequestParams
import com.twitter.auth.authentication.models.OAuth2RequestParams
import com.twitter.auth.authentication.models.OAuth2SessionRequestParams
import com.twitter.auth.authentication.utils.CredentialVerifierUtils
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode.BadAccessToken
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode.BadClientKey
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode.OrphanedAccessToken
import com.twitter.auth.models.ClientApplication
import com.twitter.auth.models.OAuth1AccessToken
import com.twitter.auth.models.OAuth1RequestToken
import com.twitter.auth.models.OAuth2AccessToken
import com.twitter.auth.models.OAuth2AppOnlyToken
import com.twitter.auth.models.OAuth2ClientAccessToken
import com.twitter.auth.passportsigning.CryptoUtils

object RequestCredentialVerifier {

  val EMPTY_STRING: String = ""

  def verifyOAuth1ThreeLeggedRequestWithAccessToken(
    accessToken: OAuth1AccessToken,
    clientApp: ClientApplication,
    oAuth1RequestParams: OAuth1RequestParams
  ): AuthResultCode = {
    CredentialVerifierUtils.validateConsumerKey(
      clientApp.consumerKey,
      oAuth1RequestParams.oauthParams.consumerKey()) match {
      case true =>
        CredentialVerifierUtils.verifyOAuth1AccessToken(accessToken) match {
          case AuthResultCode.Ok =>
            CredentialVerifierUtils.verifyOAuth1Request(
              oAuth1RequestParams,
              accessToken.secret,
              clientApp.secret,
              clientApp.id)
          case OrphanedAccessToken => OrphanedAccessToken
          case _ => BadAccessToken
        }
      case _ =>
        BadClientKey
    }
  }

  def verifyOAuth1ThreeLeggedRequestWithRequestToken(
    requestToken: OAuth1RequestToken,
    clientApp: ClientApplication,
    oAuth1RequestParams: OAuth1RequestParams
  ): AuthResultCode = {
    CredentialVerifierUtils.validateConsumerKey(
      clientApp.consumerKey,
      oAuth1RequestParams.oauthParams.consumerKey()) match {
      case true =>
        CredentialVerifierUtils.verifyOAuth1Request(
          oAuth1RequestParams: OAuth1RequestParams,
          requestToken.secret,
          clientApp.secret,
          clientApp.id
        )
      case _ =>
        BadClientKey
    }
  }

  def verifyOAuth1TwoLeggedRequest(
    clientApp: ClientApplication,
    oAuth1RequestParams: OAuth1RequestParams
  ): AuthResultCode = {
    CredentialVerifierUtils.validateConsumerKey(
      clientApp.consumerKey,
      oAuth1RequestParams.oauthParams.consumerKey()) match {
      case true =>
        CredentialVerifierUtils.verifyOAuth1Request(
          oAuth1RequestParams,
          EMPTY_STRING,
          clientApp.secret,
          clientApp.id
        )
      case false =>
        BadClientKey
    }
  }

  def verifyOAuth2Session(
    bearerToken: OAuth2AppOnlyToken,
    clientApp: ClientApplication,
    params: OAuth2SessionRequestParams,
    actAsUserId: Option[Long]
  ): AuthResultCode = {
    CredentialVerifierUtils.verifyOAuth2AppOnlyToken(bearerToken)
  }

  def verifyOAuth2AppOnly(
    bearerToken: OAuth2AppOnlyToken,
    clientApp: ClientApplication,
    params: OAuth2RequestParams
  ): AuthResultCode = {
    CredentialVerifierUtils.verifyOAuth2AppOnlyToken(bearerToken)
  }

  def verifyOAuth2ClientCredential(
    bearerToken: OAuth2ClientAccessToken
  ): AuthResultCode = {
    CredentialVerifierUtils.verifyOAuth2ClientAccessToken(bearerToken)
  }

  def verifyOAuth2User(
    bearerToken: OAuth2AccessToken,
    clientApp: ClientApplication,
    params: OAuth2RequestParams
  ): AuthResultCode = {
    CredentialVerifierUtils.verifyOAuth2AccessToken(bearerToken)
  }

  def verifyGuest(
    bearerToken: OAuth2AppOnlyToken,
    clientApp: ClientApplication,
    params: GuestAuthRequestParams
  ): AuthResultCode = {
    CredentialVerifierUtils.verifyOAuth2AppOnlyToken(bearerToken)
  }

  def verifyTia(
    userId: Long,
    signature: String,
    additionalFields: Seq[String],
    cryptoUtils: CryptoUtils
  ): AuthResultCode = {
    CredentialVerifierUtils.verifyPassbirdToken(
      userId,
      signature,
      additionalFields,
      cryptoUtils) match {
      case true => AuthResultCode.Ok
      case _ => AuthResultCode.BadSignature
    }
  }

  def verifyAmpEmail(
    ampEmailToken: OAuth2AccessToken
  ): AuthResultCode = {
    CredentialVerifierUtils.verifyOAuth2AccessToken(ampEmailToken)
  }
}
