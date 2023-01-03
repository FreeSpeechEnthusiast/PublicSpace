package com.twitter.auth.tamperproofing.paseto

import com.twitter.auth.common.EncryptionKeys
import com.twitter.finagle.stats.StatsReceiver
import net.aholbrook.paseto.crypto.exception.ByteArrayLengthException
import net.aholbrook.paseto.exception.PasetoParseException
import net.aholbrook.paseto.exception.SignatureVerificationException
import net.aholbrook.paseto.service.KeyId
import net.aholbrook.paseto.service.LocalTokenService
import scala.util.Failure
import scala.util.Success
import scala.util.Try

// This class allows to create paseto service to encode and decode local/symmetric PASETO token.
// Local/Symmetric PASETOs are encrypted using a secret key. This is very simple abstraction
// layer which takes string data as the input and encode/create and decode/parse the PASETO token

class SymmetricPasetoService(
  encryptionKeys: EncryptionKeys,
  tokenValidityPeriodOpt: Option[Long],
  statsReceiver: StatsReceiver)
    extends PasetoService {
  private[this] val Random = new scala.util.Random
  private[this] val InvalidKeyIdException = new IllegalArgumentException("Invalid key Id")
  private[this] val NonExistentTokenServiceException = new IllegalArgumentException(
    "Non existent token service exception")
  private[this] val FooterParsingException = new IllegalArgumentException("Error parsing footer")

  // List to store encryption enabled keyIds(e.g. if the secret file has 4 entries with id 1,2,3,4
  // and only keyId 3,4 are enabled for encryption, below list will have two entries seq(3,4)
  private[this] val encryptionEnabledKeyIdList: Seq[Int] =
    encryptionKeys.encryptionEnabledKeys.keys.toList
  private[this] val symmetricTokenServices: Map[Int, LocalTokenService[PasetoAuthToken]] =
    loadTokenServices(encryptionKeys, tokenValidityPeriodOpt)
  private[this] val defaultTokenServiceOpt = symmetricTokenServices.headOption

  private[this] val scopedReceiver = statsReceiver.scope("local_paseto_service")
  private[this] val encodeScope = scopedReceiver.scope("encode_token")
  private[this] val encodeTokenCreationCounter = encodeScope.counter("create")
  private[this] val encodeTokenSuccessCounter = encodeScope.counter("success")
  private[this] val encodeTokenFailureCounter = encodeScope.counter("failure")
  private[this] val encodeTokenInvalidKeyFailureCounter = encodeScope.counter("invalid_key_failure")
  private[this] val decodeScope = scopedReceiver.scope("decode_token")
  private[this] val decodeSuccess = decodeScope.counter("success")
  private[this] val decodeFailure = decodeScope.counter("failure")
  private[this] val decodeParseFailureCounter = decodeScope.counter("parse_failure")
  private[this] val decodeInvalidKeyIdFailureCounter = decodeScope.counter("invalid_key")
  private[this] val decodeNonExistentTokenServiceCounter = decodeScope.counter("non_existent_key")
  private[this] val decodeFooterParsingFailureCounter =
    decodeScope.counter("footer_parsing_failure")
  private[this] val decodeSignatureVerificationCounter =
    decodeScope.counter("signature_verification_failure")

  /*
   This method will create local/symmetric paseto token from given data. The secret key used to
   encrypt the data is included in the footer section of the token string. Note that footer section is not
   encrypted so do not send any sensitive data in the footer. Read more on it here go/paseto-intro
   */
  def encodeToken(data: String, inputKeyIdOpt: Option[Int]): Try[String] = {
    encodeTokenCreationCounter.incr()
    val keyIdOpt = getKeyId(encryptionEnabledKeyIdList, inputKeyIdOpt)
    keyIdOpt match {
      case Some(keyId) =>
        symmetricTokenServices.get(keyId) match {
          case Some(localTokenService) =>
            try {
              val authToken = new PasetoAuthToken(data)
              val footer = new KeyId
              val keyIdStr = keyId.toString
              footer.setKeyId(keyIdStr)
              val response = Success(localTokenService.encode(authToken, footer))
              encodeTokenSuccessCounter.incr()
              response
            } catch {
              case e: ByteArrayLengthException =>
                encodeTokenInvalidKeyFailureCounter.incr()
                encodeTokenFailureCounter.incr()
                Failure(e)
              case e: Exception =>
                encodeTokenFailureCounter.incr()
                Failure(e)
            }
          case _ =>
            encodeTokenInvalidKeyFailureCounter.incr()
            Failure(NonExistentTokenServiceException)
        }
      case _ =>
        encodeTokenInvalidKeyFailureCounter.incr()
        Failure(InvalidKeyIdException)
    }
  }

  /*
  This method will decode paseto local token, this method extract keyId from the footer section and
  uses specific local service based on extracted keyId to decode the token.
  Read more on it here go/paseto-intro
   */
  def decodeToken(token: String): Try[String] = {
    val footerOpt: Option[KeyId] = defaultTokenServiceOpt match {
      case Some(defaultTokenService) =>
        //TODO handle ill formed footer case
        Some(defaultTokenService._2.getFooter(token, classOf[KeyId]))
      case _ =>
        decodeInvalidKeyIdFailureCounter.incr()
        None
    }

    footerOpt match {
      case Some(footer) =>
        symmetricTokenServices.get(footer.getKeyId.toInt) match {
          case Some(localTokenService) =>
            try {
              val response = Success(localTokenService.decode(token, footer).getData())
              decodeSuccess.incr()
              response
            } catch {
              case e: PasetoParseException =>
                decodeParseFailureCounter.incr()
                Failure(e)
              case e: SignatureVerificationException =>
                decodeSignatureVerificationCounter.incr()
                Failure(e)
              case e: Throwable =>
                decodeFailure.incr()
                Failure(e)
            }
          case _ =>
            decodeNonExistentTokenServiceCounter.incr()
            Failure(NonExistentTokenServiceException)
        }
      case _ =>
        decodeFooterParsingFailureCounter.incr()
        Failure(FooterParsingException)
    }
  }

  private[this] def getKeyId(
    availableKeys: Seq[Int],
    inputKeyIdOpt: Option[Int]
  ): Option[Int] = {
    inputKeyIdOpt match {
      case Some(keyId) =>
        // If keyId is provided, validate if we have key Id available
        availableKeys.find(k => k == keyId)
      case _ =>
        // If key Id is not provided, pick random key Id from available keys
        Some(availableKeys(Random.nextInt(availableKeys.size)))
    }
  }

  // This method will create paseto symmetric tokens services. Paseto only supports one service per
  // secret key so we will need to create multiple token services based on available encryption
  // keys.
  private[this] def loadTokenServices(
    keys: EncryptionKeys,
    tokenValidityPeriodOpt: Option[Long]
  ): Map[Int, LocalTokenService[PasetoAuthToken]] = {
    keys.encryptionEnabledKeys map {
      case (k, v) =>
        (
          k,
          PasetoUtils.pasetoV2LocalTokenService(
            privateKey = v.getKey,
            validityPeriodOpt = tokenValidityPeriodOpt
          ))
    }
  }
}
