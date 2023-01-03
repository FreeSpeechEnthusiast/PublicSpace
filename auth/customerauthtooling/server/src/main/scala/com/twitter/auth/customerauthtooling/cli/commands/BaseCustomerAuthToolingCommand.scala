package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.cli.internal.LogLevelCompletionCandidates
import com.twitter.auth.customerauthtooling.cli.internal.InjectionAware
import ch.qos.logback.classic.Level
import com.twitter.auth.customerauthtooling.cli.commands.converters.BooleanConverter
import java.util.concurrent.Callable
import picocli.CommandLine.{Option => CommandLineOption}

abstract class BaseCustomerAuthToolingCommand
    extends Callable[Unit]
    with InjectionAware
    with DefaultValue {

  @CommandLineOption(
    names = Array("--verbose"),
    defaultValue = "false",
    description = Array("Verbose test output (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[BooleanConverter]))
  var verbose: Boolean = false

  @CommandLineOption(
    names = Array("--log-level"),
    paramLabel = "<log_level>",
    defaultValue = "debug",
    description = Array("Log level for outputing slf4j logs"),
    completionCandidates = classOf[LogLevelCompletionCandidates])
  var logLevelStr: String = ""

  protected def logLevel: Level = {
    Level.toLevel(logLevelStr)
  }

}
