package com.twitter.auth.utils

import com.twitter.appsec.crypto.Randomness
import com.twitter.util.Time
import java.security.MessageDigest
import java.util.Base64

object OAuth2CredentialType extends Enumeration {
  val AUTHORIZATION_CODE = Value("ac")
  val ACCESS_TOKEN = Value("at")
  val CLIENT_ACCESS_TOKEN = Value("ct")
  val REFRESH_TOKEN = Value("rt")
  val CLIENT_ID = Value("ci")
  val UNKNOWN = Value("unknown")

  def withNameWithDefault(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(UNKNOWN)
}

object TokenManagerService extends Enumeration {
  val FLIGHT_AUTH = Value("1")
  val PASSBIRD = Value("2")
  val UNKNOWN = Value("30")

  def withNameWithDefault(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(UNKNOWN)
}

object OAuth2CredentialUtils {

  val MISSING_VALID_TOKEN = "Missing valid token"

  // calculated the random string length to have 256 bit entropy based on the equation mentioned here
  // https://sourcegraph.twitter.biz/git.twitter.biz/source/-/blob/appsec/crypto-lib/src/main/scala/com/twitter/appsec/crypto/Randomness.scala#L36
  private val TOKEN_RANDOM_STRING_LENGTH = 45
  // token hash is made of <randomString>:secretId, this field indicates length of the string before
  // colon
  private val TOKEN_HASH_LENGTH_PREFIX = 43
  private val TOKEN_VERSION_V1 = "1"
  private val CLIENT_ID_VERSION_V1 = "1"
  //randomString:timestamp:formatVersion:secretId:typeOfToken:tokenManagerService
  private val ACCESS_TOKEN_PATTERN =
    s"[A-Za-z0-9\\_\\-]{$TOKEN_RANDOM_STRING_LENGTH}:[0-9]{13}:[1]:[0-9]{1,2}:at:[1]"
  //randomString:timestamp:formatVersion:secretId:typeOfToken:tokenManagerService
  private val CLIENT_ACCESS_TOKEN_PATTERN =
    s"[A-Za-z0-9\\_\\-]{$TOKEN_RANDOM_STRING_LENGTH}:[0-9]{13}:[1]:[0-9]{1,2}:ct:[1]"
  //randomString:timestamp:formatVersion:secretId:typeOfToken:tokenManagerService)
  private val REFRESH_TOKEN_PATTERN =
    s"[A-Za-z0-9\\_\\-]{$TOKEN_RANDOM_STRING_LENGTH}:[0-9]{13}:[1]:[0-9]{1,2}:rt:[1]"
  //randomString:timestamp:formatVersion:secretId:typeOfToken:tokenManagerService
  private val AUTHORIZATION_CODE_PATTERN =
    s"[A-Za-z0-9\\_\\-]{$TOKEN_RANDOM_STRING_LENGTH}:[0-9]{13}:[1]:[0-9]{1,2}:ac:[1]"
  //randomString:timestamp:formatVersion:secretId:(at|rt|ac):tokenManagerService
  private val TOKEN_PATTERN =
    s"([A-Za-z0-9\\_\\-]{$TOKEN_RANDOM_STRING_LENGTH}:[0-9]{13}:[1]:[0-9]{1,2}:(at|rt|ac|ct):)([1])".r
  private val TOKEN_HASH_PATTERN =
    //randomString:secretId:(at|rt)
    s"(.{$TOKEN_HASH_LENGTH_PREFIX}:([0-9]{1,2}):(?:at|rt|ct))".r.regex
  //randomString:timestamp:formatVersion:secretId:(at|rt|ac):1
  private val FLIGHT_AUTH_PATTERN =
    s"([A-Za-z0-9\\_\\-]{$TOKEN_RANDOM_STRING_LENGTH}:[0-9]{13}:[1]:[0-9]{1,2}:(?:at|rt|ac|ct):)([1])".r
  private val TOKEN_SECRET_ID_EXTRACT_PATTERN =
    s"([A-Za-z0-9\\_\\-]{$TOKEN_RANDOM_STRING_LENGTH}:[0-9]{13}:[1]:([0-9]{1,2}):(?:at|rt|ac|ct):)([1])".r
  private val TOKEN_HASH_SECRET_ID_EXTRACT_PATTERN =
    s"(.{$TOKEN_HASH_LENGTH_PREFIX}:([0-9]{1,2}):(at|rt|ct))".r

