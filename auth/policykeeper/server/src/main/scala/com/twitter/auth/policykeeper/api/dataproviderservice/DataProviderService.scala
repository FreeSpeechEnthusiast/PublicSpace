package com.twitter.auth.policykeeper.api.dataproviderservice

import com.twitter.auth.policykeeper.api.dataproviders.DataProviderInterface
import com.twitter.auth.policykeeper.api.dataproviders.DataProviderTimeoutException
import com.twitter.auth.policykeeper.api.dataproviders.implementations.auth.AccessTokenDataProvider
import com.twitter.auth.policykeeper.api.dataproviders.implementations.auth.AuthDataProvider
import com.twitter.auth.policykeeper.api.dataproviders.implementations.auth.AuthEventsDataProvider
import com.twitter.auth.policykeeper.api.dataproviders.implementations.auth.GizmoduckUserDataProvider
import com.twitter.auth.policykeeper.api.dataproviders.implementations.auth.StaticDataProvider
import com.twitter.auth.policykeeper.api.dataproviders.implementations.input.InputDataProvider
import com.twitter.auth.policykeeper.api.dataproviders.implementations.unittests.{
  FaultyDataProvider => UnitTestsFaultyDataProvider
}
import com.twitter.auth.policykeeper.api.dataproviders.implementations.unittests.{
  SlowDataProvider => UnitTestsSlowDataProvider
}
import com.twitter.auth.policykeeper.api.dataproviders.implementations.unittests.{
  StaticDataProvider => UnitTestsStaticDataProvider
}
import com.twitter.auth.policykeeper.api.evaluationengine.Expression
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInput
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterValue
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionParserInterface
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.thriftscala.AuthMetadata
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.finagle.stats.Stat
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.servo.util.MemoizingStatsReceiver
import com.twitter.util.Future
import com.twitter.auth.policykeeper.api.interpolation.InputAnalyzer

