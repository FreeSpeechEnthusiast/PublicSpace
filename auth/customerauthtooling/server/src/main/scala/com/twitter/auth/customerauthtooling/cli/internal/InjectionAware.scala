package com.twitter.auth.customerauthtooling.cli.internal

import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService
import com.twitter.auth.customerauthtooling.cli.InteractiveShellRunner

trait InjectionAware {
  protected var shellRunner: InteractiveShellRunner = _
  protected[cli] var customerAuthToolingService: CustomerAuthToolingService.MethodPerEndpoint = _
  def setShellRunner(shellRunner: InteractiveShellRunner): Unit = {
    this.shellRunner = shellRunner
  }
  def setCustomerAuthToolingService(
    customerAuthToolingService: CustomerAuthToolingService.MethodPerEndpoint
  ): Unit = {
    this.customerAuthToolingService = customerAuthToolingService
  }
}
