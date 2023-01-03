package com.twitter.auth.policykeeper.api

import com.twitter.finatra.mtls.thriftmux.EmbeddedMtlsThriftServer
import com.google.inject.Stage
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PolicyKeeperThriftStartupTest extends FeatureTest {

  /* NOTE: Startup Tests SHOULD NOT mock any classes nor override anything in the object graph otherwise
  the test will not be useful. See: http://twitter.github.io/finatra/user-guide/testing/#startup-tests */

  val server: EmbeddedThriftServer = new EmbeddedMtlsThriftServer(
    twitterServer = new PolicyKeeperThriftServer {
      // Since we only want to test that the server is properly configured,
      // and dependencies are wired appropriately we expressly do not want
      // warmup to run. Thus we override it with an empty function here as
      // warmup would attempt to make network calls.
      override protected def warmup(): Unit = {
        // do nothing
      }
    },
    stage = Stage.PRODUCTION,
    flags = Map(
      "thrift.clientId" -> "policykeeper",
      "decider.base" -> "decider.yml",
      "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil",
      "policy_storage_config_bus_path" -> "auth/policykeeper/server/src/main/resources/config",
      "com.twitter.finatra.authentication.filters.PasetoPassportExtractorLocalMode" -> "true",
    )
  )

  test("Server#startup") {
    // Temporary disabled, due to missing implementations of storage interfaces
    server.assertHealthy()
  }
}
