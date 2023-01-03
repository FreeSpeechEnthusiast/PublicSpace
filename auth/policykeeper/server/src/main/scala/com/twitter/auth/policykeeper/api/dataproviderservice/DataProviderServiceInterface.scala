package com.twitter.auth.policykeeper.api.dataproviderservice

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.util.Future
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.util.DefaultTimer.Implicit
import com.twitter.util.Duration

object DataProviderServiceInterface {
  // sets global timeout for all required data providers to be processed in parallel
  // should be greater than [[DataProviderInterface.dataProviderTimeout]]
  val dataProviderServiceInterfaceTimeout = 100.milliseconds
}

final case class DataProviderServiceTimeoutException(expectedTimeout: Duration) extends Exception

trait DataProviderServiceInterface extends PerDataProviderMetrics {

  /**
   * Data provider service data aggregator implementation
   * Returns map of variable names with their values for
   * execution of all requested [[policies]]
   *
   * @param policies
   * @param routeInformation
   * @return
   */
  protected def returnsData(
    policies: Seq[Policy],
    routeInformation: Option[RouteInformation],
    authMetadata: Option[AuthMetadata]
  ): Future[ExpressionInput]

  /**
   * Get data from all available data providers. Method works on top of concrete implementation [[returnsData]]
   *
   * @param policies
   * @param routeInformation
   * @return
   */
  final def getData(
    policies: Seq[Policy],
    routeInformation: Option[RouteInformation],
    authMetadata: Option[AuthMetadata]
  ): Future[ExpressionInput] =
    returnsData(
      policies = policies,
      routeInformation = routeInformation,
      authMetadata = authMetadata)
      .raiseWithin(
        timeout = DataProviderServiceInterface.dataProviderServiceInterfaceTimeout,
        exc = DataProviderServiceTimeoutException(
          DataProviderServiceInterface.dataProviderServiceInterfaceTimeout)
      )
}
