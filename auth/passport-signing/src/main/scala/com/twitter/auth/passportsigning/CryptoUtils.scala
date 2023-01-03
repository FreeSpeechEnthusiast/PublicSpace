package com.twitter.auth.passportsigning

import com.google.common.base.Charsets
import com.twitter.appsec.crypto._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.logging.Logger
import com.twitter.util.Base64StringEncoder
import com.twitter.util.Duration
import com.twitter.util.Time
import com.twitter.util.Try
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security._
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import org.apache.commons.codec.binary.Base32
import scala.annotation.tailrec

object CryptoUtils {
  val MaxPossibleOfflineCodeIterations = 5000
  private val SignatureAlgorithm = "SHA1withRSA"
  private val SignatureKeyAlgorithm = "RSA"
  private val HashAlgorithm = "SHA-256"
  private val Random = new SecureRandom
  private val Chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
  private val HexChars = ('0' to '9') ++ ('a' to 'f')

  private val Base32Chars = ('a' to 'z') ++ ('2' to '7')
  // No L, O, 0, or 1. Leaving I, since we'll lower-case and i is distinctive
  val ReadableBase32Chars = ('a' to 'k') ++ ('m' to 'n') ++ ('p' to 'z') ++ ('2' to '9')
  private val Digits = ('0' to '9')
  private val Log = Logger.get(getClass.getName)
  val PasetoPrivateKeyLengthInBits = 512

  /**
   * For use with TOTP for 2FA. Each call creates a new HMAC object initialized w/ the
   * unique client key. NOTE: Key is case sensitive -- should be upper case.
   */
  def totpHmacSignature(key: String, message: Long): Array[Byte] = {
    val byteBuff = ByteBuffer.allocate(8)
    val base32DecodedKey = new Base32().decode(key.toUpperCase)
    HmacSha1(base32DecodedKey, byteBuff.putLong(0, message).array).bytes
  }

  def byteArrayToInt(byteArray: Array[Byte]): Long = {
    ByteBuffer.wrap(byteArray).getInt
  }

}

