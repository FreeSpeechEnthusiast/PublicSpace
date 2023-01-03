package com.twitter.auth.tamperproofing.paseto

import net.aholbrook.paseto.meta.PasetoBuilders
import net.aholbrook.paseto.service.{LocalTokenService, PublicTokenService}

object PasetoUtils {

  val DefaultValidityPeriod: Long = java.time.Duration.ofMinutes(15).getSeconds

  def pasetoTokenService(
    privateKey: Array[Byte],
    publicKey: Array[Byte],
    validityPeriod: Long = DefaultValidityPeriod
  ): PublicTokenService[PasetoAuthToken] = {
    val provider: PublicTokenService.KeyProvider = new PublicTokenService.KeyProvider() {
      override def getSecretKey: Array[Byte] = privateKey
      override def getPublicKey: Array[Byte] = publicKey
    }

    PasetoBuilders.V2
      .publicService(provider, classOf[PasetoAuthToken]).withDefaultValidityPeriod(
        DefaultValidityPeriod).build
  }

  def pasetoV2LocalTokenService(
    privateKey: Array[Byte],
    validityPeriodOpt: Option[Long] = Some(DefaultValidityPeriod)
  ): LocalTokenService[PasetoAuthToken] = {
    val provider = new LocalTokenService.KeyProvider() {
      override def getSecretKey: Array[Byte] = privateKey
    }
    val service = PasetoBuilders.V2
      .localService(provider, classOf[PasetoAuthToken])

    validityPeriodOpt match {
      case Some(validityPeriod) =>
        service.withDefaultValidityPeriod(validityPeriod)
      case _ =>
    }
    service.build
  }

}