  private val CLIENT_ID_STRING_LENGTH = 20
  private val APP_SECRET_STRING_LENGTH = 50
  //randomString:formatVersion:typeOfToken
  private val OAUTH2_CLIENT_ID_PATTERN =
    s"[A-Za-z0-9\\_\\-]{$CLIENT_ID_STRING_LENGTH}:[1]:ci".r

  private val BASE64_ENCODER = Base64.getUrlEncoder.withoutPadding
  private val BASE64_DECODER = Base64.getUrlDecoder

  def createOauth2ClientId(): String = {
    //randomString:formatVersion:typeOfToken
    BASE64_ENCODER.encodeToString(
      String
        .join(
          ":",
          Randomness.string(CLIENT_ID_STRING_LENGTH),
          CLIENT_ID_VERSION_V1,
          OAuth2CredentialType.CLIENT_ID.toString()
        ).getBytes)
  }

  def createOauth2AppSecret(): String = {
    Randomness.string(APP_SECRET_STRING_LENGTH)
  }

  def isValidOauth2ClientId(encodedClientId: String): Boolean = {
    try {
      // OAuth2 client id
      val decoded = new String(BASE64_DECODER.decode(encodedClientId))
      decoded.matches(OAUTH2_CLIENT_ID_PATTERN.regex) &&
      !encodedClientId.exists { ch =>
        Character.isWhitespace(ch) || Character.isISOControl(ch)
      }
    } catch {
      // We do expect it to blow up on non OAuth2 user tokens (e.g. App-Only)
      case _: Exception => false
    }
  }

  def isFlightAuthManagedToken(encodedToken: String): Boolean = {
    try {
      val decoded = new String(BASE64_DECODER.decode(encodedToken))
      if (decoded.matches(FLIGHT_AUTH_PATTERN.regex)) {
        val FLIGHT_AUTH_PATTERN(_, manager) = decoded
        TokenManagerService.withNameWithDefault(manager) match {
          case TokenManagerService.FLIGHT_AUTH => true
          case _ => false
        }
      } else {
        false
      }
    } catch {
      // We do expect it to blow up on non OAuth2 user tokens (e.g. App-Only)
      case _: Exception => false
    }
  }

  def isFlightAuthManagedHashToken(tokenHash: String): Boolean = {
    tokenHash.matches(TOKEN_HASH_PATTERN)
  }

  def decodeTokenType(encodedToken: String): OAuth2CredentialType.Value = {
    try {
      val decoded = new String(BASE64_DECODER.decode(encodedToken))
      if (decoded.matches(TOKEN_PATTERN.regex)) {
        val TOKEN_PATTERN(_, tokenTypeString, _) = decoded
        tokenTypeString match {
          case "at" => OAuth2CredentialType.ACCESS_TOKEN
          case "ct" => OAuth2CredentialType.CLIENT_ACCESS_TOKEN
          case "rt" => OAuth2CredentialType.REFRESH_TOKEN
          case "ac" => OAuth2CredentialType.AUTHORIZATION_CODE
          case _ => OAuth2CredentialType.UNKNOWN
        }
      } else {
        OAuth2CredentialType.UNKNOWN
      }
    } catch {
      // If it blows up send unknown
      case _: Exception => OAuth2CredentialType.UNKNOWN
    }
  }

  def decodeSecretId(decodedToken: String): Option[Int] = {
    if (decodedToken.matches(TOKEN_PATTERN.regex)) {
      val TOKEN_SECRET_ID_EXTRACT_PATTERN(_, secretId, _) = decodedToken
      Some(secretId.toInt)
    } else {
      None
    }
  }

  def decodeSecretIdFromTokenHash(tokenHash: String): Option[Int] = {
    if (tokenHash.matches(TOKEN_HASH_PATTERN)) {
      val TOKEN_HASH_SECRET_ID_EXTRACT_PATTERN(_, secretId, _) = tokenHash
      Some(secretId.toInt)
    } else {
      None
    }
  }

  def decodeTokenTypeFromTokenHash(tokenHash: String): Option[OAuth2CredentialType.Value] = {
    if (tokenHash.matches(TOKEN_HASH_PATTERN)) {
      val TOKEN_HASH_SECRET_ID_EXTRACT_PATTERN(_, _, tokenType) = tokenHash
      Some(OAuth2CredentialType.withNameWithDefault(tokenType))
    } else {
      None
    }
  }

