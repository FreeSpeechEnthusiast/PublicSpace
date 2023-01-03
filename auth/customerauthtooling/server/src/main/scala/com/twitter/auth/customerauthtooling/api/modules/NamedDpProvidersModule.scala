package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.customerauthtooling.api.components.dpprovider.DpProviderInterface
import com.twitter.auth.customerauthtooling.api.components.dpprovider.ManualDpProvider
import com.twitter.auth.customerauthtooling.api.components.dpprovider.SupportedDpProviders
import com.twitter.inject.TwitterModule

object NamedDpProvidersModule extends TwitterModule {
  @Provides
  @Singleton
  def providesNamedDpProviders(
    manualDpProvider: ManualDpProvider
  ): Map[SupportedDpProviders.Value, DpProviderInterface] = {
    Seq(manualDpProvider).map { p =>
      (p.name(), p)
    }.toMap
  }
}
