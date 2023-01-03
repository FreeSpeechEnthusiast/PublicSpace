package com.twitter.auth.pasetoheaders.passport

import com.twitter.auth.pasetoheaders.finagle.ServiceBootException
import com.twitter.auth.pasetoheaders.models.Passports
import com.twitter.finagle.stats.InMemoryStatsReceiver
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import com.twitter.logging.Logger
import org.scalatest.matchers.must.Matchers
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.io.TempDirectory
import org.apache.commons.io.FileUtils
import org.hamcrest.MatcherAssert
import org.hamcrest.core.StringContains
import scala.collection.JavaConverters

@RunWith(classOf[JUnitRunner])
class PassportSignerTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with BeforeAndAfter {

  protected var testEnv = "local"
  protected var testIssuer = "unittest"
  protected var testVersion = 1

  protected var testPassportId = "1"
  protected var testUserId = 1L
  private val testAuthUserId = 0L
  private val testGuestToken = 0L
  private val testClientApplicationId = 0L
  private val testSessionHash = ""

  val random1 = scala.util.Random

  val allowed = Set[java.lang.Long](
    1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17, 18, 19, 20, 21, 22, 23, 25, 26, 27, 28, 29,
    30, 31, 32, 33, 35, 36, 37, 38, 39, 40,
  )
  val rejected = Set[java.lang.Long](
    51, 52, 53, 55, 56, 57, 58, 59, 60
  )

  // make passport more realistic and include some data permissions
  val dpd: com.twitter.auth.pasetoheaders.models.DataPermissionDecisions =
    new com.twitter.auth.pasetoheaders.models.DataPermissionDecisions(
      Option(JavaConverters.setAsJavaSet(allowed)),
      Option(JavaConverters.setAsJavaSet(rejected))
    )

  def generatePassport(
    passportId: String = testPassportId,
    userId: Long = testUserId,
  ): Passports.CustomerPassport = {
    new Passports.CustomerPassport(
      passportId,
      Some(userId),
      Some(testAuthUserId),
      Some(testGuestToken),
      Some(testClientApplicationId),
      Some(testSessionHash),
      Some(dpd),
      None,
      None)
  }

  // We are using a temp directory because of the build files macro which copies 1:1 what
  // the directory structure looks like for the files to the bazel sandbox. This is problematic
  // for the parsing logic in `PassportSigner` as it would see `pasetoheaders` from the root of
  // source `auth/pasetoheaders` and not from `auth/pasetoheaders/src/test/resources/test-private-keys/pasetoheaders/`
  // that would trick the `PassportSigner` to think the environment is `src` and not `local` because
  // of the path bazed evaluation done.
  private val tempDir = TempDirectory.create()
  private val srcDir = new java.io.File("auth/pasetoheaders/src/test/resources")
  FileUtils.copyDirectory(srcDir, tempDir)
  val testPrivateKeysFolder = s"$tempDir/test-private-keys"

  val defaultFilename = "private_keys.json"
  val defaultSubPath = "pasetoheaders"

  private[this] val statsReceiver = new InMemoryStatsReceiver
  private[this] val logger = Logger.get()

  private[this] def makePath(
    baseFolder: String,
    subPath: String,
    environment: String,
    issuer: String,
    filename: String
  ): String = {
    baseFolder + "/" + subPath + "/" + environment + "/" + issuer + "/" + filename
  }

  //  test("generate 1000 tokens") {
  //    val path = makePath(testPrivateKeysFolder, defaultSubPath, "local", "unittest", defaultFilename)
  //
  //    val passportSigner = PassportSigner(
  //      Some(logger),
  //      Some(statsReceiver),
  //      None,
  //      path
  //    )
  //
  //    var i = 0
  //    // FileWriter
  //    val file = new File("/some path here")
  //    val bw = new BufferedWriter(new FileWriter(file))
  //
  //    while (i < 1000) {
  //      val token =
  //        passportSigner.signToken(generatePassport(random1.nextInt().toString, random1.nextLong()))
  //      bw.write(token.get + "\n")
  //      token match {
  //        case Some(t) =>
  //          MatcherAssert.assertThat(t, StringContains.containsString("v2.public."))
  //        case None => fail()
  //      }
  //      i = i + 1
  //    }
  //    bw.close()
  //
  //    assert(passportSigner.getEnvironment() == "local")
  //    assert(passportSigner.getIssuer() == "unittest")
  //  }

  test("test signer with existing key with default filename") {
    val path = makePath(testPrivateKeysFolder, defaultSubPath, "local", "unittest", defaultFilename)

    println("test using: " + path)
    val passportSigner = PassportSigner(
      Some(logger),
      Some(statsReceiver),
      None,
      path
    )

    val token = passportSigner.signToken(generatePassport())
    println(token)
    token match {
      case Some(t) =>
        MatcherAssert.assertThat(t, StringContains.containsString("v2.public."))
      case None => fail()
    }

    assert(passportSigner.getEnvironment() == "local")
    assert(passportSigner.getIssuer() == "unittest")
  }

