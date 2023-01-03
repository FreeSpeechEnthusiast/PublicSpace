package com.twitter.auth.authentication.utils

import com.twitter.auth.models.OAuth2ClientAccessToken
import com.twitter.auth.models.AccessToken
import com.twitter.auth.models.OAuth1AccessToken
import com.twitter.auth.models.OAuth2AccessToken
import com.twitter.auth.models.OAuth2AppOnlyToken
import com.twitter.auth.models.SessionToken
import com.twitter.util.Time
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util
import java.util.BitSet
import scala.collection.Set
import scala.collection.mutable

object AccessTokenUtils {

  /**
   * Constants defining each position in the token privileges bitfield
   */
  val IS_WRITABLE: Int = 0
  val DM_READ: Int = 1
  val EMAIL_ADDRESS: Int = 2
  val MULTIPLE_OAUTH_SESSIONS: Int = 3
  val ADS_READ: Int = 4
  val ADS_READ_WRITE: Int = 5

  /**
   * legacy token scopes
   */
  val IsWritable = "is_writable"
  val DmRead = "dm_read"
  val EmailAddress = "email_address"
  val AdsRead = "ads_read"
  val AdsReadWrite = "ads_read_write"

  private[this] val InvalidationFixDateInSeconds = Time.at("2012-03-07 00:00:00 -0800").inSeconds
  private[this] val OrphanedTokenIdentifier = "OrphanedToken"

  def isOrphaned(token: OAuth1AccessToken): Boolean = {
    isInvalid(token) && token.secret == OrphanedTokenIdentifier
  }

  def isInvalid(token: OAuth1AccessToken): Boolean = {
    token.invalidatedAt exists { secs =>
      secs > 0 && secs <= Time.now.inSeconds
    }
  }

  def isInvalid(token: OAuth2ClientAccessToken): Boolean = {
    token.invalidateAt exists { secs =>
      secs > 0 && secs <= Time.now.inSeconds
    }
  }

  def isInvalid(
    token: OAuth2AppOnlyToken
  ): Boolean = {
    token.invalidateAt exists { secs =>
      secs > 0 && secs <= Time.now.inSeconds
    }
  }

  def isInvalid(token: OAuth2AccessToken): Boolean = {
    token.expiresAt > 0 && token.expiresAt < Time.now.inSeconds
  }

  def isInvalid(tokenOpt: Option[OAuth1AccessToken], ifNone: Boolean): Boolean = {
    tokenOpt match {
      case Some(token) => isInvalid(token)
      case None => ifNone
    }
  }

  def isCreatedPreInvalidationFix(token: AccessToken): Boolean = {
    token.createdAt < InvalidationFixDateInSeconds
  }

  /***
   * Create new Session Token with given privileges and IsWritable set
   * @param token - Session Token to copy
   * @param isWritable - is the token writable?
   * @param privileges - privileges to set
   * @return - copy of the input token modified as desired
   */
  def setSessionTokenPrivileges(
    token: SessionToken,
    isWritable: Boolean,
    privileges: Seq[Int]
  ): SessionToken = {
    var bits: util.BitSet = null
    if (token.privileges != null && token.privileges.isDefined) {
      bits = BitSet.valueOf(token.privileges.get)
    } else {
      bits = new BitSet()
    }

    privileges foreach { privilege => bits.set(privilege) }
    token.copy(
      privileges = Some(ByteBuffer.wrap(bits.toByteArray()).order(ByteOrder.LITTLE_ENDIAN)),
      isWritable = isWritable
    )
  }

  def privilegesToScopes(bitSet: BitSet): Set[String] = {
    val scopes = mutable.Set[String]()
    if (bitSet.get(IS_WRITABLE)) scopes.add(IsWritable)
    if (bitSet.get(DM_READ)) scopes.add(DmRead)
    if (bitSet.get(EMAIL_ADDRESS)) scopes.add(EmailAddress)
    if (bitSet.get(ADS_READ)) scopes.add(AdsRead)
    if (bitSet.get(ADS_READ_WRITE)) scopes.add(AdsReadWrite)

    scopes.toSet
  }

  /***
   * Get the list of privileges used in OAuth2+Session authentication
   * @return
   */
  def getOAuth2SessionPrivileges(): Seq[Int] = {
    Seq(
      ADS_READ,
      ADS_READ_WRITE,
      IS_WRITABLE,
      DM_READ,
      EMAIL_ADDRESS
    )
  }
}
