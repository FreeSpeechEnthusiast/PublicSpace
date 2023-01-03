package com.twitter.auth.policykeeper.api.modules.internal

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.TwitterModule
import com.twitter.servo.util.MemoizingStatsReceiver

/**
 * Provides MemoizingStatsReceiver for "per policy" and "per data provider" stats
 */
object AdvancedStatsModule extends TwitterModule {

  @Provides
  @Singleton
  def providesAdvancedStatsReceiver(statsReceiver: StatsReceiver): MemoizingStatsReceiver = {
    new MemoizingStatsReceiver(statsReceiver)
  }
}
