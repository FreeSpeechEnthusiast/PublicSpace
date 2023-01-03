package com.twitter.auth.policykeeper.api.services

import com.twitter.passbird.thriftscala._
import com.twitter.util.Future
import javax.inject._

@Singleton
class PassbirdService @Inject() (passbirdService: PassbirdService.MethodPerEndpoint) {

  def verifyUserPassword(
    userId: Long,
    currentPassword: String,
    sessionHash: Option[String]
  ): Future[Boolean] = {
    passbirdService.verifyUserPassword(userId, currentPassword, sessionHash)
  }
}
