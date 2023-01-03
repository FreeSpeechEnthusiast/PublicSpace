package com.twitter.auth.common

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.logging.Logger
import com.twitter.util.Return
import com.twitter.util.Throw
import com.twitter.util.Try
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.spec.PKCS8EncodedKeySpec
import java.security.Key
import java.security.KeyFactory
import java.util.Base64
import scala.io.Source

/**
 * Case class to parse OIDC key structure from json file to scala object
 */
case class OIDCPrivateKeyJSON(keyId: String, keySecret: String, enableForSigning: Boolean)

/**
 * Case class to store OIDC Private keys which can be used in services
 */
case class OIDCPrivateKey(keyId: String, privateKey: Key, enableForSigning: Boolean)

/**
 * Case class to store OIDC Private keys loader result
 */
case class OIDCPrivateKeys(
  validKeys: Seq[OIDCPrivateKey],
  enableForSigningKeys: Seq[OIDCPrivateKey],
  invalidSecrets: Map[String, Throwable])

/**
 * Parses a set of OIDC keys from a TSS file. Every key has a keyId (string), secret(string)
 * and enableForSigning(boolean) fields associated with it and all the keys must be in valid RS256 format.
 * The JSON entries that do not respect the expected format will be
 * returned in two separate Maps with failures. This is useful for debugging and testing.
 */
object OIDCPrivateKeyLoader {
  private val log = Logger.get(OIDCPrivateKeyLoader.getClass)
  private val BouncyCastleProvider = new BouncyCastleProvider
  private val RSAAlgo = "RSA"

  def apply(fileName: String, stats: StatsReceiver): OIDCPrivateKeys = {

    parseWithFailures(fileName).map { result =>
      log.info(s"Loaded OIDC Private keys - ${result.validKeys.size} from file - $fileName")

      if (result.invalidSecrets.nonEmpty) {
        log.error(s"Found invalid OIDC Private key secrets - ${result.invalidSecrets}")
        stats.counter("invalid_oidc_keys_secret").incr(result.invalidSecrets.size)
      }
      stats.counter("valid_oidc_private_keys").incr(result.validKeys.size)
      result
    } onFailure { th: Throwable =>
      log.error(s"Failed to load oidc private keys, file=$fileName, error=$th", th)
    } getOrElse OIDCPrivateKeys(Seq(), Seq(), Map.empty)
  }

  def loadKeyMap(fileName: String): Seq[OIDCPrivateKeyJSON] = {
    val fileSource = Source.fromFile(fileName)
    val jsonString = fileSource.mkString
    fileSource.close()
    JsonUtils.fromJson[Seq[OIDCPrivateKeyJSON]](jsonString)
  }

  def parseWithFailures(fileName: String): Try[OIDCPrivateKeys] = {
    Try(loadKeyMap(fileName)) map { oidcPrivateKeys =>
      val parsedKeys: Seq[(String, Try[Key], Boolean)] = oidcPrivateKeys map { k =>
        // kid -> private key
        (k.keyId, parseKeySecret(k.keySecret), k.enableForSigning)
      }
      val validKeys: Seq[OIDCPrivateKey] = parsedKeys.collect {
        case (keyId, Return(privateKey), enableForSigning) =>
          OIDCPrivateKey(keyId, privateKey, enableForSigning)
      }

      OIDCPrivateKeys(
        validKeys = validKeys,
        enableForSigningKeys = validKeys.filter(k => k.enableForSigning),
        invalidSecrets = parsedKeys.collect {
          case (keyId, Throw(th), _) => keyId -> th
        }.toMap
      )
    }
  }

  private[this] def parseKeySecret(keySecret: String): Try[Key] = {
    Try(readPrivateKey(keySecret))
  }

  @throws[Exception]
  def readPrivateKey(keySecret: String): Key = {
    val privateKeyPEM = keySecret
      .replace("-----BEGIN RSA PRIVATE KEY-----", "")
      .replaceAll(System.lineSeparator, "").replace("-----END RSA PRIVATE KEY-----", "")
    val encoded = Base64.getDecoder.decode(privateKeyPEM)
    val keyFactory = KeyFactory.getInstance(RSAAlgo, BouncyCastleProvider)
    val keySpec = new PKCS8EncodedKeySpec(encoded)
    keyFactory.generatePrivate(keySpec)
  }

}
