package com.twitter.auth.customerauthtooling.cli

import java.util.concurrent.Callable
import picocli.CommandLine

object JvmForkerMain {
  def main(args: Array[String]): Unit = {
    val exitCode = new CommandLine(new JvmForker()).execute(args: _*)
    System.exit(exitCode)
  }
}

class JvmForker extends Callable[Int] {

  @CommandLine.Option(
    names = Array("--interactive"),
    description = Array("Forks interactive console (default: 1)"))
  private var interactive = true

  @CommandLine.Option(names = Array("--dtab"), description = Array("Adds custom dtab"))
  private var dtab = ""

  override def call(): Int = {
    Option(dtab) match {
      case Some(dtabAdd) => InteractiveShellRunnerMain.main(Array(s"-dtab.add=${dtabAdd}"))
      case None => InteractiveShellRunnerMain.main(Array.empty[String])
    }
    0
  }

}
