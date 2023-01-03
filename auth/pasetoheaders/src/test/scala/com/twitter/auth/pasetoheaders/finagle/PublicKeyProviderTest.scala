package com.twitter.auth.pasetoheaders.finagle

import com.twitter.auth.pasetoheaders.encryption.KeyIdentifier
import com.twitter.auth.pasetoheaders.encryption.KeyUtils
import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.configbus.subscriber.Subscription
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import com.twitter.conversions.DurationOps._
import java.io.File
import java.nio.file.Files
import java.security.PublicKey
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RunWith(classOf[JUnitRunner])
class PublicKeyProviderTest extends KeyProvidersBase {

  protected var testPublicKeyString: String =
    "31c273c193af94a96d3a8db254cbc5aac81e881e098746d549caf2dea819ed81"
  protected var testOtherPublicKeyString: String =
    "ffb9dbbbbc047c03fd70604e0071f0987e16b28b757225c11f00415d0e20b1a2"

  val tempConfPath = Files.createTempDirectory("testconfigbus").toFile
  private[this] val configBusConfigFile = "/testpublickeys.json"

  after {
    new File(tempConfPath, configBusConfigFile).delete()
  }

  protected def buildStorage(): UpdatableKeyStorage[PublicKey] =
    SimpleUpdatableKeyStorage[PublicKey]()

