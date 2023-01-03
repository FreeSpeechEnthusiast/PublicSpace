package com.twitter.auth.policykeeper.api.dataproviderservice

import com.twitter.auth.policykeeper.api.dataproviders.DataProviderInterface
import com.twitter.servo.util.MemoizingStatsReceiver

trait PerDataProviderMetrics {

  protected val perDataProviderStatsReceiver: MemoizingStatsReceiver

  protected[api] val Scope: String = this.getClass.getSimpleName
  // bootstrap scope after setting the perDataProviderStatsReceiver
  protected lazy val perDataProviderStatsScope = perDataProviderStatsReceiver.scope(Scope)

  protected[api] def dataProviderScopeName(dataProvider: DataProviderInterface) =
    dataProvider.getClass.getSimpleName

  protected def dataProviderStatsScope(dataProvider: DataProviderInterface) =
    perDataProviderStatsScope.scope(dataProviderScopeName(dataProvider))

}
