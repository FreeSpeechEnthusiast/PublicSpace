package com.twitter.auth.sso.service

import com.twitter.auth.sso.signature.{SignatureValidatorResult, SsoSignatureClient}
import com.twitter.auth.sso.models._
import com.twitter.auth.sso.store.{AssociationReader, AssociationWriter}
import com.twitter.auth.sso.util.Monitor
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.stitch.Stitch

/**
 * This class is meant to be embedded as a library into an arbitrary service. It's responsible
 * for:
 *
 * 1. Performing associations between the [[SsoId]] and the Twitter User.
 * 2. Generating secure JWTs and exchanging them for First Step Tokens.
 */
class SingleSignOnService(
  associationWriter: AssociationWriter,
  associationReader: AssociationReader,
  ssoSignatureClient: SsoSignatureClient,
  statsReceiver: StatsReceiver = NullStatsReceiver) {

  private val scopedStatsReceiver: StatsReceiver = statsReceiver.scope("single_sign_on_service")
  private val associateToAccountMonitor = Monitor(scopedStatsReceiver.scope("associate"))
  private val disassociateFromAccountMonitor = Monitor(scopedStatsReceiver.scope("disassociate"))
  private val getSsoAccountsMonitor = Monitor(scopedStatsReceiver.scope("get_sso_accounts"))
  private val getAccountForSsoIdMonitor = Monitor(
    scopedStatsReceiver.scope("get_account_for_sso_id"))
  private val getInfoMonitor = Monitor(scopedStatsReceiver.scope("get_info"))
  private val validateSignatureScope = scopedStatsReceiver.scope("validate_signature")
  private val validateSignatureMonitor = Monitor(validateSignatureScope)

  /**
   * Called after a succesful SSO login or signup to create the mapping between the userId and the SSOID.
   *
   * Performs the following:
   *  1. ensure the association meets eligibility requirements:
   *    - The user has not opted out of SSO association
   *  2. Writes the association via the Association Store
   *
   * TODO (ACCTSEC-10505):
   *  - Add logging (e.g. guano)
   *
   * @return True if the mapping is successfully created.
   */
  def associateToAccount(
    twitterUserId: UserId,
    ssoId: SsoId,
    ssoEmail: Email,
    ssoProvider: SsoProvider,
    associationMethod: AssociationMethod
  ): Stitch[Boolean] = {
    associateToAccountMonitor.trackStitch(
      associationWriter
        .writeAssociation(
          twitterUserId = twitterUserId,
          ssoId = ssoId,
          ssoEmail = ssoEmail,
          ssoProvider = ssoProvider,
          associationMethod = associationMethod
        ).map(_ => true)
    )
  }

  /**
   * Handle the user opting out of a particular SSO method.
   *
   * TODO (ACCTSEC-10505):
   *  - Add logging (e.g. guano)
   *
   * @return  True if the mapping is successfully destroyed.
   */
  def disassociateFromAccount(
    twitterUserId: UserId,
    ssoProvider: SsoProvider
  ): Stitch[Boolean] =
    disassociateFromAccountMonitor.trackStitch(
      associationWriter
        .disassociateFromAccount(
          twitterUserId = twitterUserId,
          ssoProvider = ssoProvider
        ).map(_ => true)
    )

  /**
   * Returns all [[SsoId]]s associated with a Twitter UserId.
   */
  def getSsoAccounts(twitterUserId: UserId): Stitch[Seq[SsoId]] =
    getSsoAccountsMonitor.trackStitchSeq(
      associationReader.getSsoAccounts(twitterUserId)
    )

  /**
   * Returns all [[SsoInfo]]s associated with a Twitter UserId.
   */
  def getSsoInfo(twitterUserId: UserId): Stitch[Seq[SsoInfo]] =
    getSsoAccountsMonitor.trackStitchSeq(
      associationReader.getSsoInfo(twitterUserId)
    )

  /**
   * Return the Twitter UserId (if any) associated with an SSOId
   */
  def getAccountForSsoId(ssoId: SsoId): Stitch[Option[Long]] =
    getAccountForSsoIdMonitor.trackStitch(
      associationReader.getAccountForSsoId(ssoId)
    )

  /**
   * Returns the SsoInfo for a particuler User/Provider pair.
   */
  def getInfo(
    twitterUserId: UserId,
    ssoProvider: SsoProvider
  ): Stitch[Option[SsoInfo]] = {
    getInfoMonitor.trackStitch(
      associationReader.getInfo(
        twitterUserId = twitterUserId,
        ssoProvider = ssoProvider
      )
    )
  }

  /**
   * Creates an [[SsoSignature]].
   */
  def createSsoSignature(userId: UserId, ssoId: SsoId): SsoSignature =
    ssoSignatureClient.createSsoSignature(userId, ssoId)

  /**
   * Validates an sso request signature
   */
  def validateSsoRequestSignature(
    requestedUserId: UserId,
    requestedSsoId: SsoId,
    requestedSsoSignature: SsoSignature
  ): Stitch[SignatureValidatorResult] = {
    validateSignatureMonitor
      .trackStitch(
        ssoSignatureClient
          .validateSsoSignature(
            signature = requestedSsoSignature,
            requestedUserId = requestedUserId,
            requestedSsoId = requestedSsoId
          )).onSuccess { result =>
        validateSignatureScope.counter("result", result.getClass.getSimpleName).incr()
      }
  }

}
