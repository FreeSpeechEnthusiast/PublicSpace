package com.twitter.auth.common

import com.twitter.finagle.stats.{InMemoryStatsReceiver, NullStatsReceiver}
import com.twitter.util.Throw
import java.io.{BufferedWriter, File, FileWriter}
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class EncryptionKeysLoaderSpec
    extends AnyFunSuite
    with Matchers
    with MockitoSugar
    with BeforeAndAfter {
  val expBits = 256
  var file: File = _
  val validKeyId = 1.toString
  val validKeySecret = "C94A324B7221AA8A8760DA0717C80256EF4308EC6068B7144AA3BBA4A5F98007"
  val invalidKeyId = "invalidKeyId"
  val invalidKeySecret = "invalidSecret"

  before {
    file = File.createTempFile("tmpEncryptionKeys", ".json")
  }

  after {
    if (file.exists)
      file.delete
  }

  test("load keys should fail when file does not exist") {
    EncryptionKeysLoader.parseWithFailures("invalidFilePath", expBits) must matchPattern {
      case Throw(_: java.io.FileNotFoundException) =>
    }
  }

  test("load keys should fail when file is empty") {
    EncryptionKeysLoader.parseWithFailures("invalidFilePath", expBits) must matchPattern {
      case Throw(_: Exception) =>
    }
  }

  test("parseWithFailure should track invalid key secrets with not the expected length") {
    val validKeyId = 1.toString
    writeJSONToFile(validKeyId, invalidKeySecret)
    val res = EncryptionKeysLoader.parseWithFailures(file.getPath, expBits)
    res mustBe ('return)
    res.apply().invalidSecrets must contain key validKeyId.toInt
    res.apply().validKeys must not contain key(validKeyId.toInt)
    res.apply().encryptionEnabledKeys mustNot contain key (validKeyId.toInt)
  }

  test("parseWithFailure should track invalid key Ids") {
    writeJSONToFile(invalidKeyId, validKeySecret)
    val res = EncryptionKeysLoader.parseWithFailures(file.getPath, expBits)
    res must be('return)
    res.apply().invalidKeyIds must contain key "invalidKeyId"
  }

  test("parseWithFailure should parse the key and secret is valid") {
    writeJSONToFile(validKeyId, validKeySecret)
    val res = EncryptionKeysLoader.parseWithFailures(file.getPath, expBits)
    res must be('return)
  }

  test("apply should return an empty map when the encryption file doesn't exist") {
    EncryptionKeysLoader("nonExistentFile", expBits, NullStatsReceiver) must be
    EncryptionKeys(Map.empty, Map.empty, Map.empty, Map.empty)
  }

  test("apply should count invalid key Ids") {
    writeJSONToFile(invalidKeyId, validKeySecret)
    val stats = new InMemoryStatsReceiver
    EncryptionKeysLoader(file.getPath, expBits, stats)
    stats.counters(Seq("invalid_encryption_key_ids")) must equal(1)
  }

  test("apply should count invalid key secret") {
    writeJSONToFile(validKeyId, invalidKeySecret)
    val stats = new InMemoryStatsReceiver
    EncryptionKeysLoader(file.getPath, expBits, stats)
    stats.counters(Seq("invalid_encryption_key_secrets")) must equal(1)
  }

  test("apply should return keys") {
    writeJSONToFile(validKeyId, validKeySecret)
    val res = EncryptionKeysLoader.parseWithFailures(file.getPath, expBits)
    res must be('return)
    res.apply().validKeys must contain key validKeyId.toInt
    res.apply().encryptionEnabledKeys must contain key validKeyId.toInt
    res.apply().invalidKeyIds.size mustBe 0
    res.apply().invalidSecrets.size mustBe 0
  }

  test("apply should filter encryption not enabled keys from the list") {
    writeJSONToFile(validKeyId, validKeySecret, false)
    val res = EncryptionKeysLoader.parseWithFailures(file.getPath, expBits)
    res mustBe 'return
    res.apply().validKeys must contain key validKeyId.toInt
    res.apply().encryptionEnabledKeys.size mustBe 0
    res.apply().invalidKeyIds.size mustBe 0
    res.apply().invalidSecrets.size mustBe 0
  }

  private def writeJSONToFile(
    keyId: String,
    keySecret: String,
    enableForEncryption: Boolean = true
  ): Unit = {
    val line =
      "[{\"keyId\": \"" + keyId + "\",\"keySecret\": \"" + keySecret + "\",\"enableForEncryption\": \"" + enableForEncryption + "\"}]"
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(line)
    bw.close()
  }
}