  def subscribeToConfigBus(): Subscription[PublicKeysConfiguration] = {
    val fakeConfigSource = PollingConfigSourceBuilder()
      .basePath(tempConfPath.getAbsolutePath)
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
        path = configBusConfigFile,
        initialValue = PublicKeysConfiguration.EMPTY,
        defaultValue = None)
  }

  test("test public key set load through configbus") {
    fakeData(
      tempConfPath,
      configBusConfigFile,
      """{
        |  "keys": [
        |    {
        |      "key_issuer": "test",
        |      "key_version": 1,
        |      "key_data": "31c273c193af94a96d3a8db254cbc5aac81e881e098746d549caf2dea819ed81",
        |      "key_environment": "devel"
        |    }
        |  ]
        |}""".stripMargin
    )

    val publicKeysSubscription = subscribeToConfigBus()
    val keyProvider = PublicKeyProviderProxy(
      pasetoHeadersPublicKeySubscription = publicKeysSubscription,
      logger = Some(loggerConnector),
      stats = Some(statsConnector),
      publicKeyStorage = buildStorage()
    )

    eventually {
      val key = keyProvider.getPublicKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPublicKey(testPublicKeyString)
      } else {
        fail()
      }
    }

    publicKeysSubscription.firstLoadCompleted.isDefined must be(true)

  }

  test("test that public key can be removed from configbus") {
    fakeData(
      tempConfPath,
      configBusConfigFile,
      """{
        |  "keys": [
        |    {
        |      "key_issuer": "test",
        |      "key_version": 1,
        |      "key_data": "31c273c193af94a96d3a8db254cbc5aac81e881e098746d549caf2dea819ed81",
        |      "key_environment": "devel"
        |    },
        |    {
        |      "key_issuer": "test2",
        |      "key_version": 2,
        |      "key_data": "ffb9dbbbbc047c03fd70604e0071f0987e16b28b757225c11f00415d0e20b1a2",
        |      "key_environment": "devel"
        |    }
        |  ]
        |}""".stripMargin
    )

    val publicKeysSubscription = subscribeToConfigBus()
    val keyProvider = PublicKeyProviderProxy(
      pasetoHeadersPublicKeySubscription = publicKeysSubscription,
      logger = Some(loggerConnector),
      stats = Some(statsConnector),
      publicKeyStorage = buildStorage()
    )

    eventually {
      val key = keyProvider.getPublicKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPublicKey(testPublicKeyString)
      } else {
        fail()
      }
    }

    eventually {
      val key =
        keyProvider.getPublicKey(new KeyIdentifier(testEnv, testOtherIssuer, testOtherVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPublicKey(testOtherPublicKeyString)
      } else {
        fail()
      }
    }

    fakeData(
      tempConfPath,
      configBusConfigFile,
      """{
        |  "keys": [
        |    {
        |      "key_issuer": "test",
        |      "key_version": 1,
        |      "key_data": "31c273c193af94a96d3a8db254cbc5aac81e881e098746d549caf2dea819ed81",
        |      "key_environment": "devel"
        |    }
        |  ]
        |}""".stripMargin
    )

    eventually {
      val key = keyProvider.getPublicKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPublicKey(testPublicKeyString)
      } else {
        fail()
      }
    }

    eventually {
      val key =
        keyProvider.getPublicKey(new KeyIdentifier(testEnv, testOtherIssuer, testOtherVersion))
      key.isPresent mustEqual false
    }

    publicKeysSubscription.firstLoadCompleted.isDefined must be(true)

  }

  test("test that key set can be added to configbus") {
    fakeData(
      tempConfPath,
      configBusConfigFile,
      """{
        |  "keys": [
        |    {
        |      "key_issuer": "test",
        |      "key_version": 1,
        |      "key_data": "31c273c193af94a96d3a8db254cbc5aac81e881e098746d549caf2dea819ed81",
        |      "key_environment": "devel"
        |    }
        |  ]
        |}""".stripMargin
    )

    val publicKeysSubscription = subscribeToConfigBus()
    val keyProvider = PublicKeyProviderProxy(
      pasetoHeadersPublicKeySubscription = publicKeysSubscription,
      logger = Some(loggerConnector),
      stats = Some(statsConnector),
      publicKeyStorage = buildStorage()
    )

    eventually {
      val key = keyProvider.getPublicKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPublicKey(testPublicKeyString)
      } else {
        fail()
      }
    }

    eventually {
      val key =
        keyProvider.getPublicKey(new KeyIdentifier(testOtherEnv, testOtherIssuer, testOtherVersion))
      key.isPresent mustEqual false
    }

    fakeData(
      tempConfPath,
      configBusConfigFile,
      """{
        |  "keys": [
        |    {
        |      "key_issuer": "test",
        |      "key_version": 1,
        |      "key_data": "31c273c193af94a96d3a8db254cbc5aac81e881e098746d549caf2dea819ed81",
        |      "key_environment": "devel"
        |    },
        |    {
        |      "key_issuer": "test2",
        |      "key_version": 2,
        |      "key_data": "ffb9dbbbbc047c03fd70604e0071f0987e16b28b757225c11f00415d0e20b1a2",
        |      "key_environment": "devel"
        |    }
        |  ]
        |}""".stripMargin
    )

    eventually {
      val key = keyProvider.getPublicKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPublicKey(testPublicKeyString)
      } else {
        fail()
      }
    }

    eventually {
      val key =
        keyProvider.getPublicKey(new KeyIdentifier(testEnv, testOtherIssuer, testOtherVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPublicKey(testOtherPublicKeyString)
      } else {
        fail()
      }
    }

    publicKeysSubscription.firstLoadCompleted.isDefined must be(true)

  }

  test("public key storage concurrency test") {
    fakeData(
      tempConfPath,
      configBusConfigFile,
      """{
        |  "keys": [
        |    {
        |      "key_issuer": "test",
        |      "key_version": 1,
        |      "key_data": "31c273c193af94a96d3a8db254cbc5aac81e881e098746d549caf2dea819ed81",
        |      "key_environment": "devel"
        |    }
        |  ]
        |}""".stripMargin
    )

    val publicKeysSubscription = subscribeToConfigBus()
    val keyProvider = PublicKeyProviderProxy(
      pasetoHeadersPublicKeySubscription = publicKeysSubscription,
      logger = Some(loggerConnector),
      stats = Some(statsConnector),
      publicKeyStorage = buildStorage()
    )

    eventually {
      val key = keyProvider.getPublicKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
      if (key.isPresent) {
        key.get() mustEqual KeyUtils.getPublicKey(testPublicKeyString)
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
              tempConfPath,
              configBusConfigFile,
              """{
                |  "keys": [
                |    {
                |      "key_issuer": "test",
                |      "key_version": 1,
                |      "key_data": "31c273c193af94a96d3a8db254cbc5aac81e881e098746d549caf2dea819ed81",
                |      "key_environment": "devel"
                |    }
                |  ]
                |}""".stripMargin
            )
          } else {
            // attempt to read
            keyProvider.getPublicKey(new KeyIdentifier(testEnv, testIssuer, testVersion))
          }
        }
      })
    }
    for (result <- tasks) result.get()
    e.shutdown()

    publicKeysSubscription.firstLoadCompleted.isDefined must be(true)

  }

}
