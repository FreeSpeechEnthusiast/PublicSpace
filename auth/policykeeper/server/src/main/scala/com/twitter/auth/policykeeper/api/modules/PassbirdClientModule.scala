package com.twitter.auth.policykeeper.api.modules

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.service
import com.twitter.finagle.thriftmux.MethodBuilder
import com.twitter.finatra.mtls.thriftmux.modules.MtlsClient
import com.twitter.inject.Injector
import com.twitter.inject.thrift.ThriftMethodBuilderFactory
import com.twitter.inject.thrift.modules.ThriftMethodBuilderClientModule
import com.twitter.passbird.loginservice.thriftscala.LoginService.VerifyUserPassword
import com.twitter.passbird.thriftscala.PassbirdService
import com.twitter.passbird.thriftscala.PassbirdService._
import com.twitter.util.Duration

object PassbirdClientModule
    extends ThriftMethodBuilderClientModule[
      PassbirdService.ServicePerEndpoint,
      PassbirdService.MethodPerEndpoint
    ]
    with MtlsClient {

  private[this] val PassbirdReadRequestTimeout: Duration = 1.seconds
  private[this] val PassbirdTotalTimeout: Duration = 3.seconds
  private[this] val PassbirdAcquisitionTimeout: Duration = 1.seconds

  private[this] val PassbirdRetry = 2

  private[this] val PassbirdIdempotent: service.ResponseClassifier =
    service.ResponseClassifier.named("Passbird") {
      service.ResponseClassifier.RetryOnTimeout orElse
        service.ResponseClassifier.RetryOnChannelClosed orElse
        service.ResponseClassifier.RetryOnWriteExceptions
    }

  override val label = "passbird"
  override val dest = "/s/passbird/passbird"

  override def sessionAcquisitionTimeout: Duration = PassbirdAcquisitionTimeout

  override def configureMethodBuilder(
    injector: Injector,
    methodBuilder: MethodBuilder
  ): MethodBuilder =
    methodBuilder
      .withMaxRetries(PassbirdRetry)

  override def configureServicePerEndpoint(
    injector: Injector,
    builder: ThriftMethodBuilderFactory[PassbirdService.ServicePerEndpoint],
    servicePerEndpoint: PassbirdService.ServicePerEndpoint
  ): PassbirdService.ServicePerEndpoint = {
    servicePerEndpoint
      .withVerifyUserPassword(
        builder
          .method(VerifyUserPassword)
          .withTimeoutPerRequest(PassbirdReadRequestTimeout)
          .withTimeoutTotal(PassbirdTotalTimeout)
          .withRetryForClassifier(PassbirdIdempotent)
          .idempotent(0.1)
          .service
      )
  }
}
