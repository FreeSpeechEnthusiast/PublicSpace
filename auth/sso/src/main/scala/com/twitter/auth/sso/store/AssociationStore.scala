package com.twitter.auth.sso.store

import com.twitter.auth.sso.client.{
  SsoGuanoScriber,
  SsoInfoEncryptor,
  StratoSsoInfoDeleter,
  StratoSsoInfoFetcher,
  StratoSsoInfoForUserScanner,
  StratoSsoInfoWriter,
  StratoSsoUsersForSsoIdScanner
}
import com.twitter.auth.sso.models.{AssociationMethod, Email, SsoId, SsoInfo, SsoProvider, UserId}
import com.twitter.stitch.Stitch
import com.twitter.guano.thriftscala._
import com.twitter.util.Time

case class TooManyUserForSsoId(ssoId: SsoId, numUsers: Int)
    extends Exception(s"We found $numUsers Users associated with SsoId: $ssoId") {}

/**
 * Handles writes to associate or disassociate a [[UserId]] from a [[SsoId]].
 */
class AssociationWriter(
  encryptor: SsoInfoEncryptor,
  stratoSsoInfoWriter: StratoSsoInfoWriter,
  stratoSsoInfoDeleter: StratoSsoInfoDeleter) {

  /**
   * Writes the association to the association store. The store is responsible for ensurin
   * the values are written in the correct format and are encrypted.
   */
  def writeAssociation(
    twitterUserId: UserId,
    ssoId: SsoId,
    ssoEmail: Email,
    ssoProvider: SsoProvider,
    associationMethod: AssociationMethod
  ): Stitch[Unit] = {

    val info = SsoInfo(
      time = Time.now,
      ssoId = ssoId,
      ssoEmail = ssoEmail,
      associationMethod = associationMethod
    )
    stratoSsoInfoWriter
      .write(twitterUserId, ssoProvider, encryptor.encrypt(info)).applyEffect {
        case _ =>
          SsoGuanoScriber
            .scribe(
              ScribeMessage(
                `type` = ScribeType.SingleSignOnNotification,
                singleSignOnNotification = Some(
                  SingleSignOnNotification(
                    Time.now.inSeconds,
                    twitterUserId,
                    Some(ssoId),
                    Some(encryptor.hashSsoEmail(ssoEmail)),
                    Some(SsoEvent.SsoAssociation)
                  )
                )
              ))
          Stitch.Unit
      }
  }

  def disassociateFromAccount(
    twitterUserId: UserId,
    ssoProvider: SsoProvider
  ): Stitch[Unit] = {
    stratoSsoInfoDeleter
      .delete(twitterUserId, ssoProvider).applyEffect {
        case _ =>
          SsoGuanoScriber
            .scribe(
              ScribeMessage(
                `type` = ScribeType.SingleSignOnNotification,
                singleSignOnNotification = Some(
                  SingleSignOnNotification(
                    Time.now.inSeconds,
                    twitterUserId,
                    Some(""),
                    Some(""),
                    Some(SsoEvent.SsoDissociation)
                  )
                )
              ))
          Stitch.Unit
      }
  }
}

/**
 * Handles reads mapping the [[SsoId]] to a [[UserId]] and retrieving [[SsoInfo]].
 */
class AssociationReader(
  encryptor: SsoInfoEncryptor,
  stratoSsoInfoForUserScanner: StratoSsoInfoForUserScanner,
  stratoSsoUsersForSsoIdScanner: StratoSsoUsersForSsoIdScanner,
  stratoSsoInfoFetcher: StratoSsoInfoFetcher) {

  /**
   * Scans the SSO store to get all SSOIds under a userId.
   */
  def getSsoAccounts(twitterUserId: UserId): Stitch[Seq[SsoId]] = {
    getSsoInfo(twitterUserId).map(_.map(_.ssoId))
  }

  /**
   * Scans the SSO store to get SSO Info
   */
  def getSsoInfo(twitterUserId: UserId): Stitch[Seq[SsoInfo]] = {
    stratoSsoInfoForUserScanner.scan(twitterUserId).map(_.map(encryptor.decrypt))
  }

  /**
   * Queries the SSO ID to UserId index to get the associated UserId. Because SsoIds are stored
   * using a secondary index, we have to retrieve these with a scan operation. It's possible
   * that their may be more than one user associated with the SsoId if two [[SsoProvider]]s return
   * the same SsoId. If this happens, we will return a [[TooManyUserForSsoId]] exception.
   *
   * In the future, we may want to dedupe by SsoProvider.
   */
  def getAccountForSsoId(ssoId: SsoId): Stitch[Option[Long]] = {
    stratoSsoUsersForSsoIdScanner.scan(encryptor.hashSsoId(ssoId)).map { ids =>
      if (ids.length > 1) {
        throw TooManyUserForSsoId(ssoId, ids.length)
      }
      ids.headOption
    }
  }

  /**
   * Returns the SsoInfo for a particuler User/Provider pair. There is a limit of at most one
   * SsoInfos per pair.
   */
  def getInfo(
    twitterUserId: UserId,
    ssoProvider: SsoProvider
  ): Stitch[Option[SsoInfo]] = {
    stratoSsoInfoFetcher.fetch(twitterUserId, ssoProvider).map(_.map(encryptor.decrypt))
  }
}
