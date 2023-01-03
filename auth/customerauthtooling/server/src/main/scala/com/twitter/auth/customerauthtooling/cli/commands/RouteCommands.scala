package com.twitter.auth.customerauthtooling.cli.commands

import picocli.CommandLine
import picocli.CommandLine.Command

@Command(
  name = "route",
  description = Array("Route operations"),
  subcommands = Array(
    classOf[NewRouteCommand],
    classOf[UpdateRouteCommand],
    classOf[ApplyRouteCommand],
    //classOf[NewRouteFromEndpointCommand],
    classOf[BatchCommands],
  ))
class RouteCommands extends BaseCustomerAuthToolingCommand {
  override def call(): Unit = {
    println(new CommandLine(this).getUsageMessage())
  }
}
