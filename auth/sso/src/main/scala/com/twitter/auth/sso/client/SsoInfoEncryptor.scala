package com.twitter.auth.sso.client

import com.twitter.account_security.utils.AesCompactThriftStructEncryptor
import com.twitter.appsec.crypto.{AesGcm, HmacSha256}
import com.twitter.auth.sso.models.{AssociationMethod, SsoId, SsoInfo}
import com.twitter.auth.sso.thriftscala.{SsoMeta, SsoMetaV1, SsoInfo => TSsoInfo}
import com.twitter.util.Time
import org.apache.commons.codec.binary.Base64

case object UnhandledSsoMetaInfo extends Exception

class SsoInfoEncryptor(
  keyMap: Map[Int, String],
  keyVersion: Int) {

  lazy val aesThriftStructEncryptor: AesCompactThriftStructEncryptor[SsoMeta] =
    AesCompactThriftStructEncryptor(SsoMeta, keyMap, keyVersion)

  private val hashKey = keyMap(keyVersion)

  def hashSsoId(ssoId: SsoId): String = {
    Base64.encodeBase64String(HmacSha256(hashKey, ssoId).bytes)
  }

  def hashSsoEmail(ssoEmail: String): String = {
    Base64.encodeBase64String(HmacSha256(hashKey, ssoEmail).bytes)
  }

  def encrypt(ssoInfo: SsoInfo): TSsoInfo = {
    val ssoMeta = SsoMeta.V1(
      SsoMetaV1(
        ssoId = ssoInfo.ssoId,
        ssoEmail = ssoInfo.ssoEmail,
        creationTimeMs = ssoInfo.time.inMilliseconds,
        associationMethod = AssociationMethod.toThrift(ssoInfo.associationMethod),
        hashKeyVersion = keyVersion
      )
    )

    TSsoInfo(
      ssoIdHash = hashSsoId(ssoInfo.ssoId),
      meta = aesThriftStructEncryptor.encrypt(ssoMeta)
    )
  }

  def decrypt(ssoInfo: TSsoInfo): SsoInfo = {

    val meta = aesThriftStructEncryptor.decrypt(ssoInfo.meta) match {
      case SsoMeta.V1(m) => m
      case _ => throw UnhandledSsoMetaInfo
    }
    SsoInfo(
      ssoId = meta.ssoId,
      ssoEmail = meta.ssoEmail,
      associationMethod = AssociationMethod.fromThrift(meta.associationMethod),
      time = Time.fromMilliseconds(meta.creationTimeMs)
    )
  }
}

object SsoInfoEncryptor {
  def apply(
    secretKey: String
  ): SsoInfoEncryptor = {
    val keyMap = Map(AesGcm.DefaultKeyVersionIdentifier -> secretKey)
    new SsoInfoEncryptor(keyMap, AesGcm.DefaultKeyVersionIdentifier)
  }
}
