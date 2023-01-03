package com.twitter.auth.customerauthtooling.cli.commands

import picocli.CommandLine
import picocli.CommandLine.Command

@Command(
  name = "batch",
  description = Array("Batch operations"),
  subcommands = Array(
    classOf[BatchApplyRouteCommand],
  ))
class BatchCommands extends BaseCustomerAuthToolingCommand {
  override def call(): Unit = {
    println(new CommandLine(this).getUsageMessage())
  }
}