  test("test signer with existing key with default filename using configured key version") {
    val path = makePath(testPrivateKeysFolder, defaultSubPath, "local", "unittest", defaultFilename)

    println("test using: " + path)
    val passportSigner = PassportSigner(
      Some(logger),
      Some(statsReceiver),
      None,
      path
    )

    val token = passportSigner.signTokenUsingConfiguredKeyVersion(generatePassport())
    println(token)
    token match {
      case Some(t) =>
        MatcherAssert.assertThat(t, StringContains.containsString("v2.public."))
      case None => fail()
    }

    assert(
      1.0 === statsReceiver.gauges(List("PassportSigner", "selected_private_key_version")).apply())

    assert(passportSigner.getEnvironment() == "local")
    assert(passportSigner.getIssuer() == "unittest")
  }

  test("test signer with missing config file") {
    val path = makePath(testPrivateKeysFolder, defaultSubPath, "local", "nofile", defaultFilename)
    println("test using: " + path)

    intercept[ServiceBootException] {
      PassportSigner(
        Some(logger),
        Some(statsReceiver),
        None,
        path
      )
    }

    statsReceiver.counters(List("PassportSigner", "config_loading_timeout")) mustEqual 1L
  }

  test("test signer with invalid config file") {
    val path = makePath(testPrivateKeysFolder, defaultSubPath, "local", "badfile", defaultFilename)
    println("test using: " + path)

    intercept[ServiceBootException] {
      PassportSigner(
        Some(logger),
        Some(statsReceiver),
        None,
        path
      )
    }

    statsReceiver.counters(List("PassportSigner", "config_loading_timeout")) mustEqual 1L
  }

  // note: this test requires /var/lib/tss/keys/pasetoheaders directory
  // setup on a local machine
  // change the IF on the first line below to run the test
  test("test signer with existing key, as if it's running a TFE in PROD with tss") {
    val path = "/var/lib/tss/keys/pasetoheaders/prod/tfe/private_keys.json"
    import java.nio.file.Paths
    import java.nio.file.Files

    if (false && Files.exists(Paths.get(path))) {
      println("running prod path test")
      val passportSigner = PassportSigner(
        Some(logger),
        Some(statsReceiver),
        None,
        path
      )

      val token = passportSigner.signToken(generatePassport())
      token match {
        case Some(t) =>
          MatcherAssert.assertThat(t, StringContains.containsString("v2.public."))
        case None => fail()
      }
      assert(passportSigner.getEnvironment() == "prod")
      assert(passportSigner.getIssuer() == "tfe")
    }
  }

  // note: this test requires /usr/local/config/auth/pasetoheaders/nonprod/tfe/private_keys.json
  // setup on a local machine
  // change the IF on the first line below to run the test
  test("test signer with existing key, as if it's running a TFE in NONPROD with configbus") {
    val path = "/usr/local/config/auth/pasetoheaders/nonprod/tfe/private_keys.json"
    import java.nio.file.Paths
    import java.nio.file.Files

    if (Files.exists(Paths.get(path))) {
      println("running nonprod path test")
      val passportSigner = PassportSigner(
        Some(logger),
        Some(statsReceiver),
        None,
        path
      )

      val token = passportSigner.signToken(generatePassport())
      token match {
        case Some(t) =>
          MatcherAssert.assertThat(t, StringContains.containsString("v2.public."))
          println("signed token:\n" + token.getOrElse(None))
        case None => fail()
      }
      assert(passportSigner.getEnvironment() == "nonprod")
      assert(passportSigner.getIssuer() == "tfe")
    }
  }

  // note: this test requires /usr/local/config/auth/pasetoheaders/local/unittest/private_keys.json
  // setup on a local machine
  // change the IF on the first line below to run the test
  // you may use this test to create unit-testable tokens
  // if you do so, please make sure to set the expiry time for the token to 10+ years
  test("test signer with existing key, as if it's running a unittest in local with configbus") {
    val path = "/usr/local/config/auth/pasetoheaders/local/unittest/private_keys.json"
    import java.nio.file.Paths
    import java.nio.file.Files

    if (Files.exists(Paths.get(path))) {
      println("running unittest path test")
      val passportSigner = PassportSigner(
        Some(logger),
        Some(statsReceiver),
        None,
        path
      )

      val token = passportSigner.signToken(generatePassport())
      token match {
        case Some(t) =>
          MatcherAssert.assertThat(t, StringContains.containsString("v2.public."))
          println("signed token:\n" + token.getOrElse(None))
        case None => fail()
      }
      assert(passportSigner.getEnvironment() == "local")
      assert(passportSigner.getIssuer() == "unittest")
    }
  }
}
