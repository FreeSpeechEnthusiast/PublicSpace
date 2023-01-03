package com.twitter.auth.customerauthtooling.cli.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.TwitterModule

object ThriftClientIdModule extends TwitterModule {
  @Provides
  @Singleton
  def providesClientId: ClientId = ClientId("customerauthtooling-cli")
}
