package com.twitter.auth.pasetoheaders.passport

import com.twitter.auth.pasetoheaders.finagle.ConfigBusSource
import com.twitter.auth.pasetoheaders.finagle.ServiceBootException
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.auth.pasetoheaders.models.Passports
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.io.TempDirectory
import com.twitter.logging.Logger
import org.apache.commons.io.FileUtils
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PassportExtractorTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with BeforeAndAfter {

  protected var testVersion = 1
  protected var testPassportId = "1"
  protected var testUserId = 1L
  protected var testEnv = "local"
  protected var testOtherEnv = "local2"
  private val testAuthUserId = 0L
  private val testGuestToken = 0L
  private val testClientApplicationId = 0L
  private val testSessionHash = ""
  private val testToken =
    "v2.public.eyJwYXNzcG9ydCI6eyJwdHAiOiJjdXMiLCJwaWQiOiIxIiwiZHBkIjp7ImFpZCI6WzUsMTAsMzcsMjUsMjAsMjksMSw2LDI4LDM4LDIxLDMzLDksMTMsMiwzMiwxNywyMiwyNywxMiw3LDM5LDMsMzUsMTgsMTYsMzEsMTEsNDAsMjYsMjMsOCwzNiwzMCwxOSwxNV0sInJpZCI6WzU2LDUyLDU3LDYwLDUzLDU5LDU1LDU4LDUxXX0sInVpZCI6MSwiYWlkIjowLCJnaWQiOjAsImNpZCI6MCwic2lkIjoiIn0sImlzcyI6InVuaXR0ZXN0IiwiZXhwIjoiMjAzMi0wNC0xMFQxNTo1NDoyNy41ODkrMDA6MDAiLCJpYXQiOiIyMDIyLTA0LTEzVDE1OjU0OjI3LjU4OSswMDowMCJ9Ap1eyckoBnmr5R77TfWUgcUdcLYqSrjuF9_zDT9znVDNMl4a5ZJR1zCBwgb51llottaKyYECxmjNPoSHr2d9Bw.eyJtb2RlbFZlcnNpb24iOjEsImtpZCI6ImxvY2FsOnVuaXR0ZXN0OjEifQ"

  protected var customerPassport = new Passports.CustomerPassport(
    testPassportId,
    Some(testUserId),
    Some(testAuthUserId),
    Some(testGuestToken),
    Some(testClientApplicationId),
    Some(testSessionHash),
    None,
    None,
    None)

  val defaultFilename = "public_keys.json"
  val defaultSubPath = "pasetoheaders"
  val brokenConfigFileName = "public_key_test_invalid.json"
  val missingConfigFileName = "missing.json"
  // We are using a temp directory because of the build files macro which copies 1:1 what
  // the directory structure looks like for the files to the bazel sandbox. This is problematic
  // for the parsing logic in `PassportSigner` as it would see `pasetoheaders` from the root of
  // source `auth/pasetoheaders` and not from `auth/pasetoheaders/src/test/resources/test-public-keys/pasetoheaders/`
  // that would trick the `PassportSigner` to think the environment is `src` and not `local` because
  // of the path bazed evaluation done.
  private val tempDir = TempDirectory.create()
  private val srcDir = new java.io.File("auth/pasetoheaders/src/test/resources")
  FileUtils.copyDirectory(srcDir, tempDir)
  val testPublicKeysFolder = s"$tempDir/test-public-keys/$defaultSubPath"

  private[this] val statsReceiver = new InMemoryStatsReceiver
  private[this] val logger = Logger.get()

  test("test extractor with existing key") {
    val passportExtractor = PassportExtractor(
      logger = Some(logger),
      stats = Some(statsReceiver),
      configBusKeysSource = ConfigBusSource.local(testPublicKeysFolder),
      configBusKeysFileName = defaultFilename
    )

    val claims = passportExtractor.extractClaims(testToken)
    claims match {
      case Some(c) =>
        assertEquals(classOf[Passports.CustomerPassport], c.getEnclosedEntity.getClass)
        assertEquals(testPassportId, c.getEnclosedEntity.getPassportId)
      case None => fail()
    }
  }

  test("test extractor without integrity check") {
    val passportExtractor = PassportExtractor(
      logger = Some(logger),
      stats = Some(statsReceiver),
      configBusKeysSource = ConfigBusSource.local(testPublicKeysFolder),
      configBusKeysFileName = defaultFilename
    )

    val claims = passportExtractor.extractClaims(testToken, verifyIntegrity = false)
    claims match {
      case Some(c) =>
        assertEquals(classOf[Passports.CustomerPassport], c.getEnclosedEntity.getClass)
        assertEquals(testPassportId, c.getEnclosedEntity.getPassportId)
      case None => fail()
    }
  }

  test("test fast failing if config is missing") {
    intercept[ServiceBootException] {
      PassportExtractor(
        logger = Some(logger),
        stats = Some(statsReceiver),
        configBusKeysSource = ConfigBusSource.local(testPublicKeysFolder),
        configBusKeysFileName = missingConfigFileName
      )
    }

    statsReceiver.counters(List("PassportExtractor", "config_loading_timeout")) mustEqual 1L
  }

  test("test fast failing if config is invalid") {
    intercept[ServiceBootException] {
      PassportExtractor(
        logger = Some(logger),
        stats = Some(statsReceiver),
        configBusKeysSource = ConfigBusSource.local(testPublicKeysFolder),
        configBusKeysFileName = brokenConfigFileName
      )
    }

    statsReceiver.counters(List("PassportExtractor", "config_loading_timeout")) mustEqual 1L
  }

  /**
   * Keep here in case we want to test extraction performance locally
   */
  //  test("parse a file full of tokens, and see how long extraction takes for all tokens") {
  //    val passportExtractor = PassportExtractor(
  //      environment = testEnv,
  //      logger = Some(logger),
  //      stats = Some(statsReceiver),
  //      configBusKeysSource = ConfigBusSource.local(testPublicKeysFolder)
  //    )
  //
  //    // do this once so we "init" everything
  //    passportExtractor.extractClaims(testToken)
  //
  //    val lines = Source.fromFile("/path/to/tokens/file").getLines.toList
  //
  //    val elapsed = Stopwatch.start()
  //    val start = System.currentTimeMillis()
  //    for (line <- lines) {
  //      val c = passportExtractor.extractClaims(line).get
  //    }
  //    val timeTaken = System.currentTimeMillis() - start
  //    println("test took: " + timeTaken)
  //    val now = elapsed().inUnit(TimeUnit.MILLISECONDS).toFloat
  //    println("using time: " + now);
  //  }
}
