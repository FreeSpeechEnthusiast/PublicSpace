package com.twitter.auth.sso.signature

import com.twitter.appsec.crypto.{AesGcm, HmacSha256}
import com.twitter.auth.sso.signature.SsoSignatureClient.{
  SsoDataDelimiter,
  SsoSignatureValidFor,
  parseSignature
}
import com.twitter.auth.sso.models.{SsoId, SsoSignature, UserId}
import com.twitter.auth.sso.signature.SignatureValidatorResult._
import com.twitter.auth.sso.store.AssociationReader
import com.twitter.stitch.Stitch
import com.twitter.util.{Duration, Throw, Time, Try}
import org.apache.commons.codec.binary.Base64

case class SsoSignatureData(version: Int, userId: UserId, ssoId: SsoId, time: Time) {

  /**
   * Converts the SsoSignatureData to a string separated by [[SsoSignatureClient.SsoDataDelimiter]]
   *
   * Order is important here! The current data ordering is:
   * <signature version>.<userId>.<base 64 encoded ssoId>.<time_ms>
   */
  override def toString: String =
    Seq(version, userId, Base64.encodeBase64String(ssoId.getBytes), time.inMilliseconds)
      .mkString(SsoDataDelimiter.toString)
}

case class InvalidSignatureException(signature: SsoSignature)
    extends Exception(s"Invalid SsoSignature: $signature")
case class SignatureParsingException(signature: SsoSignature)
    extends Exception(s"Failed top parse SsoSignature: $signature")
case class InvalidKeyVersion(version: Int) extends Exception(s"Key version $version does not exist")

