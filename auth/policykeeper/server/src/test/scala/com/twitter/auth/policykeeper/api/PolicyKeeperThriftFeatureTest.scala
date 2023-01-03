package com.twitter.auth.policykeeper.api

import com.twitter.finatra.mtls.thriftmux.EmbeddedMtlsThriftServer
import com.twitter.auth.policykeeper.api.services.internal.VerifyPoliciesService
import com.twitter.auth.policykeeper.api.services.internal.VerifyRoutePoliciesService
import com.twitter.auth.policykeeper.api.storage.RouteTag
import com.twitter.auth.policykeeper.api.storage.StorageInterface
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest
import com.twitter.util.mock.Mockito
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PolicyKeeperThriftFeatureTest extends FeatureTest with Mockito with BeforeAndAfter {

  private val mockVerifyRoutePoliciesService = mock[VerifyRoutePoliciesService]
  private val mockVerifyPoliciesService = mock[VerifyPoliciesService]
  private val mockStorageInterface = mock[StorageInterface[String, RouteTag]]

  before {
    reset(mockVerifyRoutePoliciesService)
    reset(mockVerifyPoliciesService)
  }

  /* Mock server warmup */
  // mockVerifyPoliciesService.method returns Future.value()
  // mockVerifyRoutePoliciesService.method returns Future.value()

  val server: EmbeddedThriftServer = new EmbeddedMtlsThriftServer(
    twitterServer = new PolicyKeeperThriftServer,
    flags = Map(
      "thrift.name" -> "policykeeper", // Finagle server label. Used for stats and server registry, default = 'thrift'.
      "thrift.clientId" -> "policykeeper",
      "decider.base" -> "decider.yml",
      "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil",
      "com.twitter.finatra.authentication.filters.PasetoPassportExtractorLocalMode" -> "true",
    )
  ).bind[VerifyRoutePoliciesService].toInstance(mockVerifyRoutePoliciesService)
    .bind[VerifyPoliciesService].toInstance(mockVerifyPoliciesService)
    .bind[StorageInterface[String, RouteTag]].toInstance(mockStorageInterface)

  private val client = {
    server.thriftClient[PolicyKeeperService.MethodPerEndpoint]("client123")
  }

}
