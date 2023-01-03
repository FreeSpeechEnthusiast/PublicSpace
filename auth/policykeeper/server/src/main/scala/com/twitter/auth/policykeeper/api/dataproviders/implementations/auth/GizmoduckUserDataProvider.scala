package com.twitter.auth.policykeeper.api.dataproviders.implementations.auth

import com.twitter.auth.policykeeper.api.dataproviders.DataProviderInterface
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.util.Future

case class GizmoduckUserDataProvider() extends DataProviderInterface {

  /**
   * Returns provided namespace
   *
   * @return
   */
  override def namespace(): String = "gizmoduck_user"

  /**
   * Returns list of provided variable names
   *
   * @return
   */
  override def provides(): Seq[String] =
    Seq("id")

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
        authMetadata match {
          case Some(m) =>
            Map(
              "id" -> m.gizmoduckUserId
            )
          case None => Map()
        }
      )
    )
  }
}