final case class DataProviderService[T <: Expression](
  private[dataproviderservice] val expressionParser: ExpressionParserInterface[T],
  override protected val perDataProviderStatsReceiver: MemoizingStatsReceiver,
  private val statsReceiver: StatsReceiver,
  private val logger: JsonLogger)
    extends DataProviderServiceInterface {

  private val loggerScope = logger.withScope(Scope)
  private val statsScope = statsReceiver.scope(Scope)

  private val inputAnalyzer = InputAnalyzer()

  private[dataproviderservice] val DataRequested = "dataRequested"
  private[dataproviderservice] val DataReceived = "dataReceived"
  private[dataproviderservice] val DataRequestFailed = "dataRequestFailed"
  private[dataproviderservice] val DataRequestTimeout = "dataRequestTimeout"
  private[dataproviderservice] val DataProviderIncompleteResult = "dataProviderMissedFields"
  private[dataproviderservice] val DataRequestTime = "dataRequestTime"

  private[dataproviderservice] val dataRequestedCounter = statsScope.counter(DataRequested)
  private[dataproviderservice] val dataReceivedCounter = statsScope.counter(DataReceived)
  private[dataproviderservice] val dataRequestFailedCounter =
    statsScope.counter(DataRequestFailed)
  private[dataproviderservice] val dataRequestTimeoutCounter =
    statsScope.counter(DataRequestTimeout)
  private[dataproviderservice] val dataProviderIncompleteResultCounter =
    statsScope.counter(DataProviderIncompleteResult)
  private[dataproviderservice] val dataProviderProcessingTime = statsScope.stat(DataRequestTime)

  private[dataproviderservice] val knownDataProviders = Seq[DataProviderInterface](
    AuthDataProvider(),
    AuthEventsDataProvider(),
    StaticDataProvider(),
    AccessTokenDataProvider(),
    GizmoduckUserDataProvider(),
    InputDataProvider(),
    // feel free to register new data providers below this line
    //
    // unit tests
    UnitTestsSlowDataProvider(),
    UnitTestsFaultyDataProvider(),
    UnitTestsStaticDataProvider(),
  )

  private val knownVariablesToDataProvidersMap = knownDataProviders.zipWithIndex
  // convert all namespace / provides to ExpressionInputParameterName -> DataProviderIndex pairs
    .collect {
      case (provider, providerIndex) =>
        provider.provides().map { varname =>
          (providerIndex, ExpressionInputParameterName(varname, provider.namespace()))
        }
    }
    // merge all known ExpressionInputParameterName -> DataProviderIndex pairs
    .foldLeft(Seq.empty[(Int, ExpressionInputParameterName)]) {
      _ ++ _
    }.groupBy(_._2)
    // convert to map of ExpressionInputParameterName -> DataProviderIndex
    .mapValues(_.map(_._1).head)
    // convert to map of ExpressionInputParameterName -> DataProvider
    .map {
      case (k, v) => (k, knownDataProviders(v))
    }

  override protected def returnsData(
    policies: Seq[Policy],
    routeInformation: Option[RouteInformation],
    authMetadata: Option[AuthMetadata]
  ): Future[ExpressionInput] = {
    Future
      .collect(
        policies
          .map { policy =>
            policy.rules
              .map { rule =>
                expressionParser.parse(rule.expression).requiredInput() ++
                  // merge required input for rule's action
                  inputAnalyzer.requiredInputFor(rule.action) ++
                  // merge required input for rule's fallback action
                  (rule.fallbackAction match {
                    case Some(action) =>
                      inputAnalyzer.requiredInputFor(action)
                    case None => Set()
                  })
              }
              // merge all required input for all policy rules
              .foldLeft(Set.empty[ExpressionInputParameterName]) {
                _ ++ _
              }
          }
          // merge all required input for all requested policies
          .foldLeft(Set.empty[ExpressionInputParameterName]) {
            _ ++ _
          }
          // remap required input to sequence of data providers and required input ignoring unknown fields
          .toSeq
          .collect {
            case requiredInput if knownVariablesToDataProvidersMap.contains(requiredInput) =>
              knownVariablesToDataProvidersMap(requiredInput) -> requiredInput
          }
          // transform result to map of data provider -> sequence of required fields
          .groupBy(_._1).mapValues(_.map(_._2))
          // remap future result to required format
          .map {
            case (provider, requiredInput) =>
              dataRequestedCounter.incr()
              dataProviderStatsScope(provider).counter(DataRequested).incr()
              Stat.timeFuture(dataProviderStatsScope(provider).stat(DataRequestTime)) {
                Stat.timeFuture(dataProviderProcessingTime) {
                  provider
                    .getData(
                      routeInformation: Option[RouteInformation],
                      authMetadata: Option[AuthMetadata]).rescue {
                      case DataProviderTimeoutException(p, duration) =>
                        dataRequestTimeoutCounter.incr()
                        dataProviderStatsScope(provider).counter(DataRequestTimeout).incr()
                        loggerScope.warning(
                          message = "data provider failed due to timeout",
                          metadata = Some(
                            Map(
                              "dataProvider" -> provider.getClass.getSimpleName,
                              "policies" -> policies.map(_.policyId),
                              "expectedDurationMs" -> duration.inMilliseconds))
                        )
                        Future.exception(DataProviderTimeoutException(p, duration))
                      case e: Exception =>
                        dataRequestFailedCounter.incr()
                        dataProviderStatsScope(provider).counter(DataRequestFailed).incr()
                        loggerScope.error(
                          message = "data provider failed due to other exception",
                          metadata = Some(
                            Map(
                              "dataProvider" -> provider.getClass.getSimpleName,
                              "policies" -> policies.map(_.policyId),
                              "exception" -> e.getMessage))
                        )
                        Future.exception(e)
                    }.map(m => {
                      dataReceivedCounter.incr()
                      dataProviderStatsScope(provider).counter(DataReceived).incr()
                      // remap data providers data to map of ExpressionInputParameterName -> value
                      // ignoring all extra fields
                      val receivedData = m.collect {
                        case (k, v)
                            if requiredInput.contains(
                              ExpressionInputParameterName(k, provider.namespace())) =>
                          (
                            ExpressionInputParameterName(k, provider.namespace()),
                            ExpressionInputParameterValue(v))
                      }
                      // log cases if received data doesn't match expected data
                      // it can be caused by unavailability of the data (ie access token)
                      // in a specific context
                      if (receivedData.size != requiredInput.size) {
                        dataProviderIncompleteResultCounter.incr()
                        dataProviderStatsScope(provider)
                          .counter(DataProviderIncompleteResult).incr()
                        loggerScope.warning(
                          "data provider returned incomplete result",
                          Some(Map(
                            "dataProvider" -> provider.getClass.getSimpleName,
                            "policies" -> policies.map(_.policyId),
                            "requiredInput" -> requiredInput
                              .map(_.toString),
                            "receivedFields" -> receivedData.keys.map(_.toString)
                          ))
                        )
                      }
                      receivedData
                    })
                }
              }
          }
          .toSeq
      ).map { v =>
        // aggregate variables from all data providers together
        ExpressionInput(underlyingMap =
          v.foldLeft(Map.empty[ExpressionInputParameterName, ExpressionInputParameterValue]) {
            _ ++ _
          })
      }
  }
}
