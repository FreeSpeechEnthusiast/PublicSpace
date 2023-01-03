package com.twitter.auth.passport_tools

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.pasetoheaders.passport.PassportExtractor
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.TwitterModule
import com.twitter.logging.Logger

object PassportRetrieverModule extends TwitterModule {

  @Provides
  @Singleton
  def providesPassportRetriever(
    stats: StatsReceiver,
    passportExtractor: PassportExtractor
  ): PassportRetriever = {
    new PassportRetriever(
      logger = Logger.get(),
      stats = stats,
      passportExtractor = Some(passportExtractor))
  }
}
