package com.twitter.auth.policykeeper.api

import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService.VerifyPolicies
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService.VerifyRoutePolicies
import com.twitter.finagle.thrift.ClientId
import com.twitter.finatra.thrift.routing.ThriftWarmup
import com.twitter.inject.Logging
import com.twitter.inject.utils.Handler
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.util.Return
import com.twitter.util.Throw
import com.twitter.util.Try
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyKeeperThriftServerWarmupHandle @Inject() (warmup: ThriftWarmup)
    extends Handler
    with Logging
    with PolicyKeeperWarmupFixtures {

  /* Should be a ClientId that is allow-listed to your service. See src/main/resources/clients.yml */
  private[this] val clientId: ClientId = ClientId("policykeeper")

  def handle(): Unit = {
    try {
      clientId.asCurrent {
        warmup.sendRequest(
          method = VerifyPolicies,
          req = Request(VerifyPolicies.Args(verifyPoliciesRequest)))(
          assertWarmupResponse(_: Try[Response[VerifyPolicies.SuccessType]], "VerifyPolicies"))
      }

      clientId.asCurrent {
        warmup.sendRequest(
          method = VerifyRoutePolicies,
          req = Request(VerifyRoutePolicies.Args(verifyRoutePoliciesRequest)))(
          assertWarmupResponse(
            _: Try[Response[VerifyRoutePolicies.SuccessType]],
            "VerifyRoutePolicies"))
      }
    } catch {
      case e: Throwable =>
        // we don't want a warmup failure to prevent start-up
        error(e.getMessage, e)
    }
    info("Warm-up done.")
  }

  /* Private */

  private def assertWarmupResponse[T](result: Try[T], methodName: String): Unit = {
    // we collect and log any exceptions from the result.
    result match {
      case Return(_) =>
        info(s"Successfully request for method: $methodName")
      case Throw(exception) =>
        warn(s"Error performing request for method: $methodName")
        error(exception.getMessage, exception)
    }
  }
}
