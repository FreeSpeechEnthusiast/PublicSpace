package com.twitter.auth.audit

import com.twitter.finagle.thrift.ClientId
import com.twitter.guano.thriftscala._
import com.twitter.logging.Logger
import com.twitter.util.Time

/*
 * Threadsafe
 */
class AuthTokenAudit(guanoScriber: AuthGuanoScriber) {

  private val OAUTH_TOKEN_TYPE = "OAUTH"

  def scribeAccessTokenCreation(
    userId: Long,
    clientAppId: Long,
    tokenManagerService: String,
    auditLoggingMessage: String,
    tokenType: String = OAUTH_TOKEN_TYPE,
    logger: Logger
  ): Unit = {
    val noteToScribe = s"App ID: $clientAppId and token type:$tokenType"
    scribeSettingsAction(
      userId = userId,
      clientAppId = clientAppId,
      action = SettingsActionAction.GrantAppAccess,
      note = Option(noteToScribe),
      tokenManagerService = tokenManagerService)
    scribeAudit(logger, auditLoggingMessage);
  }

  def scribeAccessTokenRevocation(
    userId: Long,
    clientAppId: Long,
    byUserId: Option[Long] = None,
    notes: Option[String] = None,
    tokenManagerService: String,
    auditLoggingMessage: String,
    tokenType: String = OAUTH_TOKEN_TYPE,
    logger: Logger
  ): Unit = {
    val noteToScribe = notes.getOrElse(s"App ID: $clientAppId and token type:$tokenType")
    // If the byUserId is present that means the revocation action is take by some user and we
    // want to scribe user action instead of scribing settings action
    if (byUserId.isDefined) {
      scribeUserAction(
        userId = userId,
        byUserId = byUserId,
        action = UserActionAction.Note,
        reason = UserActionReason.CompromisedUser,
        note = Some(noteToScribe),
        tokenManagerService = tokenManagerService
      )
    } else {
      scribeSettingsAction(
        userId = userId,
        clientAppId = clientAppId,
        action = SettingsActionAction.RevokeAppAccess,
        note = Some(noteToScribe),
        tokenManagerService = tokenManagerService)
    }
    scribeAudit(logger, auditLoggingMessage);
  }

  def scribeAccessTokenUninvalidate(
    userId: Long,
    clientAppId: Long,
    tokenManagerService: String,
    auditLoggingMessage: String,
    tokenType: String = OAUTH_TOKEN_TYPE,
    logger: Logger
  ): Unit = {
    val noteToScribe = s"Uninvalidate. App ID: $clientAppId and token type:$tokenType"
    scribeSettingsAction(
      userId = userId,
      clientAppId = clientAppId,
      action = SettingsActionAction.GrantAppAccess,
      note = Option(noteToScribe),
      tokenManagerService = tokenManagerService)
    scribeAudit(logger, auditLoggingMessage);
  }

  def scribeUserActionTokensDeletion(
    accessTokens: Int,
    tokenType: String,
    distinctClientAppIds: Int,
    userId: Long,
    byUserId: Option[Long],
    note: Option[String],
    reason: UserActionReason,
    tokenManagerService: String
  ): Unit = {
    val noteToScribe =
      s"Revoked $accessTokens $tokenType tokens across " +
        s"$distinctClientAppIds apps. ${note.getOrElse("")}"
    scribeUserAction(
      userId = userId,
      byUserId = byUserId,
      action = UserActionAction.Note,
      reason = reason,
      note = Option(noteToScribe),
      tokenManagerService = tokenManagerService)
  }

  private[this] def scribeSettingsAction(
    userId: Long,
    clientAppId: Long,
    action: SettingsActionAction,
    note: Option[String] = None,
    tokenManagerService: String
  ): Unit = {
    userId match {
      case 0 =>
      // FES-1447: ignoring app only tokens
      case _ =>
        guanoScriber.scribe(
          ScribeMessage(
            ScribeType.SettingsAction,
            settingsAction = Some(
              SettingsAction(
                Time.now.inSeconds,
                Some(tokenManagerService),
                userId,
                byUserId = Some(userId),
                action = action,
                reason = Some(UserActionReason.Other),
                done = Some(true),
                actionInt = Some(clientAppId.toInt),
                runId = ClientId.current.map(_.name),
                note = note
              )
            )
          )
        )
    }
  }

  private[this] def scribeUserAction(
    userId: Long,
    byUserId: Option[Long],
    action: UserActionAction,
    reason: UserActionReason,
    note: Option[String],
    tokenManagerService: String
  ): Unit = {
    guanoScriber.scribe(
      ScribeMessage(
        ScribeType.UserAction,
        userAction = Some(
          UserAction(
            Time.now.inSeconds,
            Some(tokenManagerService),
            userId,
            byUserId = byUserId,
            action = action,
            reason = reason,
            note = note
          )
        )
      )
    )
  }

  private[this] def scribeAudit(
    logger: Logger,
    auditLoggingMessage: String
  ): Unit = {
    logger.info(auditLoggingMessage);
  }
}
