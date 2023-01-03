package com.twitter.auth.pasetoheaders.finagle

import com.twitter.auth.pasetoheaders.encryption.KeyIdentifier
import com.twitter.auth.pasetoheaders.encryption.KeyUtils
import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.configbus.subscriber.Subscription
import com.twitter.conversions.DurationOps._
import java.io.File
import java.nio.file.Files
import java.security.PrivateKey
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PrivateKeyProviderTest extends KeyProvidersBase {

  protected var testPrivateKeyString: String =
    "976e9e84221efcd8cbb4083948371fdcb833df0ad5ffd816fb07d25639c28bfe"
  protected var testOtherPrivateKeyString: String =
    "6054C48F759C6C7D73EF839FBF4C5D4CA39986AD18C8C3A49B3796C7E660A3F6"

  val tempTssPath = Files.createTempDirectory("testtss").toFile

  private[this] val tssConfigFile = "/testprivatekeys.json"

  after {
    new File(tempTssPath, tssConfigFile).delete()
  }

  protected def buildStorage(): UpdatableKeyStorage[PrivateKey] =
    SimpleUpdatableKeyStorage[PrivateKey]()

  def subscribeToTss(): Subscription[PrivateKeysConfiguration] = {
    val fakeConfigSource = PollingConfigSourceBuilder()
      .basePath(tempTssPath.getAbsolutePath)
      .pollPeriod(20.milliseconds)
      .versionFilePath(None)
      .build()
    val configSubscriber: ConfigbusSubscriber =
      new ConfigbusSubscriber(
        statsReceiver = statsReceiver,
        configSource = fakeConfigSource,
        rootPath = ".")
    configSubscriber
      .subscribeAndPublish(
        path = tssConfigFile,
        initialValue = PrivateKeysConfiguration.EMPTY,
        defaultValue = None)
  }

  test("test private key set load through tss") {
    fakeData(
      tempTssPath,
      tssConfigFile,
      """{
        |  "selected_key_version": 1,
        |  "keys": [
        |    {
        |      "key_version": 1,
        |      "key_data": "976e9e84221efcd8cbb4083948371fdcb833df0ad5ffd816fb07d25639c28bfe"
        |    }
        |  ]
        |}""".stripMargin
    )

    val privateKeysSubscription = subscribeToTss()
    val keyProvider = PrivateKeyProviderProxy(
      environment = testEnv,
      issuer = testIssuer,
      pasetoHeadersPrivateKeySubscription = privateKeysSubscription,
      logger = Some(loggerConnector),
      stats = Some(statsConnector),
      privateKeyStorage = buildStorage()
    )

    eventually {
      val key = keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPrivateKey(testPrivateKeyString)
      } else {
        fail()
      }
    }

    privateKeysSubscription.firstLoadCompleted.isDefined must be(true)

  }

  test(
    "test key set load through tss with incorrect selected_key_version doesn't replace private key set") {

    fakeData(
      tempTssPath,
      tssConfigFile,
      """{
        |  "selected_key_version": 1,
        |  "keys": [
        |    {
        |      "key_version": 1,
        |      "key_data": "976e9e84221efcd8cbb4083948371fdcb833df0ad5ffd816fb07d25639c28bfe"
        |    }
        |  ]
        |}""".stripMargin
    )

    val privateKeysSubscription = subscribeToTss()
    val keyProvider = PrivateKeyProviderProxy(
      environment = testEnv,
      issuer = testIssuer,
      pasetoHeadersPrivateKeySubscription = privateKeysSubscription,
      logger = Some(loggerConnector),
      stats = Some(statsConnector),
      privateKeyStorage = buildStorage()
    )

    eventually {
      val key = keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPrivateKey(testPrivateKeyString)
      } else {
        fail()
      }
    }

    fakeData(
      tempTssPath,
      tssConfigFile,
      """{
        |  "selected_key_version": 2,
        |  "keys": [
        |    {
        |      "key_version": 1,
        |      "key_data": "976e9e84221efcd8cbb4083948371fdcb833df0ad5ffd816fb07d25639c28bfe"
        |    }
        |  ]
        |}""".stripMargin
    )

    eventually {
      val key = keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPrivateKey(testPrivateKeyString)
      } else {
        fail()
      }
    }

    privateKeysSubscription.firstLoadCompleted.isDefined must be(true)
  }

  test("test that private key can be removed from tss") {

    fakeData(
      tempTssPath,
      tssConfigFile,
      """{
        |  "selected_key_version": 2,
        |  "keys": [
        |    {
        |      "key_version": 1,
        |      "key_data": "976e9e84221efcd8cbb4083948371fdcb833df0ad5ffd816fb07d25639c28bfe"
        |    },
        |    {
        |      "key_version": 2,
        |      "key_data": "6054C48F759C6C7D73EF839FBF4C5D4CA39986AD18C8C3A49B3796C7E660A3F6"
        |    }
        |  ]
        |}""".stripMargin
    )

    val privateKeysSubscription = subscribeToTss()
    val keyProvider = PrivateKeyProviderProxy(
      environment = testEnv,
      issuer = testIssuer,
      pasetoHeadersPrivateKeySubscription = privateKeysSubscription,
      logger = Some(loggerConnector),
      stats = Some(statsConnector),
      privateKeyStorage = buildStorage()
    )

    eventually {
      val key = keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPrivateKey(testPrivateKeyString)
      } else {
        fail()
      }
    }

    eventually {
      val key =
        keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testOtherVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPrivateKey(testOtherPrivateKeyString)
      } else {
        fail()
      }
    }

    fakeData(
      tempTssPath,
      tssConfigFile,
      """{
        |  "selected_key_version": 2,
        |  "keys": [
        |    {
        |      "key_version": 2,
        |      "key_data": "6054C48F759C6C7D73EF839FBF4C5D4CA39986AD18C8C3A49B3796C7E660A3F6"
        |    }
        |  ]
        |}""".stripMargin
    )

    eventually {
      val key = keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testOtherVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPrivateKey(testOtherPrivateKeyString)
      } else {
        fail()
      }
    }

    eventually {
      val key =
        keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      key.isPresent mustEqual false
    }

    privateKeysSubscription.firstLoadCompleted.isDefined must be(true)

  }

  test("test that key set can be added to tss") {

    fakeData(
      tempTssPath,
      tssConfigFile,
      """{
        |  "selected_key_version": 1,
        |  "keys": [
        |    {
        |      "key_version": 1,
        |      "key_data": "976e9e84221efcd8cbb4083948371fdcb833df0ad5ffd816fb07d25639c28bfe"
        |    }
        |  ]
        |}""".stripMargin
    )

    val privateKeysSubscription = subscribeToTss()
    val keyProvider = PrivateKeyProviderProxy(
      environment = testEnv,
      issuer = testIssuer,
      pasetoHeadersPrivateKeySubscription = privateKeysSubscription,
      logger = Some(loggerConnector),
      stats = Some(statsConnector),
      privateKeyStorage = buildStorage()
    )

    eventually {
      val key = keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPrivateKey(testPrivateKeyString)
      } else {
        fail()
      }
    }

    eventually {
      val key =
        keyProvider.getPrivateKey(
          new KeyIdentifier(testOtherEnv, testOtherIssuer, testOtherVersion))
      key.isPresent mustEqual false
    }

    fakeData(
      tempTssPath,
      tssConfigFile,
      """{
        |  "selected_key_version": 2,
        |  "keys": [
        |    {
        |      "key_version": 1,
        |      "key_data": "976e9e84221efcd8cbb4083948371fdcb833df0ad5ffd816fb07d25639c28bfe"
        |    },
        |    {
        |      "key_version": 2,
        |      "key_data": "6054C48F759C6C7D73EF839FBF4C5D4CA39986AD18C8C3A49B3796C7E660A3F6"
        |    }
        |  ]
        |}""".stripMargin
    )

    eventually {
      val key = keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPrivateKey(testPrivateKeyString)
      } else {
        fail()
      }
    }

    eventually {
      val key =
        keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testOtherVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPrivateKey(testOtherPrivateKeyString)
      } else {
        fail()
      }
    }

    eventually {
      val versionedKey =
        keyProvider.getLastPrivateKey(testEnv, testIssuer)
      if (versionedKey.isPresent) {
        versionedKey.get().getKey mustEqual KeyUtils.getPrivateKey(testOtherPrivateKeyString)
      } else {
        fail()
      }
    }

    privateKeysSubscription.firstLoadCompleted.isDefined must be(true)

  }

  test("private key storage concurrency test") {

    fakeData(
      tempTssPath,
      tssConfigFile,
      """{
        |  "selected_key_version": 1,
        |  "keys": [
        |    {
        |      "key_version": 1,
        |      "key_data": "976e9e84221efcd8cbb4083948371fdcb833df0ad5ffd816fb07d25639c28bfe"
        |    }
        |  ]
        |}""".stripMargin
    )

    val privateKeysSubscription = subscribeToTss()
    val keyProvider = PrivateKeyProviderProxy(
      environment = testEnv,
      issuer = testIssuer,
      pasetoHeadersPrivateKeySubscription = privateKeysSubscription,
      logger = Some(loggerConnector),
      stats = Some(statsConnector),
      privateKeyStorage = buildStorage()
    )

    eventually {
      val key = keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPrivateKey(testPrivateKeyString)
      } else {
        fail()
      }
    }

    // check for fatal exceptions during read / write
    val e: ExecutorService = Executors.newFixedThreadPool(50)
    val tasks = for (i <- 0 to 50000) yield {
      e.submit(new Callable[Unit]() {
        override def call() {
          if (i % 50 == 0) {
            // write periodically
            fakeData(
              tempTssPath,
              tssConfigFile,
              """{
                |  "selected_key_version": 1,
                |  "keys": [
                |    {
                |      "key_version": 1,
                |      "key_data": "976e9e84221efcd8cbb4083948371fdcb833df0ad5ffd816fb07d25639c28bfe"
                |    }
                |  ]
                |}""".stripMargin
            )
          } else {
            // attempt to read
            keyProvider.getPrivateKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
            keyProvider.getLastPrivateKey(testEnv, testIssuer)
          }
        }
      })
    }
    for (result <- tasks) result.get()
    e.shutdown()

    privateKeysSubscription.firstLoadCompleted.isDefined must be(true)

  }

}
