package com.twitter.auth.policykeeper.api

import com.google.inject.Module
import com.twitter.auth.policykeeper.api.controllers.PolicyKeeperThriftController
import com.twitter.auth.policykeeper.api.filters.ThriftRequestUnpackFilter
import com.twitter.auth.policykeeper.api.modules.PassbirdClientModule
import com.twitter.finatra.authentication.modules.PassportModule
import com.twitter.auth.passport_tools.PassportRetrieverModule
import com.twitter.finagle.Filter
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.finatra.decider.modules.DeciderModule
import com.twitter.finatra.mtls.thriftmux.Mtls
import com.twitter.finatra.mtls.thriftmux.modules.MtlsThriftWebFormsModule
import com.twitter.auth.policykeeper.api.modules.PolicyKeeperDarkTrafficFilterModule
import com.twitter.auth.policykeeper.api.modules.internal.EvaluationServiceModule
import com.twitter.auth.policykeeper.api.modules.internal.JsonLoggerModule
import com.twitter.auth.policykeeper.api.modules.internal.AdvancedStatsModule
import com.twitter.auth.policykeeper.api.modules.internal.StorageModule
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.modules.ClientIdAcceptListModule
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.thrift.modules.ThriftClientIdModule

object PolicyKeeperThriftServerMain extends PolicyKeeperThriftServer

class PolicyKeeperThriftServer extends ThriftServer with Mtls {
  override val name = "policykeeper-server"

  override val modules: Seq[Module] = Seq(
    DeciderModule,
    ThriftClientIdModule,
    new ClientIdAcceptListModule("/clients.yml"),
    new MtlsThriftWebFormsModule[PolicyKeeperService.MethodPerEndpoint](this),
    PassbirdClientModule,
    PolicyKeeperDarkTrafficFilterModule,
    JsonLoggerModule,
    AdvancedStatsModule,
    StorageModule,
    EvaluationServiceModule,
    PassportModule,
    PassportRetrieverModule,
  )

  def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[StatsFilter]
      .filter[AccessLoggingFilter]
      .filter[ExceptionMappingFilter]
      .filter[Filter.TypeAgnostic, DarkTrafficFilterType]
      .filter[ThriftRequestUnpackFilter]
      .add[PolicyKeeperThriftController]
  }

  override protected def warmup(): Unit = {
    handle[PolicyKeeperThriftServerWarmupHandle]()
  }
}
