package com.twitter.auth.models

import com.twitter.appsec.sanitization.DataSafety
import com.twitter.appsec.sanitization.URLSafety
import com.twitter.auth.models.ClientApplication.OAuth2ConfidentialClientAppTypes
import com.twitter.auth.clientappflag.thriftscala.ClientAppFlag
import com.twitter.passbird.bitfield.clientprivileges.thriftscala.{Constants => ClientPrivileges}
import com.twitter.passbird.clientapplication.thriftscala.ClientApplicationPrivileges
import com.twitter.passbird.clientapplication.thriftscala.Oauth2AppType
import com.twitter.passbird.clientapplication.thriftscala.{ClientApplication => TClientApplication}
import com.twitter.passbird.clientapplication.thriftscala.{Organization => TOrganization}
import java.nio.ByteBuffer
import java.util

case class Organization(
  name: String,
  url: Option[String] = None,
  termsAndConditionsUrl: Option[String] = None,
  privacyPolicyUrl: Option[String] = None)

object Organization {
  def fromThrift(t: TOrganization): Organization = {
    Organization(
      name = t.name,
      url = t.url,
      termsAndConditionsUrl = t.termsAndConditionsUrl,
      privacyPolicyUrl = t.privacyPolicyUrl
    )
  }

  def toThrift(o: Organization): TOrganization = {
    TOrganization(
      name = o.name,
      url = o.url,
      termsAndConditionsUrl = o.termsAndConditionsUrl,
      privacyPolicyUrl = o.privacyPolicyUrl
    )
  }
}

/**
 * Copied from Macaw Login excluding deprecated fields
 */
case class ClientApplication(
  id: Long,
  name: String,
  consumerKey: String,
  secret: String,
  userId: Long,
  isActive: Boolean,
  isWritable: Boolean,
  supportsLogin: Boolean,
  parentId: Option[Long] = None,
  maxTokens: Option[Int] = None,
  usedTokens: Option[Int] = None,
  url: Option[String] = None,
  callbackUrl: Option[String] = None,
  additionalCallbackUrls: Option[Set[String]] = None,
  supportUrl: Option[String] = None,
  description: Option[String] = None,
  imageUrl: Option[String] = None,
  appPrivileges: Option[_root_.java.nio.ByteBuffer] = None,
  organization: Option[Organization] = None,
  createdAt: Long,
  updatedAt: Long,
  supportsOauth2: Option[Boolean] = None,
  oauth2ClientId: Option[String] = None,
  oauth2Secret: Option[String] = None,
  oauth2AppType: Option[String] = None,
  clientAppFlags: Option[Set[ClientAppFlag]] = None) {

  lazy val isOAuth2ConfidentialClient: Boolean =
    oauth2AppType.exists(OAuth2ConfidentialClientAppTypes.contains(_))

  // The ClientApplication has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"ClientApplication(<all fields redacted, id: ${this.id}>)"

  // Privileges
  lazy val supportsMultiOauthSessions: Boolean = hasPrivilege(
    ClientPrivileges.MULTIPLE_OAUTH_SESSIONS)
  lazy val supportsAutoApproveAndHideApp: Boolean = hasPrivilege(
    ClientPrivileges.AUTO_APPROVE_AND_HIDE_APP)
  lazy val supportRestrictAppTokenInvalidation: Boolean = hasPrivilege(
    ClientPrivileges.RESTRICT_APP_TOKEN_INVALIDATION)

  lazy val canReadDMs: Boolean = isActive && hasPrivilege(ClientPrivileges.DM_READ)
  lazy val canViewEmail: Boolean = isActive && hasPrivilege(ClientPrivileges.REQUEST_EMAIL_ADDRESS)

  lazy val allowAdsAnalytics: Boolean = hasPrivilege(ClientPrivileges.ADS_ANALYTICS)
  lazy val allowAdsCampaignManagement: Boolean = hasPrivilege(
    ClientPrivileges.ADS_CAMPAIGN_MANAGEMENT)

  lazy val isApplicationWhitelistedForDMs: Boolean =
    isActive && hasPrivilege(ClientPrivileges.INCLUDE_DIRECT_MESSAGES)

  private lazy val appPrivilegeBitSet: util.BitSet = getBitset(appPrivileges)

  private def getBitset(buffer: Option[ByteBuffer]): java.util.BitSet =
    buffer.map(java.util.BitSet.valueOf).getOrElse(new java.util.BitSet)

  private def hasPrivilege(privilege: Int): Boolean = appPrivilegeBitSet.get(privilege)
}

object ClientApplication {
  val OAuth2ConfidentialClientAppTypes: Seq[String] =
    Seq(
      Oauth2AppType.WebApp.name,
      Oauth2AppType.MachineToMachineAppOrBot.name
    )

