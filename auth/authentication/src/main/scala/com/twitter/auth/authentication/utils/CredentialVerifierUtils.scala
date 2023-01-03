package com.twitter.auth.authentication.utils

import com.twitter.auth.passportsigning.CryptoUtils
import com.twitter.joauth.Request
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode
import com.twitter.auth.authresultcode.thriftscala.AuthResultCode._
import com.twitter.joauth.NonceValidator
import com.twitter.joauth.Normalizer
import com.twitter.joauth.Signer
import com.twitter.joauth.UrlCodec
import com.twitter.auth.authentication.models.OAuth1RequestParams
import com.twitter.auth.models.OAuth1AccessToken
import com.twitter.auth.models.OAuth2AccessToken
import com.twitter.auth.models.OAuth2AppOnlyToken
import com.twitter.auth.models.OAuth2ClientAccessToken
import java.nio.charset.Charset
import java.util.Collections

object CredentialVerifierUtils {
  val AccessTokenNotFound: AuthResultCode = AuthResultCode.AccessTokenNotFound
  val OrphanedAccessToken: AuthResultCode = AuthResultCode.OrphanedAccessToken
  val BadClientKey: AuthResultCode = AuthResultCode.BadClientKey
  val BadAccessToken: AuthResultCode = AuthResultCode.BadAccessToken
  val BadClientAccessToken: AuthResultCode = AuthResultCode.BadClientAccessToken
  val DuplicateNonce: AuthResultCode = AuthResultCode.DuplicateNonce
  val UnknownAuthType: AuthResultCode = AuthResultCode.UnknownAuthType
  val BadSignature: AuthResultCode = AuthResultCode.BadSignature
  val TimeStampOutOfRange: AuthResultCode = AuthResultCode.TimestampOutOfRange
  val InternalServerError: AuthResultCode = AuthResultCode.InternalServerError
  val AuthSucceeds: AuthResultCode = AuthResultCode.Ok

  /**
   * This array is a lookup table that translates Unicode characters drawn from the "Base64 Alphabet" (as specified in
   * Table 1 of RFC 2045) into their 6-bit positive integer equivalents. Characters that are not in the Base64
   * alphabet but fall within the bounds of the array are translated to -1.
   *
   * Note: '+' and '-' both decode to 62. '/' and '_' both decode to 63. This means decoder seamlessly handles both
   * URL_SAFE and STANDARD base64. (The encoder, on the other hand, needs to know ahead of time what to emit).
   *
   * Thanks to "commons" project in ws.apache.org for this code.
   * http://svn.apache.org/repos/asf/webservices/commons/trunk/modules/util/
   */
  private val DECODE_TABLE = Array[Byte](-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, 62, -1, 62, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1,
    -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
    24, 25, -1, -1, -1, -1, 63, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
    42, 43, 44, 45, 46, 47, 48, 49, 50, 51)

  private val UTF_8 = Charset.forName("UTF-8")

  def verifyOAuth1AccessToken(accessToken: OAuth1AccessToken): AuthResultCode = {
    if (AccessTokenUtils.isOrphaned(accessToken)) {
      // orphaned token i.e. user not found
      OrphanedAccessToken
    } else if (AccessTokenUtils.isInvalid(accessToken)) {
      // invalid token
      BadAccessToken
    } else {
      Ok
    }
  }

  def verifyOAuth2AppOnlyToken(accessToken: OAuth2AppOnlyToken): AuthResultCode = {
    if (AccessTokenUtils.isInvalid(accessToken)) {
      // invalid token
      BadAccessToken
    } else {
      Ok
    }
  }

  def verifyOAuth2ClientAccessToken(clientAccessToken: OAuth2ClientAccessToken): AuthResultCode = {
    if (AccessTokenUtils.isInvalid(clientAccessToken)) {
      // invalid token
      BadClientAccessToken
    } else {
      Ok
    }
  }

  def verifyOAuth2AccessToken(accessToken: OAuth2AccessToken): AuthResultCode = {
    // TODO: Verify if Orphan token (e.g. user is still active). One way is to invoke Gizmoduck
    //  on every token authentication request. AUTHPLT-1068.
    if (AccessTokenUtils.isInvalid(accessToken)) {
      // invalid token
      BadAccessToken
    } else {
      Ok
    }
  }

  /***
   * verify OAuth signature of OAuth1 request
   */
  def validateSignature(
    normalizedRequest: String,
    signature: String,
    signatureMethod: String,
    tokenSecret: String,
    consumerSecret: String
  ): Boolean = {
    try {
      equals(
        UrlCodec.decode(signature).trim,
        new Signer.StandardSigner()
          .getBytes(normalizedRequest, signatureMethod, tokenSecret, consumerSecret))
    } catch {
      case _: Exception =>
        false
    }
  }

