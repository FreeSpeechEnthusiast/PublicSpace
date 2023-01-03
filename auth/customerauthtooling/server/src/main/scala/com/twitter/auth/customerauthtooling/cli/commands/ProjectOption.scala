package com.twitter.auth.customerauthtooling.cli.commands

import picocli.CommandLine.{Option => CommandLineOption}

trait ProjectOption extends DefaultValue {
  @CommandLineOption(
    names = Array("--project"),
    required = true,
    description = Array("Kite project, for example: authplat-default"))
  var project: String = _
}
