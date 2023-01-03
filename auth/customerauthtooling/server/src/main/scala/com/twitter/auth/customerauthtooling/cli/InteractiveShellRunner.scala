package com.twitter.auth.customerauthtooling.cli

import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException
import java.io.InputStream
import java.io.OutputStream
import com.twitter.auth.customerauthtooling.cli.internal.ShellRunnerInterface
import com.twitter.auth.customerauthtooling.cli.modules.JLineFactoryModule.JLineState

class InteractiveShellRunner extends ShellRunnerInterface {

  // Mutable State
  @volatile private var done = false
  private var jLineState: JLineState = _

  def getGetJLineState(): JLineState = jLineState

  /**
   * Runs the InteractiveShellRunner with an interactive console
   */
  override protected def run(): Unit = {
    jLineState = injector.instance[JLineState]
    try {
      while (!done) {
        try {
          val line = jLineState.lineReader.readLine("CustomerAuth> ")
          jLineState.systemRegistry.execute(line)
        } catch {
          case e: UserInterruptException =>
            // ignore exceptions caused by ctrl-c
            trace("UserInterruptException", e)
          case _: EndOfFileException =>
            // happens when "exit" is typed
            println("Exiting")
            jLineState.close()
            done = true
            systemExit()
          case e: Exception =>
            // systemRegistry.trace knows how to write output to the terminal (e.g. help usage)
            jLineState.systemRegistry.trace(true, e)
        }
      }
    } catch {
      case t: Throwable =>
        t.printStackTrace()
    }
  }

  protected def systemExit(): Unit = {
    System.exit(0)
  }

  /** Override to support testing the local loop runner in a ScalaTest */
  def inputStreamOverride(): InputStream = null

  /** Override to support testing the local loop runner in a ScalaTest */
  def outputStreamOverride(): OutputStream = null
}

object InteractiveShellRunnerMain extends InteractiveShellRunner
