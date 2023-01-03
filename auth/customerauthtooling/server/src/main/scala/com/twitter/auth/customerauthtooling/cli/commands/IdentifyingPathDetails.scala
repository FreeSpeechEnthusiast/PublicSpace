package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.RequestMethod
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptRequestMethodConverter
import picocli.CommandLine.{Option => CommandLineOption}

class IdentifyingPathDetails extends DefaultValue {
  @CommandLineOption(
    names = Array("--cluster"),
    description = Array("Route cluster"),
    required = true)
  var routeCluster: String = _

  @CommandLineOption(
    names = Array("--method"),
    required = true,
    description = Array("Request method for the route (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptRequestMethodConverter]),
    paramLabel = "<method>")
  var method: Option[RequestMethod] = Some(RequestMethod.Get)

  @CommandLineOption(names = Array("--path"), description = Array("Route path"), required = true)
  var routePath: String = _
}
