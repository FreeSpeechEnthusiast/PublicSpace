package com.twitter.auth.customerauthtooling.api

import com.google.inject.Module
import com.twitter.finagle.Filter
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.finatra.decider.modules.DeciderModule
import com.twitter.finatra.mtls.thriftmux.Mtls
import com.twitter.finatra.mtls.thriftmux.modules.MtlsThriftWebFormsModule
import com.twitter.auth.customerauthtooling.api.controllers.CustomerAuthToolingServiceThriftController
import com.twitter.auth.customerauthtooling.api.modules.JsonLoggerModule
import com.twitter.auth.customerauthtooling.api.modules.AdoptionCheckerServiceModule
import com.twitter.auth.customerauthtooling.api.modules.FoundInTfeResolverModule
import com.twitter.auth.customerauthtooling.api.modules.ConfigModule
import com.twitter.auth.customerauthtooling.api.modules.CustomerauthtoolingDarkTrafficFilterModule
import com.twitter.auth.customerauthtooling.api.modules.ManualDpProviderModule
import com.twitter.auth.customerauthtooling.api.modules.NamedDpProvidersModule
import com.twitter.auth.customerauthtooling.api.modules.RouteInformationServiceModule
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.modules.ClientIdAcceptListModule
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.thrift.modules.ThriftClientIdModule
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService
import com.twitter.auth.customerauthtooling.api.modules.AlreadyAdoptedDpsResolverModule
import com.twitter.auth.customerauthtooling.api.modules.IsAppOnlyOrGuestResolverModule
import com.twitter.auth.customerauthtooling.api.modules.IsInternalEndpointResolverModule
import com.twitter.auth.customerauthtooling.api.modules.IsNgRouteResolverModule
import com.twitter.auth.customerauthtooling.api.modules.LoggerModule
import com.twitter.auth.customerauthtooling.api.modules.Oauth1OrSessionResolverModule
import com.twitter.auth.customerauthtooling.api.modules.PacmanNgRouteStorageServiceModule
import com.twitter.auth.customerauthtooling.api.modules.RequiresAuthResolverModule
import com.twitter.auth.customerauthtooling.api.modules.RouteDrafterServiceModule
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.kite.clients.modules.KiteClientModule

object CustomerauthtoolingThriftServerMain extends CustomerauthtoolingThriftServer

class CustomerauthtoolingThriftServer extends ThriftServer with Mtls {
  override val name = "customerauthtooling-server"

  override val modules: Seq[Module] = Seq(
    DeciderModule,
    ThriftClientIdModule,
    ConfigModule,
    new ClientIdAcceptListModule("/clients.yml"),
    new MtlsThriftWebFormsModule[CustomerAuthToolingService.MethodPerEndpoint](this),
    CustomerauthtoolingDarkTrafficFilterModule,
    LoggerModule,
    JsonLoggerModule,
    ManualDpProviderModule,
    NamedDpProvidersModule,
    FoundInTfeResolverModule,
    AlreadyAdoptedDpsResolverModule,
    IsAppOnlyOrGuestResolverModule,
    IsInternalEndpointResolverModule,
    IsNgRouteResolverModule,
    Oauth1OrSessionResolverModule,
    RequiresAuthResolverModule,
    AdoptionCheckerServiceModule,
    ScalaObjectMapperModule,
    KiteClientModule,
    PacmanNgRouteStorageServiceModule,
    RouteInformationServiceModule,
    RouteDrafterServiceModule,
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
      .add[CustomerAuthToolingServiceThriftController]
  }

  override protected def warmup(): Unit = {
    handle[CustomerauthtoolingThriftServerWarmupHandler]()
  }
}
