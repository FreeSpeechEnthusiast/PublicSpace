package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.cli.commands.converters.OptStringConverter
import picocli.CommandLine.ArgGroup
import picocli.CommandLine.{Option => CommandLineOption}

class IdOrPathIdentifiers extends DefaultValue {
  @ArgGroup(exclusive = false, multiplicity = "1")
  var pathDetails: IdentifyingPathDetails = _

  @CommandLineOption(
    names = Array("--id"),
    description = Array("Route ID (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptStringConverter]),
    required = true,
    defaultValue = "")
  var id: Option[String] = _
}
