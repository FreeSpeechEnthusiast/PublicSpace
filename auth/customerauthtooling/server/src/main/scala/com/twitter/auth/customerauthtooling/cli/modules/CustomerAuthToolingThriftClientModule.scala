package com.twitter.auth.customerauthtooling.cli.modules

import com.twitter.finagle.service
import com.twitter.finagle.thriftmux.MethodBuilder
import com.twitter.inject.Injector
import com.twitter.inject.thrift.ThriftMethodBuilderFactory
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService._
import com.twitter.util.Duration
import com.twitter.conversions.DurationOps._
import com.twitter.finatra.mtls.thriftmux.modules.MtlsClient
import com.twitter.inject.thrift.modules.ThriftMethodBuilderClientModule

object CustomerAuthToolingThriftClientModule
    extends ThriftMethodBuilderClientModule[
      CustomerAuthToolingService.ServicePerEndpoint,
      CustomerAuthToolingService.MethodPerEndpoint
    ]
    with MtlsClient {

  private[this] val CustomerAuthToolingServiceReadRequestTimeout: Duration = 10.seconds
  private[this] val CustomerAuthToolingServiceTotalTimeout: Duration = 30.seconds
  private[this] val CustomerAuthToolingServiceAcquisitionTimeout: Duration = 10.seconds

  private[this] val CustomerAuthToolingServiceRetry = 2

  private[this] val CustomerAuthToolingServiceIdempotent: service.ResponseClassifier =
    service.ResponseClassifier.named("CustomerAuthToolingService") {
      service.ResponseClassifier.RetryOnTimeout orElse
        service.ResponseClassifier.RetryOnChannelClosed orElse
        service.ResponseClassifier.RetryOnWriteExceptions
    }

  override val label = "customerauthtooling"
  override val dest = "/s/customerauthtooling/customerauthtooling"

  override def sessionAcquisitionTimeout: Duration = CustomerAuthToolingServiceAcquisitionTimeout

  override def configureMethodBuilder(
    injector: Injector,
    methodBuilder: MethodBuilder
  ): MethodBuilder =
    methodBuilder
      .withMaxRetries(CustomerAuthToolingServiceRetry)

  override def configureServicePerEndpoint(
    injector: Injector,
    builder: ThriftMethodBuilderFactory[CustomerAuthToolingService.ServicePerEndpoint],
    servicePerEndpoint: CustomerAuthToolingService.ServicePerEndpoint
  ): CustomerAuthToolingService.ServicePerEndpoint = {
    servicePerEndpoint
      .withCheckAdoptionStatus(
        builder
          .method(CheckAdoptionStatus)
          .withTimeoutPerRequest(CustomerAuthToolingServiceReadRequestTimeout)
          .withTimeoutTotal(CustomerAuthToolingServiceTotalTimeout)
          .withRetryForClassifier(CustomerAuthToolingServiceIdempotent)
          .idempotent(0.1)
          .service
      )
  }
}
