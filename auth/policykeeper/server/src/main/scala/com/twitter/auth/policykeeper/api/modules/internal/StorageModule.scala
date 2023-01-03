package com.twitter.auth.policykeeper.api.modules.internal

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.RouteTag
import com.twitter.auth.policykeeper.api.storage.StorageInterface
import com.twitter.auth.policykeeper.api.storage.solutions.SimpleStorageSolution
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.TwitterModule

object StorageModule extends TwitterModule {

  val policyStorageConfigBusPath = flag[String](
    "policy_storage_config_bus_path",
    "/usr/local/config",
    "path to config bus root"
  )

  @Provides
  @Singleton
  def providesStorage(
    statsReceiver: StatsReceiver,
    logger: JsonLogger
  ): StorageInterface[String, RouteTag] =
    SimpleStorageSolution(
      statsReceiver = statsReceiver,
      logger = logger,
      configBusDir = policyStorageConfigBusPath())
}
