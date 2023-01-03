package com.twitter.auth.policykeeper.loadtest

import com.twitter.auth.policykeeper.thriftscala.VerifyPoliciesRequest
import com.twitter.auth.policykeeper.thriftscala._
import com.twitter.finagle.thrift.RichClientParam
import com.twitter.iago.core.IagoConfig
import com.twitter.iago.core.LoadGenerator
import com.twitter.iago.core.SimpleFeedProvider
import com.twitter.iago.internal.thrift.ThriftRecordProcessor
import com.twitter.iago.thrift.IagoThriftRequest
import com.twitter.logging.Logger

case class SamplePolicyCallFeed(request: VerifyPoliciesRequest)

class PolicyKeeperLoadTest(config: IagoConfig)
    extends LoadGenerator[SamplePolicyCallFeed, IagoThriftRequest, Array[Byte]](config) {

  private val logger = Logger.get()

  // Programmed feed provider.
  override val feedProvider = new SimpleFeedProvider[SamplePolicyCallFeed](config) {
    override def generate: SamplePolicyCallFeed = {
      SamplePolicyCallFeed(request = VerifyPoliciesRequest(
        policyIds = Set("simple_policy"),
        customInput = Some(Map("unittests_static.varInt" -> randomInRange(-100, 100).toString))))
    }
  }

  override val recordProcessor = new ThriftRecordProcessor[SamplePolicyCallFeed](config) {
    // Create a thrift client with the thriftService of the ThriftRecordProcessor.
    val client = new PolicyKeeperService.FinagledClient(thriftService, RichClientParam())

    override def process(record: SamplePolicyCallFeed): Unit = {
      logger.info(s"Processing the record: $record.")
      // Send the request.
      client
        .verifyPolicies(record.request)
        // Do something with the response.
        .onSuccess { resp => println("Got echoed message: " + resp) }
        .onFailure { e => println(s"Got error: $e") }
    }
  }

  private def randomInRange(start: Int, end: Int) = {
    start + new scala.util.Random().nextInt((end - start) + 1)
  }
}
