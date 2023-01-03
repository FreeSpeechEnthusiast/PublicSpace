package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.cli.commands.converters.AuthTypeSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.AuthTypeSetWrapper
import com.twitter.auth.customerauthtooling.cli.commands.converters.StringSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.StringSetWrapper
import picocli.CommandLine.{Option => CommandLineOption}

class CommonButNotRequiredForUpdateOptions extends DefaultValue {
  @CommandLineOption(
    names = Array("--auth_types"),
    description = Array("Allowed auth types (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[AuthTypeSetConverter]),
    paramLabel = "<auth_types>")
  var authTypes: AuthTypeSetWrapper = _

  @CommandLineOption(
    names = Array("--domains"),
    description = Array("Request domains for the route"),
    converter = Array(classOf[StringSetConverter]))
  var domains: StringSetWrapper = _

  @CommandLineOption(
    names = Array("--project"),
    required = false,
    description = Array("Kite project, for example: authplat-default"))
  var project: String = _
}
