package com.twitter.auth.customerauthtooling.cli.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.finagle.mtls.authentication.ServiceIdentifier
import com.twitter.inject.TwitterModule

/**
 * Authenticates the CLI app for s2s auth using local user certs
 */
object ServiceIdentifierModule extends TwitterModule {
  @Provides
  @Singleton
  def providesServiceIdentifier: ServiceIdentifier =
    ServiceIdentifier(
      role = System.getenv("USER"),
      service = "customerauthtooling-cli",
      environment = "devel",
      zone = "local")
}
