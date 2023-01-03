package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.cli.commands.converters.AuthTypeSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.AuthTypeSetWrapper
import com.twitter.auth.customerauthtooling.cli.commands.converters.StringSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.StringSetWrapper
import picocli.CommandLine.{Option => CommandLineOption}

class CommonRequiredOptions extends DefaultValue with ProjectOption {
  @CommandLineOption(
    names = Array("--auth_types"),
    description = Array("Allowed auth types (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[AuthTypeSetConverter]),
    paramLabel = "<auth_types>",
    required = true)
  var authTypes: AuthTypeSetWrapper = _

  @CommandLineOption(
    names = Array("--domains"),
    description = Array("Request domains for the route"),
    converter = Array(classOf[StringSetConverter]),
    required = true)
  var domains: StringSetWrapper = _
}
