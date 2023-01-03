package com.twitter.auth.sso.signature

import com.twitter.auth.sso.store.AssociationReader
import com.twitter.auth.sso.signature.SignatureValidatorResult._
import com.twitter.stitch.Stitch
import com.twitter.util.Await
import com.twitter.util.Time
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class SsoSignatureClientSpec extends AnyFunSuite with Matchers with MockitoSugar {

  val testSecret: String = "1234567890abcdefghijklmnopqrstuv"

  val reader = mock[AssociationReader]
  val client: SsoSignatureClient = SsoSignatureClient(testSecret, reader)

  val testVersion: Int = 0
  val testUserId: Long = 1L
  val testSsoId: String = "ssoId123"
  val testRequestedUserId: Long = 1L
  val testRequestedSsoId: String = "ssoId123"
  val testRequestsSsoIdEncoded: String = "c3NvSWQxMjM="
  val testRequestedWrongUserId: Long = 2L
  val testRequestedWrongSsoId: String = "ssoId124"
  val testTimestampMs: Long = 123L
  val testHash: String = "kHFvZ1l4k2xjndHFvtQskJoQZ0YzssH/ajL/6dTTJBA="
  val testTime: Time = Time.fromMilliseconds(testTimestampMs)
  val ssoSignatureData: SsoSignatureData =
    SsoSignatureData(testVersion, testUserId, testSsoId, testTime)

  val testValidSignature: String =
    Seq(testVersion, testUserId, testSsoId, testTimestampMs, testHash).mkString(
      SsoSignatureClient.SsoDataDelimiter.toString)

  val testInvalidHash: String = "hashhash"
  val testInvalidSignature: String =
    Seq(testVersion, testUserId, testRequestsSsoIdEncoded, testTimestampMs, testInvalidHash)
      .mkString(SsoSignatureClient.SsoDataDelimiter.toString)

  def stitchAwait[E](s: Stitch[E]): E = {
    Await.result(Stitch.run(s))
  }

  test("SsoSignatureClient#parseSignature returns SsoSignatureData and hash") {
    SsoSignatureClient.parseSignature(testInvalidSignature).toOption match {
      case Some((data, hash)) =>
        data must equal(ssoSignatureData)
        hash must equal(testInvalidHash)
      case _ => fail()
    }
  }

  test(
    "SsoSignatureClient#parseSignature throws InvalidSignatureException when the signature doesn't match the expected format") {
    val signature: String =
      Seq(testVersion, testUserId).mkString(SsoSignatureClient.SsoDataDelimiter.toString)

    intercept[InvalidSignatureException] {
      SsoSignatureClient.parseSignature(signature).get()
    }
  }

  test(
    "SsoSignatureClient#parseSignature throws InvalidSignatureException when the signature components are the wrong type") {
    val signature: String =
      Seq(testSsoId, testSsoId, testSsoId, testSsoId, testSsoId).mkString(
        SsoSignatureClient.SsoDataDelimiter.toString)

    intercept[SignatureParsingException] {
      SsoSignatureClient.parseSignature(signature).get()
    }
  }

  test("SsoSignatureClient#HashMatches verifies the hash") {
    stitchAwait(
      client
        .HashMatches(
          SignatureValidationContext(
            ssoSignatureData,
            testHash,
            testRequestedUserId,
            testRequestedSsoId,
            reader
          )
        ).map(_.isValid)) must be(true)
    stitchAwait(
      client
        .HashMatches(
          SignatureValidationContext(
            ssoSignatureData,
            testInvalidHash,
            testRequestedUserId,
            testRequestedSsoId,
            reader)
        ).map(_.isValid)) must be(false)
  }

  test("SsoSignatureClient#WithinWindow verifies the time window") {
    Time.withTimeAt(testTime) { _ =>
      stitchAwait(
        client
          .WithinWindow(
            SignatureValidationContext(
              ssoSignatureData,
              testHash,
              testRequestedUserId,
              testRequestedSsoId,
              reader)
          ).map(_.isValid)) must be(true)
    }

    Time.withTimeAt(Time.Top) { _ =>
      stitchAwait(
        client
          .HashMatches(
            SignatureValidationContext(
              ssoSignatureData,
              testInvalidHash,
              testRequestedUserId,
              testRequestedSsoId,
              reader)).map(_.isValid)) must be(false)
    }
  }

  test("SsoSignatureClient#WithinRequestedMapping verifies the context") {
    stitchAwait(
      client
        .WithinRequestedMapping(
          SignatureValidationContext(
            ssoSignatureData,
            testHash,
            testRequestedUserId,
            testRequestedSsoId,
            reader
          )).map(_.isValid)) must be(true)
  }

  test(
    "SsoSignatureClient#WithinRequestedMapping verifies the context (wrong requested ssoid and userid)") {
    stitchAwait(
      client
        .WithinRequestedMapping(
          SignatureValidationContext(
            ssoSignatureData,
            testHash,
            testRequestedWrongUserId,
            testRequestedWrongSsoId,
            reader
          )).map(_.isValid)) must be(false)
  }

  test("SsoSignatureClient#WithinRequestedMapping verifies the context (wrong requested ssoid)") {
    stitchAwait(
      client
        .WithinRequestedMapping(
          SignatureValidationContext(
            ssoSignatureData,
            testHash,
            testRequestedUserId,
            testRequestedWrongSsoId,
            reader
          )).map(_.isValid)) must be(false)
  }

  test("SsoSignatureClient#WithinRequestedMapping verifies the context (wrong requested userid)") {
    stitchAwait(
      client
        .WithinRequestedMapping(
          SignatureValidationContext(
            ssoSignatureData,
            testHash,
            testRequestedWrongUserId,
            testRequestedSsoId,
            reader
          )
        ).map(_.isValid)) must be(false)
  }

  test("SsoSignatureClient#WithinActualMapping verifies the context with actual mapping") {
    when(reader.getAccountForSsoId(testRequestedSsoId)) thenReturn Stitch.value(
      Some(testRequestedUserId))
    stitchAwait(
      client
        .WithinActualMapping(
          SignatureValidationContext(
            ssoSignatureData,
            testHash,
            testRequestedUserId,
            testRequestedSsoId,
            reader
          )).map(_.isValid)) must be(true)
  }

  test("SsoSignatureClient#WithinActualMapping verifies the context with outdated mapping") {
    when(reader.getAccountForSsoId(testRequestedSsoId)) thenReturn Stitch.value(
      Some(testRequestedWrongUserId))
    stitchAwait(
      client
        .WithinActualMapping(
          SignatureValidationContext(
            ssoSignatureData,
            testHash,
            testRequestedUserId,
            testRequestedSsoId,
            reader
          )).map(_.isValid)) must be(false)
  }

  test(
    "SsoSignatureClient#WithinActualMapping verifies the context non-existing mapping for registration flow") {
    when(reader.getAccountForSsoId(testRequestedSsoId)) thenReturn Stitch.None
    stitchAwait(
      client
        .WithinActualMapping(
          SignatureValidationContext(
            ssoSignatureData,
            testHash,
            testRequestedUserId,
            testRequestedSsoId,
            reader
          )).map(_.isValid)) must be(true)
  }

  test("SsoSignatureClient#validateSsoSignature returns true for a valid signature and context") {
    when(reader.getAccountForSsoId(testRequestedSsoId)) thenReturn Stitch.value(
      Some(testRequestedUserId))
    Time.withCurrentTimeFrozen { _ =>
      val signature = client.createSsoSignature(testUserId, testSsoId)
      stitchAwait(
        client.validateSsoSignature(signature, testRequestedUserId, testRequestedSsoId)) must be(
        Valid)
    }
  }

  test("SsoSignatureClient#validateSsoSignature returns false for an invalid signature") {
    stitchAwait(
      client
        .validateSsoSignature(testInvalidSignature, testRequestedUserId, testRequestedSsoId).map(
          _.isValid)) must be(false)
  }

  test(
    "SsoSignatureClient#validateSsoSignature returns false for an invalid context (wrong requested userid)") {
    when(reader.getAccountForSsoId(testRequestedSsoId)) thenReturn Stitch.value(
      Some(testRequestedUserId))
    Time.withCurrentTimeFrozen { _ =>
      val signature = client.createSsoSignature(testUserId, testSsoId)
      stitchAwait(
        client
          .validateSsoSignature(signature, testRequestedWrongUserId, testRequestedSsoId).map(
            _.isValid)) must be(false)
    }
  }

  test(
    "SsoSignatureClient#validateSsoSignature returns false for an invalid context (wrong requested ssoid)") {
    when(reader.getAccountForSsoId(testRequestedWrongSsoId)) thenReturn Stitch.value(
      Some(testRequestedUserId))
    Time.withCurrentTimeFrozen { _ =>
      val signature = client.createSsoSignature(testUserId, testSsoId)
      stitchAwait(
        client
          .validateSsoSignature(signature, testRequestedUserId, testRequestedWrongSsoId).map(
            _.isValid)) must be(false)
    }
  }

  test(
    "SsoSignatureClient#validateSsoSignature returns false for an invalid context (wrong requested ssoid and userid)") {
    when(reader.getAccountForSsoId(testRequestedWrongSsoId)) thenReturn Stitch.value(
      Some(testRequestedUserId))
    Time.withCurrentTimeFrozen { _ =>
      val signature = client.createSsoSignature(testUserId, testSsoId)
      stitchAwait(
        client
          .validateSsoSignature(signature, testRequestedWrongUserId, testRequestedWrongSsoId).map(
            _.isValid)) must be(false)
    }
  }

  test(
    "SsoSignatureClient#validateSsoSignature returns false for an invalid context (wrong current mapping)") {
    when(reader.getAccountForSsoId(testRequestedSsoId)) thenReturn Stitch.value(
      Some(testRequestedWrongUserId))
    Time.withCurrentTimeFrozen { _ =>
      val signature = client.createSsoSignature(testUserId, testSsoId)
      stitchAwait(
        client
          .validateSsoSignature(signature, testRequestedUserId, testRequestedSsoId).map(
            _.isValid)) must be(false)
    }
  }

  test(
    "SsoSignatureClient#validateSsoSignature returns true for non existing mapping for registration flow") {
    when(reader.getAccountForSsoId(testRequestedSsoId)) thenReturn Stitch.None
    Time.withCurrentTimeFrozen { _ =>
      val signature = client.createSsoSignature(testUserId, testSsoId)
      stitchAwait(
        client
          .validateSsoSignature(signature, testRequestedUserId, testRequestedSsoId).map(
            _.isValid)) must be(true)
    }
  }

}
