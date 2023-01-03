package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.inject.TwitterModule
import com.twitter.logging.Logger

object LoggerModule extends TwitterModule {

  @Provides
  @Singleton
  def providesLogger(): Logger = {
    Logger.get()
  }
}
