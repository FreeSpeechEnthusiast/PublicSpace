package com.twitter.auth.policykeeper.api.dataproviders.implementations.unittests

import com.twitter.conversions.DurationOps._
import com.twitter.auth.policykeeper.api.dataproviders.DataProviderInterface
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.Future

/**
 * This data provider is designed for unit testing only.
 * Do not use for any other purposes!
 */
case class SlowDataProvider() extends DataProviderInterface {

  /**
   * Returns provided namespace
   *
   * @return
   */
  override def namespace(): String = "unittests_slow"

  /**
   * Returns list of provided variable names
   *
   * @return
   */
  override def provides(): Seq[String] =
    Seq("var1", "var2", "var3")

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
    Future.sleep(1.second)(DefaultTimer).map { _ =>
      Map(
        "var1" -> 100,
        "var2" -> 200,
        "var3" -> 300,
      )
    }
  }
}
