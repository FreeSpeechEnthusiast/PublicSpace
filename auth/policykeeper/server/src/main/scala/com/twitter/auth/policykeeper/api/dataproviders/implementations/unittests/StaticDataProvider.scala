package com.twitter.auth.policykeeper.api.dataproviders.implementations.unittests

import com.twitter.auth.policykeeper.api.dataproviders.DataProviderInterface
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.util.Future

/**
 * This data provider is designed for unit testing only.
 * Do not use for any other purposes!
 */
case class StaticDataProvider() extends DataProviderInterface {

  /**
   * Returns provided namespace
   *
   * @return
   */
  override def namespace(): String = "unittests_static"

  /**
   * Returns list of provided variable names
   *
   * @return
   */
  override def provides(): Seq[String] =
    Seq("varInt", "varBool", "varStr", "varZeroLong", "varLong")

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
      Map(
        "varInt" -> 100,
        "varBool" -> true,
        "varStr" -> "test",
        "varZeroLong" -> 0L,
        "varLong" -> 1000L
      )
    )
  }
}
