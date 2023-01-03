package com.twitter.auth.policykeeper.api.dataproviders.implementations.input

import com.twitter.auth.policykeeper.api.dataproviders.DataProviderInterface
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.RequestInformation
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.util.Future

case class InputDataProvider() extends DataProviderInterface {

  /**
   * Returns provided namespace
   *
   * @return
   */
  override def namespace(): String = "input"

  /**
   * Returns list of provided variable names
   *
   * @return
   */
  override def provides(): Seq[String] =
    Seq("current_password", "redirect_after_verify")

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
    routeInformation match {
      case Some(RouteInformation(_, _, Some(RequestInformation(_, _, _, _, Some(bodyParams))))) =>
        Future.value {
          deleteNoneValues(
            Map(
              "current_password" -> bodyParams.get("current_password"),
              "redirect_after_verify" -> Some(bodyParams.getOrElse("redirect_after_verify", "/"))
            ))
        }
      case _ => Future.value(Map())
    }
  }
}