  val DefaultAppImgUrl = "https://abs.twimg.com/a/1419377433/images/oauth_application.png"

  def fromThrift(t: TClientApplication): ClientApplication = {
    ClientApplication(
      id = t.id,
      name = t.name,
      userId = t.userId,
      maxTokens = t.maxTokens,
      usedTokens = t.usedTokens,
      consumerKey = t.consumerKey,
      secret = t.secret,
      description = t.description,
      url = t.url,
      callbackUrl = t.callbackUrl,
      additionalCallbackUrls = t.additionalCallbackUrls.map(_.toSet),
      supportUrl = t.supportUrl,
      imageUrl = t.imageUrl,
      supportsLogin = t.supportsLogin,
      parentId = t.parentId,
      isActive = t.isActive,
      isWritable = t.isWritable,
      appPrivileges = t.appPrivileges,
      organization = t.organization.map(Organization.fromThrift),
      createdAt = t.createdAt,
      updatedAt = t.updatedAt,
      supportsOauth2 = t.supportsOauth2,
      oauth2ClientId = t.oauth2ClientId,
      oauth2Secret = t.oauth2Secret,
      oauth2AppType = t.oauth2AppType.map(_.name),
      clientAppFlags = t.clientAppFlags.map(_.toSet)
    )
  }

  def toThrift(c: ClientApplication): TClientApplication = {
    TClientApplication(
      id = c.id,
      name = c.name,
      userId = c.userId,
      maxTokens = c.maxTokens,
      usedTokens = c.usedTokens,
      consumerKey = c.consumerKey,
      secret = c.secret,
      description = c.description,
      url = c.url,
      callbackUrl = c.callbackUrl,
      additionalCallbackUrls = c.additionalCallbackUrls,
      supportUrl = c.supportUrl,
      imageUrl = c.imageUrl,
      supportsLogin = c.supportsLogin,
      parentId = c.parentId,
      isActive = c.isActive,
      isWritable = c.isWritable,
      appPrivileges = c.appPrivileges,
      organization = c.organization.map(Organization.toThrift),
      createdAt = c.createdAt,
      updatedAt = c.updatedAt,
      // privileges is deprecated
      privileges = ClientApplicationPrivileges(),
      supportsOauth2 = c.supportsOauth2,
      oauth2ClientId = c.oauth2ClientId,
      oauth2Secret = c.oauth2Secret,
      oauth2AppType = c.oauth2AppType.flatMap(Oauth2AppType.valueOf),
      clientAppFlags = c.clientAppFlags
    )
  }

  def appName(clientApplication: ClientApplication, shouldSanitize: Boolean): String = {
    DataSafety.sanitizeOAuthHeader(shouldSanitize) {
      clientApplication.name
    }
  }

  def orgName(clientApplication: ClientApplication, shouldSanitize: Boolean): String = {
    DataSafety.sanitizeOAuthHeader(shouldSanitize) {
      clientApplication.organization.map { org: Organization =>
        org.name
      } getOrElse {
        ""
      }
    }
  }

  def appUrl(clientApplication: ClientApplication, shouldSanitize: Boolean): String = {
    URLSafety.encodeUrl(shouldSanitize) {
      clientApplication.url.map(URLSafety.httpOnly).getOrElse("")
    }
  }

  def shortAppUrl(clientApplication: ClientApplication, shouldSanitize: Boolean): String = {
    appUrl(clientApplication, shouldSanitize)
      .replaceAll("(http://|https://)", "")
  }

  def imageUrl(clientApplication: ClientApplication, shouldSanitize: Boolean): String = {
    URLSafety.encodeUrl(shouldSanitize) {
      clientApplication.imageUrl.map(URLSafety.httpOnly).getOrElse(DefaultAppImgUrl)
    }
  }

  def appDescription(clientApplication: ClientApplication, shouldSanitize: Boolean): String = {
    DataSafety.sanitizeOAuthHeader(shouldSanitize) {
      clientApplication.description match {
        case Some(s: String) => s
        case _ => ""
      }
    }
  }

  def termsAndConditionsUrl(
    clientApplication: ClientApplication,
    shouldSanitize: Boolean
  ): String = {
    URLSafety.encodeUrl(shouldSanitize) {
      clientApplication.organization flatMap { org: Organization =>
        org.termsAndConditionsUrl.map(URLSafety.httpOnly)
      } getOrElse {
        ""
      }
    }
  }

  def privacyPolicyUrl(clientApplication: ClientApplication, shouldSanitize: Boolean): String = {
    URLSafety.encodeUrl(shouldSanitize) {
      clientApplication.organization flatMap { org: Organization =>
        org.privacyPolicyUrl.map(URLSafety.httpOnly)
      } getOrElse ""
    }
  }
}
