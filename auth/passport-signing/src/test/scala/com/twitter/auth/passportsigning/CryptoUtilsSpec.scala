package com.twitter.auth.passportsigning

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.util.Base64StringEncoder
import com.twitter.util.Duration
import com.twitter.util.Time
import java.security.KeyPairGenerator
import java.security.Signature
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite

@RunWith(classOf[JUnitRunner])
class CryptoUtilsSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with BeforeAndAfterEach {

  import com.twitter.auth.passportsigning.CryptoUtils._

  var statsReceiver = new InMemoryStatsReceiver

  val cryptoUtils = new CryptoUtils("ABCD", statsReceiver)
  val cryptoUtilsWithDefaults = new CryptoUtils("ABCD", statsReceiver)
  val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
  keyPairGenerator.initialize(2048)
  val keyPair = keyPairGenerator.generateKeyPair()
  val publicKeyBytes = keyPair.getPublic.getEncoded
  val challenge = cryptoUtils.randomDigitString(32)
  val challengeBytes = challenge.getBytes
  val invalidBytes = "invalid".getBytes

  val instance = Signature.getInstance("SHA1withRSA")
  instance.initSign(keyPair.getPrivate)
  instance.update(challenge.getBytes)
  val signatureBytes = instance.sign
  val challengeResponseBytes = signatureBytes

  override def beforeEach(): Unit = {
    statsReceiver.clear()
  }

  test(
    "Random alphanumeric string: generate a string of the correct length using the correct mix of characters"
  ) {
    cryptoUtils.randomAlphanumericString(12) must fullyMatch regex "^[a-zA-Z0-9]{12}$"
  }

  test(
    "Random hex string: generate a string of the correct length using the correct mix of characters"
  ) {
    cryptoUtils.randomHexString(12) must fullyMatch regex ("^[a-f0-9]{12}$")
  }

  test("Random digit string: generate a string of the correct number of digits") {
    cryptoUtils.randomDigitString(12) must fullyMatch regex ("^[0-9]{12}$")
  }

  test(
    "Random readable base 32 string: generate a string of the correct number of readable base 32 characters"
  ) {
    cryptoUtils.randomReadableBase32String(12) must fullyMatch regex ("^[a-km-np-z2-9]{12}$")
  }

  {
    val signature = cryptoUtils.hmacSignature("message")

    test("Hmac signature: be a base64 string representing an array of 32 bytes") {
      val signatureBytes = Base64StringEncoder.decode(signature)
      signatureBytes.length mustEqual (32)
    }

    test("Hmac signature: be verifiable") {
      cryptoUtils.verifyHmacSignature("message", "messageSignature") mustBe false
      cryptoUtils.verifyHmacSignature("message", signature) mustBe true
    }

    test("Hmac signature: be verifiable with an expiration") {
      Time.withCurrentTimeFrozen { time =>
        val signature =
          cryptoUtils.hmacSignatureWithExpiration("message", Duration.fromMilliseconds(1000))
        time.advance(Duration.fromMilliseconds(500))
        cryptoUtils.verifyHmacSignatureWithExpiration(
          "message",
          signature,
          Duration.fromMilliseconds(1000)
        ) mustBe true
      }
    }

    test("Hmac signature: be verifiable with an expiration with partial matching") {
      Time.withCurrentTimeFrozen { time =>
        val partialLength = 6
        val signature =
          cryptoUtils.hmacSignatureWithExpiration("message", Duration.fromMilliseconds(1000))
        time.advance(Duration.fromMilliseconds(500))
        cryptoUtils.verifyHmacSignatureWithExpiration(
          "message",
          signature.take(partialLength),
          Duration.fromMilliseconds(1000),
          checkFullSignature = false
        ) mustBe true
      }
    }

    test("Hmac signature: reject partial matching with empty string") {
      Time.withCurrentTimeFrozen { time =>
        val partialLength = 0
        val signature =
          cryptoUtils.hmacSignatureWithExpiration("message", Duration.fromMilliseconds(1000))
        time.advance(Duration.fromMilliseconds(500))
        cryptoUtils.verifyHmacSignatureWithExpiration(
          "message",
          signature.take(partialLength),
          Duration.fromMilliseconds(1000),
          checkFullSignature = false
        ) mustBe false
      }
    }

    test("Hmac signature: expire with an expiration") {
      Time.withCurrentTimeFrozen { time =>
        val signature =
          cryptoUtils.hmacSignatureWithExpiration("message", Duration.fromMilliseconds(1000))
        time.advance(Duration.fromMilliseconds(3000))
        cryptoUtils.verifyHmacSignatureWithExpiration(
          "message",
          signature,
          Duration.fromMilliseconds(1000)
        ) mustBe false
      }
    }
  }

