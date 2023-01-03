package com.twitter.auth.customerauthtooling.cli.commands

import picocli.CommandLine
import picocli.CommandLine.Command

@Command(
  subcommands = Array(
    classOf[ClearCommand],
    classOf[AdoptionCheckCommand],
    classOf[RouteCommands]
  ))
class CustomerAuthCommands extends BaseCustomerAuthToolingCommand {
  override def call(): Unit = {
    println(new CommandLine(this).getUsageMessage())
  }
}