class SsoSignatureClient(
  keyMap: Map[Int, String],
  // TODO (AUTHPLT-1697): refactor/improve key rotation
  currentKeyVersion: Int,
  contextAssociationReader: AssociationReader) {

  def hashData(data: SsoSignatureData): String = {
    val hashKey = keyMap.getOrElse(data.version, throw InvalidKeyVersion(data.version))

    Base64.encodeBase64String(HmacSha256(hashKey, data.toString).bytes)
  }

  /**
   * Creates an [[SsoSignature]].
   *
   * I looked into using Paseto, such as [[com.twitter.auth.tamperproofing.paseto.PasetoDataSigner]],
   * but this felt like overkill for the current use case. The SsoSignature format is versioned,
   * should we need to change it in the future, we can.
   */
  def createSsoSignature(userId: UserId, ssoId: SsoId): SsoSignature = {
    val data = SsoSignatureData(currentKeyVersion, userId, ssoId, Time.now)
    val signature = hashData(data)

    Seq(data.toString, signature).mkString(SsoDataDelimiter.toString)
  }

  private def stitchAnd(
    a: Stitch[SignatureValidatorResult],
    b: Stitch[SignatureValidatorResult]
  ): Stitch[SignatureValidatorResult] = {
    Stitch.partialJoinMap(a, b) {
      case (Some(Valid), Some(Valid)) => Some(Valid)
      case (Some(reason: Invalid), _) => Some(reason)
      case (_, Some(reason: Invalid)) => Some(reason)
      case _ => None
    }
  }

  /**
   * Validates an SSO signature with the following checks:
   *  - The signature is generated for requested mapping
   *  - The signature is valid.
   *  - The creation time was created after Time.now - [[SsoSignatureClient.SsoSignatureValidFor]]
   */
  def validateSsoSignature(
    signature: SsoSignature,
    requestedUserId: UserId,
    requestedSsoId: SsoId
  ): Stitch[SignatureValidatorResult] = {
    parseSignature(signature).toOption match {
      case Some((data, hash)) =>
        // Recursively merge stitches with "AND" condition
        Seq(WithinRequestedMapping, WithinWindow, HashMatches, WithinActualMapping)
          .foldLeft(ValidSignatureStitch) { (leftCondition, rightCondition) =>
            stitchAnd(
              leftCondition,
              rightCondition(
                SignatureValidationContext(
                  data,
                  hash,
                  requestedUserId,
                  requestedSsoId,
                  contextAssociationReader)))
          }
      case _ => Stitch.value(ParserFailed(signature, requestedUserId, requestedSsoId))
    }
  }

  /**
   * Verifies that the signature was created for specific requested mapping.
   */
  case object WithinRequestedMapping extends SignatureValidator {
    override def apply(
      context: SignatureValidationContext
    ): Stitch[SignatureValidatorResult] =
      if (context.signatureData.userId != context.requestedUserId) {
        Stitch.value(Invalid(
          this,
          s"Requested User ID ${context.requestedUserId} did not match User ID ${context.signatureData.userId} in signature."))
      } else if (context.signatureData.ssoId != context.requestedSsoId) {
        Stitch.value(Invalid(
          this,
          s"Requested SSO ID ${context.requestedSsoId} did not match SSO ID ${context.signatureData.ssoId} in signature."))
      } else {
        ValidSignatureStitch
      }
  }

  /**
   * Verifies that the signature was created within the allowable time window.
   */
  case object WithinWindow extends SignatureValidator {
    override def apply(
      context: SignatureValidationContext
    ): Stitch[SignatureValidatorResult] =
      if (context.signatureData.time > Time.now - SsoSignatureValidFor) {
        ValidSignatureStitch
      } else {
        Stitch.value(Invalid(this, s"Signature Time not within time window"))
      }
  }

  /**
   * Verifies the signature matches the sso data.
   */
  case object HashMatches extends SignatureValidator {
    override def apply(
      context: SignatureValidationContext
    ): Stitch[SignatureValidatorResult] = {
      val hashedSignatureData = hashData(context.signatureData)
      if (context.signatureHash.equals(hashedSignatureData)) {
        ValidSignatureStitch
      } else {
        Stitch.value(Invalid(
          this,
          s"Signature Hash ${context.signatureHash} does not match hashed data ${context.signatureData}"))
      }
    }
  }

  /**
   * Verifies that the signature was created for specific requested mapping
   * or the mapping is not yet created (required for registration flow)
   */
  case object WithinActualMapping extends SignatureValidator {
    override def apply(
      context: SignatureValidationContext
    ): Stitch[SignatureValidatorResult] = {
      context.associationReader
        .getAccountForSsoId(context.requestedSsoId)
        .map {
          case Some(userId) if (userId != context.requestedUserId) =>
            Invalid(
              this,
              s"Associated ${userId} does not match requested user id ${context.requestedUserId}")
          case _ => Valid
        }
    }
  }

}

object SsoSignatureClient {
  final val SsoSignatureValidFor: Duration = Duration.fromSeconds(30)
  final val SsoDataDelimiter: Char = '.'

  /**
   * Create a [[SsoSignatureClient]] class. It's responsible for
   * 1) SSO signature creation
   * 2) SSO signature validation within validation context
   *
   * @param secretKey signature encryption key
   * @param contextAssociationReader provides access to context's storage for signature context validation (i.e current userId - ssoId mapping)
   */
  def apply(
    secretKey: String,
    contextAssociationReader: AssociationReader
  ): SsoSignatureClient = {
    val keyMap = Map(AesGcm.DefaultKeyVersionIdentifier -> secretKey)
    new SsoSignatureClient(
      keyMap = keyMap,
      currentKeyVersion = AesGcm.DefaultKeyVersionIdentifier,
      contextAssociationReader = contextAssociationReader
    )
  }

  def parseSignature(signature: SsoSignature): Try[(SsoSignatureData, String)] = {
    signature.split(SsoDataDelimiter) match {
      case Array(version, userId, ssoId, timeMs, hash) =>
        Try(
          SsoSignatureData(
            version.toInt,
            userId.toLong,
            new String(Base64.decodeBase64(ssoId)),
            Time.fromMilliseconds(timeMs.toLong)
          ))
          .map(data => (data, hash))
          .handle {
            case _: NumberFormatException => throw SignatureParsingException(signature)
          }
      case _ =>
        Throw(InvalidSignatureException(signature))
    }
  }
}
