package com.twitter.auth.customerauthtooling.api.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.customerauthtooling.api.components.dpprovider.ManualDpProvider
import com.twitter.inject.TwitterModule

object ManualDpProviderModule extends TwitterModule {
  @Provides
  @Singleton
  def providesManualDpProvider(): ManualDpProvider = {
    ManualDpProvider()
  }
}
