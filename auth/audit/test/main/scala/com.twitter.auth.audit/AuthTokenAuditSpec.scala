package scala.com.twitter.auth.audit

import com.twitter.auth.audit.{AuthGuanoScriber, AuthTokenAudit}
import com.twitter.finagle.thrift.ClientId
import com.twitter.guano.thriftscala._
import com.twitter.logging.Logger
import com.twitter.util.Time
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{verify, _}
import org.scalatest.{FunSuite, MustMatchers, OneInstancePerTest}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class AuthTokenAuditSpec
    extends FunSuite
    with OneInstancePerTest
    with MockitoSugar
    with MustMatchers {

  private val agentTokenRevokeNote = "notes"
  private val agentUserId = 1L
  private val tokenUserId = 2L
  private val appOnlyTokenUserId = 0L
  private val tokenAppId = 3L
  private val tokenManagerService = "tokenManagerService"
  private val auditMessage = "auditMessage"
  private val logger = Logger.get("testLogger")

  private def createScribeMessage(
    settingsAction: SettingsActionAction,
    note: String
  ): ScribeMessage = {
    ScribeMessage(
      ScribeType.SettingsAction,
      settingsAction = Some(
        SettingsAction(
          Time.now.inSeconds,
          Some(tokenManagerService),
          tokenUserId,
          byUserId = Some(tokenUserId),
          action = settingsAction,
          reason = Some(UserActionReason.Other),
          done = Some(true),
          actionInt = Some(tokenAppId.toInt),
          runId = ClientId.current.map(_.name),
          note = Option(note)
        )
      )
    )
  }

  private def createUserActionScribeMessage(
    userId: Long,
    byUserId: Option[Long],
    action: UserActionAction,
    reason: UserActionReason,
    note: Option[String]
  ): ScribeMessage = {
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
  }

  test("scribe app only tokens - creation") {
    val guanoScriber = mock[AuthGuanoScriber]
    val authTokenAudit = new AuthTokenAudit(guanoScriber)

    authTokenAudit.scribeAccessTokenCreation(
      userId = appOnlyTokenUserId,
      clientAppId = tokenAppId,
      tokenManagerService = tokenManagerService,
      auditLoggingMessage = auditMessage,
      logger = logger)

    verify(guanoScriber, never()).scribe(any[ScribeMessage])
  }

  test("scribe app only tokens - revocation") {
    val guanoScriber = mock[AuthGuanoScriber]
    val authTokenAudit = new AuthTokenAudit(guanoScriber)

    authTokenAudit.scribeAccessTokenRevocation(
      userId = appOnlyTokenUserId,
      clientAppId = tokenAppId,
      tokenManagerService = tokenManagerService,
      auditLoggingMessage = auditMessage,
      logger = logger)

    verify(guanoScriber, never()).scribe(any[ScribeMessage])
  }

  test("scribe user tokens - creation") {
    Time.withCurrentTimeFrozen { _ =>
      val scribeMessage =
        createScribeMessage(
          SettingsActionAction.GrantAppAccess,
          s"App ID: ${tokenAppId} and token type:OAUTH")
      val guanoScriber = mock[AuthGuanoScriber]
      val authTokenAudit = new AuthTokenAudit(guanoScriber)

      authTokenAudit.scribeAccessTokenCreation(
        userId = tokenUserId,
        clientAppId = tokenAppId,
        tokenManagerService = tokenManagerService,
        auditLoggingMessage = auditMessage,
        logger = logger)

      verify(guanoScriber, times(1)).scribe(scribeMessage)
    }
  }

  test("scribe user tokens - revocation") {
    Time.withCurrentTimeFrozen { _ =>
      val scribeMessage =
        createScribeMessage(
          SettingsActionAction.RevokeAppAccess,
          s"App ID: ${tokenAppId} and token type:OAUTH")

      val guanoScriber = mock[AuthGuanoScriber]
      val authTokenAudit = new AuthTokenAudit(guanoScriber)

      authTokenAudit.scribeAccessTokenRevocation(
        userId = tokenUserId,
        clientAppId = tokenAppId,
        tokenManagerService = tokenManagerService,
        auditLoggingMessage = auditMessage,
        logger = logger)

      verify(guanoScriber, times(1)).scribe(scribeMessage)
    }
  }

  test("scribe user tokens - revocation by agent user") {
    Time.withCurrentTimeFrozen { _ =>
      val scribeMessage = createUserActionScribeMessage(
        userId = tokenUserId,
        byUserId = Some(agentUserId),
        action = UserActionAction.Note,
        reason = UserActionReason.CompromisedUser,
        note = Some(agentTokenRevokeNote))

      val guanoScriber = mock[AuthGuanoScriber]
      val authTokenAudit = new AuthTokenAudit(guanoScriber)

      authTokenAudit.scribeAccessTokenRevocation(
        userId = tokenUserId,
        clientAppId = tokenAppId,
        tokenManagerService = tokenManagerService,
        byUserId = Some(agentUserId),
        notes = Some(agentTokenRevokeNote),
        auditLoggingMessage = auditMessage,
        logger = logger
      )

      verify(guanoScriber, times(1)).scribe(scribeMessage)
    }
  }

  test("scribe user tokens - uninvalidate") {
    Time.withCurrentTimeFrozen { _ =>
      val scribeMessage = createScribeMessage(
        SettingsActionAction.GrantAppAccess,
        s"Uninvalidate. App ID: ${tokenAppId} and token type:OAUTH")

      val guanoScriber = mock[AuthGuanoScriber]
      val authTokenAudit = new AuthTokenAudit(guanoScriber)

      authTokenAudit.scribeAccessTokenUninvalidate(
        userId = tokenUserId,
        clientAppId = tokenAppId,
        tokenManagerService = tokenManagerService,
        auditLoggingMessage = auditMessage,
        logger = logger)

      verify(guanoScriber, times(1)).scribe(scribeMessage)
    }
  }

  test("scribe user action - token deletion") {
    val noteToScribe = s"Revoked 1 OAUTH tokens across 1 apps. "
    val scribeMessage = createUserActionScribeMessage(
      userId = tokenUserId,
      byUserId = Some(agentUserId),
      action = UserActionAction.Note,
      reason = UserActionReason.DeleteAllWebToken,
      note = Some(noteToScribe))

    val guanoScriber = mock[AuthGuanoScriber]
    val authTokenAudit = new AuthTokenAudit(guanoScriber)

    authTokenAudit.scribeUserActionTokensDeletion(
      accessTokens = 1,
      tokenType = "OAUTH",
      distinctClientAppIds = 1,
      userId = tokenUserId,
      note = None,
      byUserId = Some(agentUserId),
      reason = UserActionReason.DeleteAllWebToken,
      tokenManagerService = tokenManagerService
    )

    verify(guanoScriber, times(1)).scribe(scribeMessage)
  }

}
