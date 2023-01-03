package com.twitter.auth.sso.signature

import com.twitter.auth.sso.models.{SsoId, UserId, SsoSignature}
import com.twitter.auth.sso.store.AssociationReader
import com.twitter.stitch.Stitch

case class SignatureValidationContext(
  signatureData: SsoSignatureData,
  signatureHash: String,
  requestedUserId: UserId,
  requestedSsoId: SsoId,
  associationReader: AssociationReader)

sealed trait SignatureValidatorResult {
  def isValid: Boolean
}

trait SignatureValidator extends (SignatureValidationContext => Stitch[SignatureValidatorResult])

object SignatureValidatorResult {
  case object Valid extends SignatureValidatorResult {
    override val isValid: Boolean = true
  }
  case class Invalid(validator: SignatureValidator, reason: String)
      extends SignatureValidatorResult {
    override val isValid: Boolean = false

    override val toString: SsoId =
      s"Signature Invalid using validator ${validator.getClass.getSimpleName} w/ reason ${reason}"
  }

  case class ParserFailed(signature: SsoSignature, userId: UserId, ssoId: SsoId)
      extends SignatureValidatorResult {
    override val isValid: Boolean = false
  }

  val ValidSignatureStitch: Stitch[SignatureValidatorResult] = Stitch.value(Valid)
}
