package com.twitter.auth.policykeeper.api.dataproviders

import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.util.DefaultTimer.Implicit
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.util.Duration
import com.twitter.util.Future

object DataProviderInterface {
  // sets timeout for each data provider [[returnsData]] method
  val dataProviderTimeout = 80.milliseconds
}

final case class DataProviderTimeoutException(
  dataProviderInterface: DataProviderInterface,
  expectedTimeout: Duration)
    extends Exception

abstract class DataProviderInterface {

  /**
   * Returns provided namespace
   *
   * @return
   */
  def namespace(): String

  /**
   * Returns list of provided variable names
   *
   * @return
   */
  def provides(): Seq[String]

  /**
   * Data provider data source implementation
   * Returns map of variable names with their values
   *
   * @param routeInformation
   * @return
   */
  protected def returnsData(
    routeInformation: Option[RouteInformation],
    authMetadata: Option[AuthMetadata]
  ): Future[Map[String, Any]]

  /**
   * Get data from data provider. Method works on top of concrete implementation [[returnsData]]
   *
   * @param routeInformation
   * @return
   */
  final def getData(
    routeInformation: Option[RouteInformation],
    authMetadata: Option[AuthMetadata]
  ): Future[Map[String, Any]] = {
    try {
      returnsData(routeInformation = routeInformation, authMetadata = authMetadata)
        .raiseWithin(
          timeout = DataProviderInterface.dataProviderTimeout,
          exc = DataProviderTimeoutException(this, DataProviderInterface.dataProviderTimeout))
    } catch {
      case e: Exception => Future.exception(e)
    }
  }

  protected def deleteNoneValues(data: Map[String, Option[Any]]): Map[String, Any] = {
    data
      .map {
        case (k, o) =>
          o match {
            case Some(v) => Some((k, v))
            case _ => Option.empty[(String, Any)]
          }
        case _ => Option.empty[(String, Any)]
      }
      // convert Option[A] to A removing None fields
      .collect {
        case Some(v) => v
      }.toMap
  }
}