  /***
   * helper function for validating oauth signature
   */
  private[this] def equals(base64: String, bytes: Array[Byte]): Boolean = {
    val in = base64.getBytes(UTF_8)
    val length = in.length
    var eof = false
    var bitWorkArea = 0
    var modulus = 0
    var pos = 0
    var i = 0
    while ({
      i < length && !eof
    }) {
      val b = in(i)
      if (b == '=') eof = true
      else if (b >= 0 && b < DECODE_TABLE.length) {
        val result = DECODE_TABLE(b)
        if (result >= 0) {
          modulus = (modulus + 1) % AuthenticationConfig.BYTES_PER_ENCODED_BLOCK
          bitWorkArea = (bitWorkArea << AuthenticationConfig.BITS_PER_ENCODED_BYTE) + result
          if (modulus == 0) {
            if (bytes(pos) != ((bitWorkArea >> 16) & 0xff).toByte) return false
            pos += 1
            if (bytes(pos) != ((bitWorkArea >> 8) & 0xff).toByte) return false
            pos += 1
            if (bytes(pos) != (bitWorkArea & 0xff).toByte) return false
            pos += 1
          }
        }
      }
      i += 1
    }
    // Some may be left over at the end, we need to compare that as well
    if (eof && modulus != 0) modulus match {
      case 2 =>
        bitWorkArea = bitWorkArea >> 4
        if (bytes(pos) != (bitWorkArea & 0xff).toByte) return false
        pos += 1
      case 3 =>
        bitWorkArea = bitWorkArea >> 2
        if (bytes(pos) != ((bitWorkArea >> 8) & 0xff).toByte) return false
        pos += 1
        if (bytes(pos) != (bitWorkArea & 0xff).toByte) return false
        pos += 1
    }
    pos == bytes.length
  }

  /***
   * normalize an OAuth1 request
   * the normalized OAuth1 request will be used for computing oauth signature
   * @param oauth1RequestParams oauth1 request params to use
   *
   * @return
   */
  def normalizeRequest(oauth1RequestParams: OAuth1RequestParams): Option[String] = {
    if (oauth1RequestParams.scheme.isEmpty ||
      oauth1RequestParams.host.isEmpty ||
      oauth1RequestParams.port.isEmpty ||
      oauth1RequestParams.verb.isEmpty ||
      oauth1RequestParams.path.isEmpty) {
      None
    } else {
      val normalizedRequest: String = Normalizer.getStandardNormalizer.normalize(
        oauth1RequestParams.scheme.get.toUpperCase(),
        oauth1RequestParams.host.get,
        oauth1RequestParams.port.get,
        oauth1RequestParams.verb.get.toUpperCase(),
        oauth1RequestParams.path,
        oauth1RequestParams.otherParams.getOrElse(Collections.emptyList[Request.Pair]),
        oauth1RequestParams.oauthParams
      )
      Some(normalizedRequest)
    }
  }

  def verifyOAuth1Request(
    oAuth1RequestParams: OAuth1RequestParams,
    tokenSecret: String,
    consumerSecret: String,
    clientAppId: Long
  ): AuthResultCode = {
    verifyCredentials(oAuth1RequestParams, tokenSecret, consumerSecret) match {
      case BadSignature =>
        if (clientAppId == AuthenticationConfig.IOsID || clientAppId == AuthenticationConfig.TwitterForIphoneId) {
          verifyCredentials(oAuth1RequestParams, tokenSecret = "", consumerSecret) match {
            // If signature OK with empty string for IOs, then return UNKNOWN_AUTH_TYPE 400 which
            // gets mapped to TFE error code 215 "Bad Authentication data" so that the IOs app
            // will re-auth the client instead of failing when receiving 32 "generic auth error".
            case Ok => UnknownAuthType
            case _ => BadSignature
          }
        } else {
          BadSignature
        }
      case TimestampOutOfRange => TimestampOutOfRange
      case DuplicateNonce => DuplicateNonce
      case BadAccessToken => BadAccessToken
      case _ => Ok
    }
  }

  def verifyCredentials(
    oAuth1RequestParams: OAuth1RequestParams,
    tokenSecret: String,
    consumerSecret: String
  ): AuthResultCode = {
    val oAuth1Params = oAuth1RequestParams.oauthParams

    val normalizedRequest = CredentialVerifierUtils.normalizeRequest(oAuth1RequestParams)
    if (normalizedRequest.isEmpty) {
      BadAccessToken
    } else {
      if (!validateTimestampSecs(oAuth1Params.timestampSecs)) {
        //if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("bad timestamp -> %s", request.toString))
        TimeStampOutOfRange
      } else if (!NonceValidator.NO_OP_NONCE_VALIDATOR.validate(oAuth1Params.nonce)) {
        //if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("bad nonce -> %s", request.toString))
        DuplicateNonce
      } else if (!CredentialVerifierUtils.validateSignature(
          normalizedRequest.get,
          oAuth1Params.signature,
          oAuth1Params.signatureMethod,
          tokenSecret,
          consumerSecret)) {
        //if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("bad signature -> %s", request.toString))
        BadSignature
      } else {
        Ok
      }
    }
  }

  def validateTimestampSecs(timestampSecs: Long): Boolean = {
    val nowSecs = System.currentTimeMillis / 1000L
    (AuthenticationConfig.maxClockFloatBehindMins < 0 || (timestampSecs >= nowSecs - AuthenticationConfig.maxClockFloatBehindSecs)) &&
    (AuthenticationConfig.maxClockFloatAheadMins < 0 || (timestampSecs <= nowSecs + AuthenticationConfig.maxClockFloatAheadSecs))
  }

  def validateConsumerKey(
    clientConsumerKey: String,
    reqConsumerKey: String
  ): Boolean = {
    clientConsumerKey == reqConsumerKey
  }

  def verifyPassbirdToken(
    userId: Long,
    signature: String,
    additionalFields: Seq[String],
    cryptoUtils: CryptoUtils
  ): Boolean = {
    cryptoUtils.verifyHmacSignature(
      (Seq(userId.toString) ++ additionalFields).mkString("-"),
      signature
    )
  }
}
