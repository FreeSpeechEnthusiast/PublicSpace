package com.twitter.auth.customerauthtooling.cli.modules

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService
import com.twitter.auth.customerauthtooling.cli.InteractiveShellRunner
import com.twitter.auth.customerauthtooling.cli.commands.CustomerAuthCommands
import com.twitter.auth.customerauthtooling.cli.internal.InjectionAware
import com.twitter.inject.TwitterModule
import com.twitter.util.logging.Logging
import java.nio.file.ClosedWatchServiceException
import java.nio.file.Paths
import org.jline.console.SystemRegistry
import org.jline.console.impl.SystemRegistryImpl
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import picocli.CommandLine
import picocli.CommandLine.ExitCode
import picocli.CommandLine.ParseResult
import picocli.shell.jline3.PicocliCommands
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory

// Set up picocli cliCommands for jline3
// See: https://github.com/remkop/picocli/tree/master/picocli-shell-jline3
object JLineFactoryModule extends TwitterModule with Logging {

  @Provides
  @Singleton
  def create(
    shellRunner: InteractiveShellRunner,
    customerAuthToolingService: CustomerAuthToolingService.MethodPerEndpoint,
  ): JLineState = {
    val commands = new CustomerAuthCommands

    val picocliCommandsFactory = new PicocliCommandsFactory {
      // Inject Twitter Modules into Picocli framework
      override def create[K](cls: Class[K]): K = {
        val cmd = super.create(cls)
        cmd match {
          case newInjectionAware: InjectionAware =>
            newInjectionAware.setShellRunner(shellRunner)
            newInjectionAware.setCustomerAuthToolingService(customerAuthToolingService)
            newInjectionAware.asInstanceOf[K]
          case newClass =>
            newClass
        }
      }
    }

    val commandLine = new CommandLine(commands, picocliCommandsFactory)

    // Add an exception handler that can suppress the ClosedWatchServiceException that our DirectoryWatcher
    // library throws when we ctrl-c out of a running watch command (which causes our ctrl-c signal handler
    // to call directoryWatcher.close(). See: terminal.handle below
    commandLine.setExecutionExceptionHandler(
      (ex: Exception, commandLine: CommandLine, parseResult: ParseResult) => {
        ex match {
          case e: ClosedWatchServiceException =>
            debug(s"Suppress ClosedWatchServiceException $e from ctrl-c when watching", ex)
            ExitCode.OK
          case e: UserInterruptException =>
            debug(s"Suppress UserInterruptException $e", ex)
            ExitCode.OK
          case _ =>
            throw ex
        }
      })

    val picocliCommands = new PicocliCommands(commandLine)
    val defaultParser = new DefaultParser

    val terminal = TerminalBuilder
      .builder()
      .streams(shellRunner.inputStreamOverride(), shellRunner.outputStreamOverride())
      .jansi(true)
      .jna(false)
      .build()

    picocliCommandsFactory.setTerminal(terminal)

    val systemRegistry = new SystemRegistryImpl(
      defaultParser,
      terminal,
      () => Paths.get(System.getProperty("user.dir")),
      null)
    systemRegistry.setCommandRegistries(picocliCommands)

    val reader = LineReaderBuilder
      .builder()
      .terminal(terminal)
      .completer(systemRegistry.completer)
      .history(new DefaultHistory())
      .parser(defaultParser)
      .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
      .build()

    JLineState(systemRegistry, reader)
  }

  case class JLineState(
    systemRegistry: SystemRegistry,
    lineReader: LineReader) {

    def close(): Unit = {
      // Note: there's also systemRegistry.terminal().close() but calling it hangs...
      systemRegistry.close()
    }
  }
}
