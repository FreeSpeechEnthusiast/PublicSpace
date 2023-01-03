package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.inject.TwitterModule
import com.twitter.logging.Logger
import com.twitter.auth.policykeeper.api.logger.JsonLogger

object JsonLoggerModule extends TwitterModule {

  @Provides
  @Singleton
  def providesJsonLogger(logger: Logger): JsonLogger = {
    JsonLogger(logger = logger).withScope("CustomerAuthToolingService")
  }
}
