package com.twitter.auth.pasetoheaders.finagle

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.logging.Logger
import java.io.{File, FileOutputStream, PrintStream}
import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
trait KeyProvidersBase
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  protected var testEnv = "devel"
  protected var testIssuer = "test"
  protected var testVersion = 1

  protected var testOtherEnv = "devel2"
  protected var testOtherIssuer = "test2"
  protected var testOtherVersion = 2

  override implicit val patienceConfig = PatienceConfig(timeout = Span(2, Seconds))
  protected val statsReceiver = new InMemoryStatsReceiver
  protected val statsConnector = FinagleStatsProxy(statsReceiver)
  protected val logger = Logger.get()
  protected val loggerConnector = FinagleLoggerProxy(logger)

  def fakeData(parentPath: File, fileName: String, str: String) = {
    val path = new File(parentPath, fileName)
    val stream = new PrintStream(new FileOutputStream(path))
    stream.print(str)
    stream.close()
  }

}
