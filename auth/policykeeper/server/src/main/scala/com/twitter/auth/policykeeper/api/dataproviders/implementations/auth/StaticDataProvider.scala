package com.twitter.auth.policykeeper.api.dataproviders.implementations.auth

import com.twitter.auth.policykeeper.api.dataproviders.DataProviderInterface
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.util.Future

case class StaticDataProvider() extends DataProviderInterface {

  /**
   * Returns provided namespace
   *
   * @return
   */
  override def namespace(): String = "static"

  /**
   * Returns list of provided variable names
   *
   * @return
   */
  override def provides(): Seq[String] =
    Seq("i0", "i30", "i60", "i90", "i120", "i150", "i180", "i210", "i240", "bTrue", "bFalse")

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
  ): Future[Map[String, Any]] = Future.value(
    Map(
      "i0" -> 0,
      "i30" -> 30,
      "i60" -> 60,
      "i90" -> 90,
      "i120" -> 120,
      "i150" -> 150,
      "i180" -> 180,
      "i210" -> 210,
      "i240" -> 240,
      "bTrue" -> true,
      "bFalse" -> false
    )
  )
}