  def createAuthorizationCodeString(secretId: Int): String = {
    createBase64EncodedString(
      randomStringLength = TOKEN_RANDOM_STRING_LENGTH,
      secretId = secretId,
      tokenType = OAuth2CredentialType.AUTHORIZATION_CODE,
      tokenManagerService = TokenManagerService.FLIGHT_AUTH,
      tokenVersion = TOKEN_VERSION_V1
    )
  }

  def createRefreshToken(secretId: Int): String = {
    //randomString:timestamp:formatVersion:secretId:typeOfToken:tokenManagerService
    createBase64EncodedString(
      randomStringLength = TOKEN_RANDOM_STRING_LENGTH,
      secretId = secretId,
      tokenType = OAuth2CredentialType.REFRESH_TOKEN,
      tokenManagerService = TokenManagerService.FLIGHT_AUTH,
      tokenVersion = TOKEN_VERSION_V1
    )
  }

  def createAccessToken(secretId: Int): String = {
    //randomString:timestamp:formatVersion:secretId:typeOfToken:tokenManagerService
    createBase64EncodedString(
      randomStringLength = TOKEN_RANDOM_STRING_LENGTH,
      secretId = secretId,
      tokenType = OAuth2CredentialType.ACCESS_TOKEN,
      tokenManagerService = TokenManagerService.FLIGHT_AUTH,
      tokenVersion = TOKEN_VERSION_V1
    )
  }
  def createClientAccessToken(secretId: Int): String = {
    //randomString:timestamp:formatVersion:secretId:typeOfToken:tokenManagerService
    createBase64EncodedString(
      randomStringLength = TOKEN_RANDOM_STRING_LENGTH,
      secretId = secretId,
      tokenType = OAuth2CredentialType.CLIENT_ACCESS_TOKEN,
      tokenManagerService = TokenManagerService.FLIGHT_AUTH,
      tokenVersion = TOKEN_VERSION_V1
    )
  }

  def createAmpEmailToken(secretId: Int): String = {
    //randomString:timestamp:formatVersion:secretId:typeOfToken:tokenManagerService
    createBase64EncodedString(
      randomStringLength = TOKEN_RANDOM_STRING_LENGTH,
      secretId = secretId,
      /** tokenType ACCESS_TOKEN is fine because amp email tokens are stored in dedicated DB/Cache  */
      tokenType = OAuth2CredentialType.ACCESS_TOKEN,
      tokenManagerService = TokenManagerService.FLIGHT_AUTH,
      tokenVersion = TOKEN_VERSION_V1
    )
  }

  def hashKey(
    key: String,
    isHashed: Boolean = false,
    secretId: Int,
    tokenType: OAuth2CredentialType.Value
  ): String = {
    if (isHashed) {
      key
    } else {
      hash(key) + ":" + secretId + ":" + tokenType
    }
  }

  def hash(toHash: String): String = {
    val Utf8Charset = java.nio.charset.StandardCharsets.UTF_8
    val hasher = MessageDigest.getInstance("SHA-256")
    BASE64_ENCODER.encodeToString(hasher.digest(toHash.getBytes(Utf8Charset)))
  }

  def createTokenHashFromToken(encodedToken: String): Option[String] = {
    if (isFlightAuthManagedToken(encodedToken)) {
      val plainKey = new String(BASE64_DECODER.decode(encodedToken))
      val tokenType = decodeTokenType(encodedToken)
      decodeSecretId(plainKey) match {
        case Some(secretId) =>
          Some(hashKey(key = encodedToken, secretId = secretId, tokenType = tokenType))
        case _ =>
          None
      }
    } else {
      None
    }
  }

  private def createBase64EncodedString(
    randomStringLength: Int,
    secretId: Int,
    tokenType: OAuth2CredentialType.Value,
    tokenManagerService: TokenManagerService.Value,
    tokenVersion: String
  ): String = {
    BASE64_ENCODER.encodeToString(
      String
        .join(
          ":",
          Randomness.string(randomStringLength),
          Time.now.inMilliseconds.toString,
          tokenVersion,
          secretId.toString,
          tokenType.toString,
          tokenManagerService.toString
        ).getBytes)
  }

}