class CryptoUtils(
  hmacSecretKeyBase64: String,
  val statsReceiver: StatsReceiver) {
  import CryptoUtils._

  private[this] val decodedHmacKey = Base64StringEncoder.decode(hmacSecretKeyBase64)
  private[this] val defaultThreadLocalHmac = new ThreadLocal[Hmac] {
    override def initialValue(): Hmac = HmacSha256(decodedHmacKey)
  }

  def hmacSignature(
    message: String,
    charSet: Charset = Charsets.UTF_8,
    forUrl: Boolean = false
  ): String = {
    val byteArray = defaultThreadLocalHmac.get.apply(message.getBytes(charSet))
    if (forUrl) {
      byteArray.asUrlEncodedBase64
    } else {
      byteArray.asBase64
    }
  }

  /**
   * This creates a signature that expires after a duration. To do this, it
   * appends the current timestamp integer divided by the duration, here called the
   * period. That period will change after the expiration time.
   */
  def hmacSignatureWithExpiration(
    message: String,
    expiration: Duration,
    forUrl: Boolean = false
  ): String = {
    val messageWithPeriod = message + "-" + Time.now.inMillis / expiration.inMillis
    hmacSignature(messageWithPeriod, forUrl = forUrl)
  }

  private[this] def previousHmacSignatureWithExpiration(
    message: String,
    expiration: Duration,
    forUrl: Boolean = false
  ): String = {
    // This is used to check if the signature was created in the last period.
    val messageWithPreviousPeriod = message + "-" + (Time.now.inMillis / expiration.inMillis - 1)
    hmacSignature(messageWithPreviousPeriod, forUrl = forUrl)
  }

  def verifyHmacSignature(
    message: String,
    expectedSignature: String,
    forUrl: Boolean = false
  ): Boolean = {
    expectedSignature == hmacSignature(message, forUrl = forUrl)
  }

  /**
   * We check for the current window, and the previous window. Thus, the actual
   * expiration is between 1-2x the specified expiration.
   */
  def verifyHmacSignatureWithExpiration(
    message: String,
    expectedSignature: String,
    expiration: Duration,
    checkFullSignature: Boolean = true,
    forUrl: Boolean = false
  ): Boolean = {
    Seq(
      hmacSignatureWithExpiration(message, expiration, forUrl = forUrl),
      previousHmacSignatureWithExpiration(message, expiration, forUrl = forUrl)
    ).exists { signature: String =>
      if (checkFullSignature) {
        expectedSignature == signature
      } else {
        expectedSignature.length > 0 &&
        expectedSignature == signature.take(expectedSignature.length)
      }
    }
  }

  def randomAlphanumericString(length: Int): String = {
    randomString(Chars, length)
  }

  def randomHexString(length: Int): String = {
    randomString(HexChars, length)
  }

  def randomDigitString(length: Int): String = {
    randomString(Digits, length)
  }

  def randomReadableBase32String(length: Int): String = {
    randomString(ReadableBase32Chars, length)
  }

  def randomInt(): Int = {
    Random.nextInt()
  }

  def randomLong(): Long = {
    Random.nextLong()
  }

  def randomByteArray(length: Int): Array[Byte] = {
    Randomness.bytes(length)
  }

  private def randomString(chars: Seq[Char], length: Int): String = {
    val sb = new StringBuilder(length)
    for (_ <- 1 to length) {
      sb.append(chars(randomInt(chars.length)))
    }
    sb.toString
  }

  private def randomInt(range: Int): Int = {
    Random.nextInt(range)
  }

  def hash(value: Array[Byte]): Array[Byte] = {
    val md = MessageDigest.getInstance(HashAlgorithm)
    md.digest(value)
  }

  def sign(
    challengeBytes: Array[Byte],
    privateKeyBytes: Array[Byte]
  ): Array[Byte] = {
    try {
      val signer = Signature.getInstance(SignatureAlgorithm)
      val pkcs8keySpec = new PKCS8EncodedKeySpec(privateKeyBytes)
      val privateKey = KeyFactory.getInstance(SignatureKeyAlgorithm).generatePrivate(pkcs8keySpec)
      signer.initSign(privateKey)
      signer.update(challengeBytes)
      signer.sign()
    } catch {
      case t: Throwable =>
        Log.debug("failed signing with exception: %s", t.getMessage)
        Array.empty
    }
  }

  def verifyChallengeResponse(
    challengeBytes: Array[Byte],
    challengeResponseBytes: Array[Byte],
    publicKeyBytes: Array[Byte]
  ): Boolean = {
    try {
      val signer = Signature.getInstance(SignatureAlgorithm)
      val x509keySpec = new X509EncodedKeySpec(publicKeyBytes)
      val publicKey = KeyFactory.getInstance(SignatureKeyAlgorithm).generatePublic(x509keySpec)
      signer.initVerify(publicKey)
      signer.update(challengeBytes)
      signer.verify(challengeResponseBytes)
    } catch {
      case t: Throwable =>
        Log.debug("failed verifying challenge response with exception: %s", t.getMessage)
        false
    }
  }

  // Base32 should technically always be upper case. Google Authenticator seems to
  // convert any entered key string to upper case, so it should be safe to surface the
  // key to users as lowercase. However, caveat emptor that not all 3rd party
  // authenticators necessarily operate in this way. encodeAsString() naturally returns
  // an upper case string.
  //
  // TODO ACCTSEC-4419: This should probably be converted to a readable base 32 string,
  // but it seems that we sometimes lose bytes in the process of the conversion, which
  // results in an invalid key. Need to somehow do the conversion in a way that ensures
  // that we retain the same number of bytes.
  def convertBytesToBase32SharedKeyString(randomBytes: Array[Byte]): String = {
    new Base32().encodeAsString(randomBytes)
  }

  def convertBytesToOfflineCode(offlineCodeBytes: Array[Byte]): String = {
    // We take 12 bytes here to get an easily type-able code
    base32ToReadable(new Base32().encodeAsString(offlineCodeBytes).toLowerCase.substring(0, 12))
  }

  def convertOfflineCodeToBytes(offlineCode: String): Array[Byte] = {
    try {
      // We have to add "A" to get it over 64 bits, so we get 8 bytes back, then we strip the last 4
      val offlineCodeBytes =
        new Base32().decode(readableToBase32(offlineCode.toLowerCase).toUpperCase + "A")
      offlineCodeBytes.slice(0, 7) :+ (offlineCodeBytes(7) & 0xf0).toByte
    } catch {
      case t: IndexOutOfBoundsException => Array.empty[Byte]
    }
  }

  def hashAndTruncate(value: Array[Byte]): Array[Byte] = {
    val hashedValue = hash(value)
    if (hashedValue.length < 8) throw new RuntimeException("Error calculating hash value")
    // We need 60 bits here, so we mask the last four
    hashedValue.slice(0, 7) :+ (hashedValue(7) & 0xf0).toByte
  }

  def generateOfflineCode(
    seed: Array[Byte],
    count: Int
  ): String = {
    val bytes = generateOfflineCodeBytes(seed, count)
    convertBytesToOfflineCode(bytes)
  }

  @tailrec
  private def generateOfflineCodeBytes(seed: Array[Byte], count: Int): Array[Byte] = {
    if (count <= 0) {
      seed
    } else {
      val nextOfflineCode = hashAndTruncate(seed)
      generateOfflineCodeBytes(nextOfflineCode, count - 1)
    }
  }

  /**
   * The function verifyOfflineCode determines if the offline code provided is valid by hashing
   * it for several rounds to see if it matches the last seen offline code.
   *
   * @return A (Boolean, Int) tuple representing whether the code was valid, and how many
   *         iterations it took to find the code
   */
  def verifyOfflineCode(
    offlineCode: String,
    lastSeenOfflineCode: Array[Byte]
  ): (Boolean, Int) = {
    val offlineCodeBytes = convertOfflineCodeToBytes(offlineCode)
    if (offlineCodeBytes.isEmpty) {
      (false, 0)
    } else {
      verifyOfflineCodeBytes(offlineCodeBytes, lastSeenOfflineCode)
    }
  }

  private def verifyOfflineCodeBytes(
    offlineCode: Array[Byte],
    lastSeenOfflineCode: Array[Byte]
  ): (Boolean, Int) = {
    @tailrec
    def verifyOfflineCodeInner(
      offlineCode: Array[Byte],
      lastSeenOfflineCode: Array[Byte],
      iterations: Int
    ): (Boolean, Int) = {
      val nextOfflineCode = hashAndTruncate(offlineCode)
      if (iterations == MaxPossibleOfflineCodeIterations) {
        (false, 0)
      } else if (nextOfflineCode.deep == lastSeenOfflineCode.deep) {
        (true, iterations)
      } else {
        verifyOfflineCodeInner(nextOfflineCode, lastSeenOfflineCode, iterations + 1)
      }
    }
    verifyOfflineCodeInner(offlineCode, lastSeenOfflineCode, 1)
  }

  def base32ToReadable(base32String: String): String = {
    base32String map { c =>
      ReadableBase32Chars(Base32Chars.indexOf(c))
    }
  }

  def readableToBase32(readableString: String): String = {
    readableString map { c =>
      Base32Chars(ReadableBase32Chars.indexOf(c))
    }
  }

  def createClientToken(userId: Long, clientApplicationId: Long): String = {
    createClientToken(userId, clientApplicationId, nonce = randomHexString(6))
  }

  private def createClientToken(userId: Long, clientApplicationId: Long, nonce: String): String = {
    val clientAppIdStr = clientApplicationId.toString

    Seq(
      clientAppIdStr,
      nonce,
      hmacSignature(clientAppIdStr + userId.toString + nonce)
    ).mkString("-")
  }

  def getClientAppIdFromToken(userId: Long, token: String): Option[Long] = {
    token.split("-") match {
      case Array(clientAppIdStr, nonce, signature) =>
        for {
          clientAppId <- Try(clientAppIdStr.toLong).toOption
          if token == createClientToken(userId, clientAppId, nonce)
        } yield clientAppId
      case _ => None
    }
  }

  // Implementation borrowed from macaw-login
  def constantTimeEquals(a: String, b: String): Boolean = {
    MessageDigest.isEqual(a.getBytes(Charsets.UTF_8), b.getBytes(Charsets.UTF_8))
  }
}
