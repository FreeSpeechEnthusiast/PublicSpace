package com.twitter.auth.common

import com.twitter.appsec.crypto.Conversions
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.logging.Logger
import com.twitter.util.{Return, Throw, Try}
import org.bouncycastle.crypto.params.KeyParameter
import scala.io.Source

/**
 * Case class to parse encryption key structure from json file to scala object
 */
case class EncryptionKey(keyId: String, keySecret: String, enableForEncryption: Boolean)

/**
 * Case class to store encryption keys loader result
 */
case class EncryptionKeys(
  validKeys: Map[Int, KeyParameter],
  encryptionEnabledKeys: Map[Int, KeyParameter],
  invalidKeyIds: Map[String, Throwable],
  invalidSecrets: Map[Int, Throwable])

/**
 * Parses a set of Aes keys from a TSS file. Every key has a version number (string), secret(string)
 * and enableForEncryption(boolean) fields associated with it and all the keys must be of an
 * expected length in bits. The JSON entries that do not respect the expected format will be
 * returned in two separate Maps with failures. This is useful for debugging and testing.
 */
object EncryptionKeysLoader {
  private val log = Logger.get(EncryptionKeysLoader.getClass)

  def apply(
    fileName: String,
    expectedKeyBits: Int,
    stats: StatsReceiver
  ): EncryptionKeys = {
    parseWithFailures(fileName, expectedKeyBits).map { result =>
      log.info(s"Loaded encryption keys - ${result.validKeys.size} from file - $fileName")
      if (result.invalidKeyIds.nonEmpty) {
        log.error(s"Found invalid encryption keys - ${result.invalidKeyIds}")
        stats.counter("invalid_encryption_key_ids").incr(result.invalidKeyIds.size)
      }
      if (result.invalidSecrets.nonEmpty) {
        log.error(s"Found invalid encryption secrets - ${result.invalidSecrets}")
        stats.counter("invalid_encryption_key_secrets").incr(result.invalidSecrets.size)
      }
      stats.counter("valid_encryption_keys").incr(result.validKeys.size)
      result
    } onFailure { th: Throwable =>
      log.error(s"Failed to load encryption keys, file=$fileName, error=$th", th)
    } getOrElse EncryptionKeys(Map.empty, Map.empty, Map.empty, Map.empty)
  }

  def loadKeyMap(fileName: String): Seq[EncryptionKey] = {
    val fileSource = Source.fromFile(fileName)
    val jsonString = fileSource.mkString
    fileSource.close()
    JsonUtils.fromJson[Seq[EncryptionKey]](jsonString)
  }

  def parseWithFailures(fileName: String, expectedKeyBits: Int): Try[EncryptionKeys] = {
    Try(loadKeyMap(fileName)) map { encryptionKeys =>
      val keyIdToSecretMap = encryptionKeys.map(k => k.keyId -> k.keySecret).toMap
      val encryptionEnabledKeyIds =
        encryptionKeys.filter(k => k.enableForEncryption).map(k => k.keyId).toList
      val parsedKeyIds = encryptionKeys.map { k =>
        k.keyId -> parseKeyId(k.keyId)
      }.toMap
      val invalidKeyIds = parsedKeyIds collect {
        case (key, Throw(th)) => key -> th
      }
      val parsedKeys = parsedKeyIds collect {
        case (keyIdStr, Return(keyId)) =>
          keyId -> parseKeySecret(keyIdToSecretMap(keyIdStr), expectedKeyBits)
      }
      val validKeys = parsedKeys.collect {
        case (keyId, Return(keySecret)) => keyId -> keySecret
      }

      EncryptionKeys(
        validKeys = validKeys,
        encryptionEnabledKeys =
          validKeys.filter(k => encryptionEnabledKeyIds.contains(k._1.toString)),
        invalidKeyIds = invalidKeyIds,
        invalidSecrets = parsedKeys.collect {
          case (keyId, Throw(th)) => keyId -> th
        }
      )
    }
  }

  private[this] def parseKeyId(key: String): Try[Int] = Try {
    key.toInt match {
      case value: Int => value
    }
  }

  private[this] def parseKeySecret(keySecret: String, expBits: Int): Try[KeyParameter] =
    for {
      secretBytes <- Try(new Conversions.EnhancedString(keySecret).fromHex)
      if secretBytes.length * 8 == expBits
    } yield new KeyParameter(secretBytes)
}
