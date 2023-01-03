package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.customerauthtooling.api.components.PacmanNgRouteStorageService
import com.twitter.auth.customerauthtooling.api.components.pacmanngroutestorage.PacmanNgRouteStorageServiceInterface
import com.twitter.inject.TwitterModule
import com.twitter.kite.clients.KiteClient

object PacmanNgRouteStorageServiceModule extends TwitterModule {
  @Provides
  @Singleton
  def providesPacmanNgRouteStorageService(
    kiteClient: KiteClient
  ): PacmanNgRouteStorageServiceInterface = {
    PacmanNgRouteStorageService(kiteClient = kiteClient)
  }
}
