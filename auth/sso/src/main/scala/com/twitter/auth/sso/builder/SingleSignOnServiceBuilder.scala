package com.twitter.auth.sso.builder

import com.twitter.auth.sso.client._
import com.twitter.auth.sso.service.SingleSignOnService
import com.twitter.auth.sso.store.{AssociationReader, AssociationWriter}
import com.twitter.auth.sso.signature.SsoSignatureClient
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.strato.client.Client
import com.twitter.strato.generated.client.auth.sso.{
  SsoInfoBySsoIdClientColumn,
  SsoInfoByUserIdClientColumn
}
import com.twitter.util.security.Credentials
import java.io.File

case object FailedToLoadSecrets extends Exception

object SingleSignOnServiceBuilder {

  /**
   * Location of secrets used for the following:
   *  1. Encrypting the [[com.twitter.auth.sso.models.SsoInfo]] that we store for user/ssoId associations
   *  2. Creating the SsoIdHash that we key on for lookups by SsoId.
   *  3. Creating the the [[com.twitter.auth.sso.models.SsoSignature]] that passbird will verify when creating first step tokens
   *
   * Any service that uses the library for any of the above methods will need to be added to the
   * the Tss Material metadata.
   *
   * The Tss can contain multiple versions of the secrets. However, at this time,
   * only one version is supported. In the future, we may choose to support multiple concurrent
   * versions. This field is the cannonical current version.
   *
   * Note:
   *  Updating this may break the ability to perform and hashing or encrypting operation
   *  supported by the library. Don't update unless you know what you're doing.
   */
  final val TssCredentialPath = "/var/lib/tss/keys/auth/sso/sso_secrets"
  final val TssCredentialEncryptionSecretFieldName = "sso_secret"
  final val TssCredentialSignatureSecretFieldName = "sso_signature"

  def buildSingleSignOnService(
    stratoClient: Client,
    statsReceiver: StatsReceiver,
    secretFileName: String = TssCredentialPath,
    encryptionSecretFieldName: String = TssCredentialEncryptionSecretFieldName,
    ssoSignatureSecretFieldName: String = TssCredentialSignatureSecretFieldName
  ): SingleSignOnService = {

    val credentials = Credentials(new File(secretFileName))

    val encryptor = SsoInfoEncryptor(credentials(encryptionSecretFieldName))

    val ssoInfoByUserIdClientColumn = new SsoInfoByUserIdClientColumn(stratoClient)
    val ssoInfoBySsoIdClientColumn = new SsoInfoBySsoIdClientColumn(stratoClient)

    val associationWriter = new AssociationWriter(
      encryptor = encryptor,
      stratoSsoInfoWriter = new StratoSsoInfoWriter(ssoInfoByUserIdClientColumn, statsReceiver),
      stratoSsoInfoDeleter = new StratoSsoInfoDeleter(ssoInfoByUserIdClientColumn, statsReceiver)
    )
    val associationReader = new AssociationReader(
      encryptor = encryptor,
      stratoSsoInfoForUserScanner =
        new StratoSsoInfoForUserScanner(ssoInfoByUserIdClientColumn, statsReceiver),
      stratoSsoUsersForSsoIdScanner =
        new StratoSsoUsersForSsoIdScanner(ssoInfoBySsoIdClientColumn, statsReceiver),
      stratoSsoInfoFetcher = new StratoSsoInfoFetcher(ssoInfoByUserIdClientColumn, statsReceiver)
    )

    val ssoSignatureClient =
      SsoSignatureClient(
        secretKey = credentials(ssoSignatureSecretFieldName),
        contextAssociationReader = associationReader
      )

    new SingleSignOnService(
      associationWriter = associationWriter,
      associationReader = associationReader,
      ssoSignatureClient = ssoSignatureClient,
      statsReceiver = statsReceiver
    )
  }
}
