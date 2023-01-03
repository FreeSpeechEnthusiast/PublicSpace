package com.twitter.auth.customerauthtooling.api

import com.twitter.finatra.mtls.thriftmux.EmbeddedMtlsThriftServer
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest
import com.twitter.util.mock.Mockito
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CustomerauthtoolingThriftFeatureTest extends FeatureTest with Mockito {

  val server: EmbeddedThriftServer = new EmbeddedMtlsThriftServer(
    twitterServer = new CustomerauthtoolingThriftServer,
    flags = Map(
      "thrift.name" -> "customerauthtooling", // Finagle server label. Used for stats and server registry, default = 'thrift'.
      "thrift.clientId" -> "client123",
      "decider.base" -> "decider.yml",
      "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil",
      "kite.client.env" -> "devel"
    )
  )

}
