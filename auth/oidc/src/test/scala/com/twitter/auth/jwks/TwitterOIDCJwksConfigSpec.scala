package com.twitter.auth.jwks

import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.finagle.stats.NullStatsReceiver
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TwitterOIDCJwksConfigSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with BeforeAndAfter {

  private val fileName = "testMapping"
  private var source: MockConfigSource = _
  private var configbusSubscriber: ConfigbusSubscriber = _
  private var jwksConfig: TwitterOIDCJwksConfig = _

  before {
    source = new MockConfigSource
    source.setFileContents(fileName, keysString)
    configbusSubscriber = new ConfigbusSubscriber(NullStatsReceiver, source, "")
    jwksConfig = new TwitterOIDCJwksConfig(configbusSubscriber, fileName, NullStatsReceiver)
  }

  val publicKey = TwitterOIDCPublicKey("t", "t", "t", "t", "t", "t")
  val publicKey2 = TwitterOIDCPublicKey("t2", "t2", "t2", "t2", "t2", "t2")

  private val publicKeys = List(publicKey)
  private val publicKeys2 = List(publicKey, publicKey2)

  private val keysString: String =
    """
      |{
      |  "keys": [
      |    {
      |      "alg": "t",
      |      "n": "t",
      |      "kid": "t",
      |      "kty": "t",
      |      "use": "t",
      |      "e": "t",
      |      "pkey": "t"
      |    }
      |  ]
      |}
    """.stripMargin

  private val keysString2: String =
    """
      |{
      |  "keys": [
      |    {
      |      "alg": "t",
      |      "n": "t",
      |      "kid": "t",
      |      "kty": "t",
      |      "use": "t",
      |      "e": "t",
      |      "pkey": "t"
      |    },
      |        {
      |      "alg": "t2",
      |      "n": "t2",
      |      "kid": "t2",
      |      "kty": "t2",
      |      "use": "t2",
      |      "e": "t2",
      |      "pkey": "t2"
      |    }
      |  ]
      |}
    """.stripMargin

  private val keysStringWithInvalidJSON: String =
    """
      |{
      |  "keys": [
      |    {
      |      "alg": "RS256",
      |      "n": "gRtjwICtIC_4ae33Ks7S80n32PLFEC4UtBanBFE9Pjzcpp4XWDPgbbOkNC9BZ-Jkyq6aoP_UknfJPI-cIvE6IE96bPNGs6DcfZ73Cq2A9ZXTdiuuOiqMwhEgLKFVRUZZ50calENLGyi96-6lcDnwLehh-kEg7ARITmrBO0iAjFU",
      |      "kid": "14a00zz-23c5-4f35-bbdc-2d171h97b80g",
      |      "kty": "RSA",
      |      "use": "sig",
      |      "e": "AQAB
      |      "pkey": "test"
      |    }
      |  ]
      |}
    """.stripMargin

  test("Return the original config if file is present") {
    source.setFileContents(fileName, keysString)
    Assert.assertEquals(TwitterOIDCJwks(publicKeys), jwksConfig.getTwitterOIDCJwks().sample())
  }

  test("Updates in file gets reflected") {
    source.setFileContents(fileName, keysString)
    Assert.assertEquals(TwitterOIDCJwks(publicKeys), jwksConfig.getTwitterOIDCJwks().sample())
    source.setFileContents(fileName, keysString2)
    Assert.assertEquals(TwitterOIDCJwks(publicKeys2), jwksConfig.getTwitterOIDCJwks().sample())
  }

  test("Does not update mapping if the file is updated incorrectly") {
    source.setFileContents(fileName, keysString)
    Assert.assertEquals(TwitterOIDCJwks(publicKeys), jwksConfig.getTwitterOIDCJwks().sample())
    source.setFileContents(fileName, keysStringWithInvalidJSON)
    // Var does not change with invalid configs committed
    Assert.assertEquals(TwitterOIDCJwks(publicKeys), jwksConfig.getTwitterOIDCJwks().sample())
  }

}
