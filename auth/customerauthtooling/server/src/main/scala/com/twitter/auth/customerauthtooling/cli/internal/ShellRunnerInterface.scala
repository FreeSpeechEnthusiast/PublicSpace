package com.twitter.auth.customerauthtooling.cli.internal

import com.google.inject.Module
import com.twitter.auth.customerauthtooling.cli.modules.CustomerAuthToolingThriftClientModule
import com.twitter.auth.customerauthtooling.cli.modules.JLineFactoryModule
import com.twitter.auth.customerauthtooling.cli.modules.ServiceIdentifierModule
import com.twitter.auth.customerauthtooling.cli.modules.ThriftClientIdModule
import com.twitter.inject.app.AbstractApp
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.server.TwitterServer

trait ShellRunnerInterface extends AbstractApp with TwitterServer {

  override protected def disableAdminHttpServer = true

  override protected def modules: Seq[Module] = Seq(
    StatsReceiverModule,
    ThriftClientIdModule,
    ServiceIdentifierModule,
    CustomerAuthToolingThriftClientModule,
    JLineFactoryModule
  )
}
