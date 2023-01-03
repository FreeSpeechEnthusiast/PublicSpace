package com.twitter.auth.policykeeper.api.storage.solutions

import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.FeatureNotSupported
import com.twitter.auth.policykeeper.api.storage.ReadOnlyPolicyStorageInterface
import com.twitter.auth.policykeeper.api.storage.RouteTag
import com.twitter.auth.policykeeper.api.storage.StaticPolicyToRouteTagMapping
import com.twitter.auth.policykeeper.api.storage.StorageInterface
import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.auth.policykeeper.api.storage.configbus.ROConfigBusPolicyStorage
import com.twitter.auth.policykeeper.api.storage.configbus.parser.PolicyDatabaseFile
import com.twitter.auth.policykeeper.api.storage.configbus.parser.PolicyDatabaseConfig
import com.twitter.auth.policykeeper.api.storage.configbus.parser.PolicyDatabaseParser
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.configbus.common
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.configbus.subscriber.Subscription
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.conversions.DurationOps._
import com.twitter.util.Await
import com.twitter.util.Duration
import com.twitter.util.Future
import com.twitter.util.TimeoutException

case class ServiceBootException(
  service: String,
  timeout: Duration)
    extends Throwable {
  override def getMessage: String =
    "service boot failed (" + service + "): database is not loaded within requested timeout (" + timeout
      .toString() + ")"
}

case class SimpleStorageSolution(
  statsReceiver: StatsReceiver,
  logger: JsonLogger,
  waitForBoot: Boolean = true,
  configBusDir: String = common.DefaultBaseRepo)
    extends StorageInterface[String, RouteTag] {

  private val DatabaseBootDeadline: Duration = 5.seconds
  private val Scope = "SimpleStorageSolution"

  /**
   * Waits for configbus database subscription
   * Prevents service to be used without preloaded database
   * If database file is missing or incorrect throws [[ServiceBootException]]
   */
  private[solutions] def awaitForSubscriptionReady(
    subscription: Subscription[Map[String, PolicyDatabaseFile]]
  ): Subscription[Map[String, PolicyDatabaseFile]] = {
    try {
      if (waitForBoot) {
        Await.result(subscription.firstLoadCompleted, DatabaseBootDeadline)
      }
      subscription
    } catch {
      case _: TimeoutException =>
        throw ServiceBootException(this.getClass.getSimpleName, DatabaseBootDeadline)
    }
  }

  private val loggerScope = logger.withScope(Scope)
  private val statsScope = statsReceiver.scope(Scope)

  private val configBusPollingInterval = 30.seconds
  private val policyConfigSubscription =
    awaitForSubscriptionReady(
      new ConfigbusSubscriber(
        statsReceiver = statsReceiver,
        configSource = PollingConfigSourceBuilder()
          .basePath(configBusDir)
          .pollPeriod(configBusPollingInterval)
          .statsReceiver(statsReceiver)
          .build(),
        rootPath = "")
        .subscribeAndPublishDirectory(
          path = PolicyDatabaseConfig.ConfigBusStorageDir,
          parser = PolicyDatabaseParser,
          initialValue = Map.empty[String, PolicyDatabaseFile],
          defaultValue = None,
          glob = Some("*.yaml")
        ))

  private[solutions] val roPolicyStorage = ROConfigBusPolicyStorage(
    policyDatabaseSubscription = policyConfigSubscription,
    statsReceiver = statsScope,
    logger = loggerScope
  )

  private[solutions] val roEndpointAssociationStorage = StaticPolicyToRouteTagMapping(
    policyStorage = roPolicyStorage,
    statsReceiver = statsScope,
    logger = loggerScope
  )

  override def policyStorage: ReadOnlyPolicyStorageInterface = roPolicyStorage

  /**
   * Returns policies by with specified policy identifiers
   *
   * @param policyIds
   *
   * @return
   */
  override def getPoliciesByIds(policyIds: Seq[PolicyId]): Future[Seq[Policy]] = {
    roPolicyStorage.getPoliciesByIds(policyIds)
  }

  /**
   * Attaches specific policy identifiers to associated endpoint identifiers
   *
   * @param associatedData
   * @param policyIds
   *
   * @return
   */
  override def attachAssociatedPolicies(
    associatedData: Set[RouteTag],
    policyIds: Set[PolicyId]
  ): Future[Boolean] = {
    throw FeatureNotSupported()
  }

  /**
   * If [[policyIds]] is present then detaches specific policies from associated endpoint identifiers
   * If [[policyIds]] is omitted then detaches all policies from associated endpoint identifiers
   *
   * @param associatedData
   *
   * @return
   */
  override def detachAssociatedPolicies(
    associatedData: Set[RouteTag],
    policyIds: Option[Set[PolicyId]]
  ): Future[Boolean] = {
    throw FeatureNotSupported()
  }

  /**
   * Syncs policy identifiers with associated endpoint identifiers
   *
   * @param associatedData
   * @param policyIds
   *
   * @return
   */
  override def syncAssociatedPolicies(
    associatedData: Set[RouteTag],
    policyIds: Set[PolicyId]
  ): Future[Boolean] = {
    throw FeatureNotSupported()
  }

  override protected def getAssociatedPoliciesIds(
    associatedData: Seq[RouteTag]
  ): Future[Seq[PolicyId]] = {
    roEndpointAssociationStorage.getAssociatedPoliciesIds(associatedData)
  }

  override protected def storeValidatedPolicy(policy: Policy): Future[PolicyId] = {
    throw FeatureNotSupported()
  }

  override protected def updateValidatedPolicy(
    policyId: PolicyId,
    policy: Policy
  ): Future[Boolean] = {
    throw FeatureNotSupported()
  }

  override def deletePolicy(policyId: PolicyId): Future[Boolean] = {
    throw FeatureNotSupported()
  }

  override def deletePolicies(policyIds: Seq[PolicyId]): Future[Boolean] = {
    throw FeatureNotSupported()
  }
}
