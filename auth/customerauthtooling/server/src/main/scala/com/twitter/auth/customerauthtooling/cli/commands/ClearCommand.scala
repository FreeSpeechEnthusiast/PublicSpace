package com.twitter.auth.customerauthtooling.cli.commands

import org.jline.utils.InfoCmp.Capability
import picocli.CommandLine.Command

@Command(name = "clear", description = Array("Clears the screen"))
class ClearCommand extends BaseCustomerAuthToolingCommand {
  override def call: Unit = {
    val terminal = shellRunner.getGetJLineState().systemRegistry.terminal
    if (terminal != null) {
      terminal.puts(Capability.clear_screen)
    }
  }
}
