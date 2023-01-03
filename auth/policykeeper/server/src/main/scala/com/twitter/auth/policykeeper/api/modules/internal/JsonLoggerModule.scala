package com.twitter.auth.policykeeper.api.modules.internal

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.inject.TwitterModule
import com.twitter.logging.Logger

object JsonLoggerModule extends TwitterModule {

  @Provides
  @Singleton
  def providesJsonLogger(): JsonLogger = {
    JsonLogger(logger = Logger.get()).withScope("PolicyKeeper")
  }
}
