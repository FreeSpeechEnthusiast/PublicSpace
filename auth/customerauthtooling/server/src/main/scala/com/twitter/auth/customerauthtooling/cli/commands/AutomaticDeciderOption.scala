package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.cli.commands.converters.OptBooleanConverter
import picocli.CommandLine.{Option => CommandLineOption}

trait AutomaticDeciderOption extends DefaultValue {
  @CommandLineOption(
    names = Array("--append_decider_automatically"),
    description = Array(
      "Allows to set the foundInTfeOverride checker override (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptBooleanConverter]),
    defaultValue = "false")
  var automaticDecider: Option[Boolean] = None
}
