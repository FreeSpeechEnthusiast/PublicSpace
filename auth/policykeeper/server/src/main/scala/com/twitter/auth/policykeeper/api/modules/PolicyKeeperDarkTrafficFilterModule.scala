package com.twitter.auth.policykeeper.api.modules

import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService
import com.twitter.decider.Decider
import com.twitter.finagle.thrift.MethodMetadata
import com.twitter.finatra.mtls.thriftmux.modules.MtlsClient
import com.twitter.inject.Injector
import com.twitter.inject.thrift.modules.ReqRepDarkTrafficFilterModule

object PolicyKeeperDarkTrafficFilterModule
    extends ReqRepDarkTrafficFilterModule[PolicyKeeperService.ReqRepServicePerEndpoint]
    with MtlsClient {

  override protected def enableSampling(injector: Injector): Any => Boolean = {
    val decider = injector.instance[Decider] // we store the decider reference
    _ => shouldSample(decider)
  }

  // example use of a decider to sample dark traffic by method name
  private[this] def shouldSample(decider: Decider): Boolean = {
    if (decider.isAvailable("dark_traffic_percent")) {
      MethodMetadata.current match {
        case Some(m) => m.methodName.equals("search")
        case _ => true
      }
    } else {
      false
    }
  }

}