  {
    test("Verify signature: return false when challenge is malformed") {
      cryptoUtils.verifyChallengeResponse(
        "challenge".getBytes,
        challengeResponseBytes,
        publicKeyBytes
      ) mustBe false
    }

    test("Verify signature: return false when challenge is invalid") {
      cryptoUtils.verifyChallengeResponse(
        invalidBytes,
        challengeResponseBytes,
        publicKeyBytes) mustBe false
    }

    test("Verify signature: return false when response is malformed") {
      cryptoUtils.verifyChallengeResponse(
        challengeBytes,
        "challenge_reponse".getBytes,
        publicKeyBytes
      ) mustBe false
    }

    test("Verify signature: return false when response is invalid") {
      cryptoUtils.verifyChallengeResponse(challengeBytes, invalidBytes, publicKeyBytes) mustBe false
    }

    test("Verify signature: return false when key is malformed") {
      cryptoUtils.verifyChallengeResponse(
        challengeBytes,
        challengeResponseBytes,
        "public_key".getBytes
      ) mustBe false
    }

    test("Verify signature: return false when key is invalid") {
      cryptoUtils.verifyChallengeResponse(
        challengeBytes,
        challengeResponseBytes,
        invalidBytes) mustBe false
    }

    test("Verify signature: return true when response is valid") {
      cryptoUtils.verifyChallengeResponse(
        challengeBytes,
        challengeResponseBytes,
        publicKeyBytes) mustBe true
    }
  }

  val seed = cryptoUtils.randomDigitString(32).getBytes

  val lastSeenOfflineCode = cryptoUtils.generateOfflineCode(seed, MaxPossibleOfflineCodeIterations)
  val lastSeenOfflineCodeBytes = cryptoUtils.convertOfflineCodeToBytes(lastSeenOfflineCode)
  val oneIterOfflineCode =
    cryptoUtils.generateOfflineCode(seed, MaxPossibleOfflineCodeIterations - 1)
  val maxIterOfflineCode = cryptoUtils.generateOfflineCode(seed, 1)
  val maxPlusOneIterOfflineCode = cryptoUtils.generateOfflineCode(seed, 0)

  test("Convert offline code to bytes: return empty when offline code is invalid") {
    cryptoUtils.convertOfflineCodeToBytes("000ooolll111").isEmpty mustBe true
  }

  //test("Verify offline code")
  {
    test("Verify offline code: return false when offline code is malformed") {
      cryptoUtils.verifyOfflineCode("offlineCode", lastSeenOfflineCodeBytes) mustEqual ((false, 0))
    }
    test("Verify offline code: return false when offline code is invalid") {
      cryptoUtils.verifyOfflineCode(lastSeenOfflineCode, lastSeenOfflineCodeBytes) mustEqual (
        (
          false,
          0
        )
      )
    }
    test(
      "Verify offline code: return true and correct count when offline code is valid for a single iterations"
    ) {
      cryptoUtils.verifyOfflineCode(oneIterOfflineCode, lastSeenOfflineCodeBytes) mustEqual (
        (
          true,
          1
        )
      )
    }
    test(
      "Verify offline code: return true and correct count when offline code is valid for max iterations"
    ) {
      cryptoUtils.verifyOfflineCode(maxIterOfflineCode, lastSeenOfflineCodeBytes) mustEqual
        ((true, MaxPossibleOfflineCodeIterations - 1))
    }
    test("Verify offline code: return false when offline code is valid for max + 1 iterations") {
      cryptoUtils.verifyOfflineCode(maxPlusOneIterOfflineCode, lastSeenOfflineCodeBytes) mustEqual (
        (
          false,
          0
        )
      )
    }
  }

  test("Readable base 32 converters: return the same value when consecutively applied") {
    val randomReadableBase32 = cryptoUtils.randomReadableBase32String(32)
    val convertedReadableBase32 =
      cryptoUtils.base32ToReadable(cryptoUtils.readableToBase32(randomReadableBase32))
    randomReadableBase32 mustEqual convertedReadableBase32
  }

  //test("Client tokens")
  {
    val userAWithClientA = cryptoUtils.createClientToken(1, 3)
    val userAWithClientB = cryptoUtils.createClientToken(1, 4)
    val userBWithClientA = cryptoUtils.createClientToken(2, 3)
    val userBWithClientB = cryptoUtils.createClientToken(2, 4)

    test("Client tokens: be unique to users and client application ids") {
      Set(
        userAWithClientA,
        userAWithClientB,
        userBWithClientA,
        userBWithClientB
      ).size mustEqual 4
    }

    test("Client tokens: not be predictable") {
      userAWithClientA must not be (cryptoUtils.createClientToken(1, 3))
    }

    test("Client tokens: be parseable") {
      cryptoUtils.getClientAppIdFromToken(1, userAWithClientA) mustEqual Some(3)
      cryptoUtils.getClientAppIdFromToken(1, userAWithClientB) mustEqual Some(4)
      cryptoUtils.getClientAppIdFromToken(2, userBWithClientA) mustEqual Some(3)
      cryptoUtils.getClientAppIdFromToken(2, userBWithClientB) mustEqual Some(4)
    }

    test("Client tokens: return none for invalid tokens") {
      cryptoUtils.getClientAppIdFromToken(1, userBWithClientA) mustEqual None
      cryptoUtils.getClientAppIdFromToken(2, userAWithClientA) mustEqual None
      cryptoUtils.getClientAppIdFromToken(1, "123098-fala-fel") mustEqual None
      cryptoUtils.getClientAppIdFromToken(1, "falafel") mustEqual None
    }
  }
}
