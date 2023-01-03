package com.twitter.auth.policykeeper.api.storage.configbus

import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.InMemoryPolicyStorage
import com.twitter.auth.policykeeper.api.storage.ReadOnlyPolicyStorageInterface
import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.auth.policykeeper.api.storage.configbus.parser.PolicyDatabaseFile
import com.twitter.auth.policykeeper.api.storage.configbus.parser.PolicyDatabaseConfig
import com.twitter.auth.policykeeper.thriftscala.Policy
import com.twitter.util.Future
import com.twitter.configbus.subscriber.Subscription
import com.twitter.finagle.stats.StatsReceiver
import java.util.concurrent.atomic.AtomicInteger

case class ROConfigBusPolicyStorage(
  policyDatabaseSubscription: Subscription[Map[String, PolicyDatabaseFile]],
  statsReceiver: StatsReceiver,
  logger: JsonLogger)
    extends ReadOnlyPolicyStorageInterface {

  private[configbus] val underlyingStorage = InMemoryPolicyStorage()

  private[configbus] val Scope = "ROConfigBusPolicyStorage"
  private[configbus] val DataReloaded = "dataReloaded"
  private[configbus] val WrongPolicyId = "wrongPolicyId"

  private val loggerScope = logger.withScope(Scope)
  private val statsScope = statsReceiver.scope(Scope)
  private[configbus] val dataReloadedCounter = statsScope.counter(DataReloaded)
  private[configbus] val wrongPolicyIdCounter = statsScope.counter(WrongPolicyId)
  private[configbus] val loadedPoliciesCount = new AtomicInteger(0)

  private[configbus] val policyConvertor = PolicyConvertor(loggerScope)
  private val policyIdPattern = PolicyId.Pattern.r

  policyDatabaseSubscription.data.changes.respond { changesInFiles =>
    if (changesInFiles.nonEmpty) {
      var newLoadedPoliciesCount = 0
      underlyingStorage
        .replaceDataWithNewVersion(
          changesInFiles
            .collect {
              case (fileName, newValue) =>
                fileName match {
                  case PolicyDatabaseConfig.policyFolderStructurePattern(_, _) =>
                    newValue.policies
                      .map(policyConvertor.policyFromConfig)
                      .filter { jsonPolicy =>
                        jsonPolicy.policyId match {
                          case policyIdPattern(_) => true
                          case _ =>
                            wrongPolicyIdCounter.incr()
                            loggerScope.info(
                              message = "wrong policy identifier found",
                              metadata = Some(Map("policyId" -> jsonPolicy.policyId))
                            )
                            false
                        }
                      //TODO: bouncer settings check
                      }
                }
            }.foldLeft(Set.empty[Policy])({
              newLoadedPoliciesCount += 1
              _ ++ _
            }))
      loadedPoliciesCount.set(underlyingStorage.storage.size)
      dataReloadedCounter.incr()
      loggerScope.info(
        message = "data reloaded",
        metadata = Some(
          Map(
            "loaded_policies_count" -> (newLoadedPoliciesCount - 1).toString,
            "files_updated" -> changesInFiles.size.toString))
      )
    }
  }

  /**
   * Returns policies by with specified policy identifiers
   *
   * @param policyIds
   *
   * @return
   */
  override def getPoliciesByIds(policyIds: Seq[PolicyId]): Future[Seq[Policy]] = {
    underlyingStorage.getPoliciesByIds(policyIds)
  }
}
