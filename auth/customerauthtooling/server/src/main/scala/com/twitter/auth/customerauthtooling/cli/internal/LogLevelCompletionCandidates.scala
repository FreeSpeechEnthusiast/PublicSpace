package com.twitter.auth.customerauthtooling.cli.internal

import scala.collection.JavaConverters._

class LogLevelCompletionCandidates extends java.lang.Iterable[String] {
  private val logLevels =
    Seq("off", "trace", "debug", "info", "warn", "error", "all").asJava.iterator()

  override def iterator(): java.util.Iterator[String] = {
    logLevels
  }
}
