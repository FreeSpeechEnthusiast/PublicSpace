package com.twitter.auth.policykeeper.api.dataproviders.implementations.auth

import com.twitter.auth.policykeeper.api.context.LocalContext
import com.twitter.auth.policykeeper.api.dataproviders.DataProviderInterface
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.util.Future
import java.net.URLEncoder

case class AuthDataProvider() extends DataProviderInterface {

  /**
   * Returns provided namespace
   *
   * @return
   */
  override def namespace(): String = "auth"

  /**
   * Returns list of provided variable names
   *
   * @return
   */
  override def provides(): Seq[String] =
    Seq(
      "clientApplicationId",
      "sessionHash",
      "userId",
      "authenticatedUserId",
      "guestToken",
      "encodedSessionHash")

  /**
   * Returns map of variable names with their values
   *
   * @param routeInformation
   * @param authMetadata
   *
   * @return
   */
  override def returnsData(
    routeInformation: Option[RouteInformation],
    authMetadata: Option[AuthMetadata]
  ): Future[Map[String, Any]] = {
    Future.value(
      deleteNoneValues(
        Map(
          "clientApplicationId" -> LocalContext.getClientApplicationId,
          "sessionHash" -> LocalContext.getSessionHash,
          "encodedSessionHash" -> (LocalContext.getSessionHash match {
            case Some(sessionHash) => Some(URLEncoder.encode(sessionHash, "UTF-8"))
            case _ => None
          }),
          "userId" -> LocalContext.getUserId,
          "authenticatedUserId" -> LocalContext.getAuthenticatedUserId,
          "guestToken" -> LocalContext.getGuestToken
        ))
    )
  }
}
