package com.twitter.auth.common

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.util.Throw
import com.twitter.util.Try
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

@RunWith(classOf[JUnitRunner])
class OIDCRSAPrivateKeysLoaderSpec
    extends AnyFunSuite
    with Matchers
    with MockitoSugar
    with BeforeAndAfter {
  private var file: File = _
  private val validKeyId = "1"
  private val validKeySecret =
    "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"
  private val invalidKeySecret = "invalidSecret"
  private val validFileName = "tempOIDCFile"

  before {
    file = File.createTempFile(validFileName, ".json")
  }

  after {
    if (file.exists)
      file.delete
  }

  test("load keys should fail when file does not exist") {
    OIDCPrivateKeyLoader.parseWithFailures("invalidFilePath") must matchPattern {
      case Throw(_: java.io.FileNotFoundException) =>
    }
  }

  test("load keys should fail when file is empty") {
    OIDCPrivateKeyLoader.parseWithFailures(validFileName) must matchPattern {
      case Throw(_: Exception) =>
    }
  }

  test("parseWithFailure should track invalid key secrets") {
    writeJSONToFile(keyId = validKeyId, keySecret = invalidKeySecret, enableForSigning = true)
    val res = OIDCPrivateKeyLoader.parseWithFailures(file.getPath)
    res mustBe ('return)
    res.apply().invalidSecrets must contain key validKeyId
    res.apply().enableForSigningKeys.count(t => t.keyId == validKeyId) == 0
  }

  test("apply should return an empty map when the  file doesn't exist") {
    OIDCPrivateKeyLoader("nonExistentFile", NullStatsReceiver) must be
    OIDCPrivateKeys(Seq(), Seq(), Map.empty)
  }

  test("apply should count invalid key secret") {
    writeJSONToFile(keyId = validKeyId, keySecret = invalidKeySecret, enableForSigning = true)
    val stats = new InMemoryStatsReceiver
    OIDCPrivateKeyLoader(file.getPath, stats)
    stats.counters(Seq("invalid_oidc_keys_secret")) must equal(1)
  }

  test("apply should return keys") {
    writeJSONToFile(keyId = validKeyId, keySecret = validKeySecret, enableForSigning = true)
    val stats = new InMemoryStatsReceiver
    val OIDCPrivateKeys = OIDCPrivateKeyLoader.apply(file.getPath, stats)
    OIDCPrivateKeys.validKeys.size mustBe 1
    OIDCPrivateKeys.validKeys.head.enableForSigning mustBe true
    OIDCPrivateKeys.invalidSecrets.size mustBe 0
    stats.counters(Seq("valid_oidc_private_keys")) must equal(1)
  }

  test("parseWithFailures successfully returns keys") {
    writeJSONToFile(keyId = validKeyId, keySecret = validKeySecret, enableForSigning = true)
    val res: Try[OIDCPrivateKeys] = OIDCPrivateKeyLoader.parseWithFailures(file.getPath)
    res must be('return)
    res.apply().validKeys.size mustBe 1
    res.apply().validKeys.head.keyId mustBe validKeyId
    res.apply().validKeys.head.enableForSigning mustBe true
    res.apply().invalidSecrets.size mustBe 0
  }

  test("apply should filter not enabled signing keys from the list") {
    writeJSONToFile(keyId = validKeyId, keySecret = validKeySecret, enableForSigning = false)
    val res: Try[OIDCPrivateKeys] = OIDCPrivateKeyLoader.parseWithFailures(file.getPath)
    res must be('return)
    res.apply().validKeys.size mustBe 1
    res.apply().enableForSigningKeys.size mustBe 0
    res.apply().invalidSecrets.size mustBe 0
  }

  private def writeJSONToFile(
    keyId: String,
    keySecret: String,
    enableForSigning: Boolean
  ): Unit = {
    val line =
      "[{\"keyId\": \"" + keyId + "\",\"keySecret\": \"" + keySecret + "\",\"enableForSigning\": \"" + enableForSigning + "\"}]"
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(line)
    bw.close()
  }
}
