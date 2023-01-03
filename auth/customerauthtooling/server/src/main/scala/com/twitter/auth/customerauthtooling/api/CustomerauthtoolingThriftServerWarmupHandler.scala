package com.twitter.auth.customerauthtooling.api

import com.twitter.finagle.thrift.ClientId
import com.twitter.finatra.thrift.routing.ThriftWarmup
import com.twitter.inject.utils.Handler
import com.twitter.util.logging.Logging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerauthtoolingThriftServerWarmupHandler @Inject() (warmup: ThriftWarmup)
    extends Handler
    with Logging {

  /* Should be a ClientId that is allowlisted to your service. See src/main/resources/client_allowlist.yml */
  private[this] val clientId: ClientId = ClientId("client123")

  def handle(): Unit = {
    //TODO: Add warm up logic if required
  }

}
