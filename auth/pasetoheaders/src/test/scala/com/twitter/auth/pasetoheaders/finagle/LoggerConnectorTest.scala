package com.twitter.auth.pasetoheaders.finagle

import com.twitter.logging.{BareFormatter, Level, Logger, StringHandler}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.OneInstancePerTest
import org.scalatest.BeforeAndAfter
import org.scalatestplus.junit.JUnitRunner
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.auth.pasetoheaders.javahelpers.MapConv._
import com.twitter.decider.Feature
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class LoggerConnectorTest
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with BeforeAndAfter {

  private[this] val logger = Logger.get()
  private[this] val loggerConnector = FinagleLoggerProxy(logger)
  private[this] val loggingEnabledFeature = mock[Feature]
  private[this] val loggerConnectorWithDecider =
    FinagleLoggerProxy(logger, Some(loggingEnabledFeature))
  private[this] val traceHandler = new StringHandler(BareFormatter, None)

  before {
    logger.addHandler(traceHandler)
  }

  after {
    logger.clearHandlers()
  }

  def logLines(): Seq[String] = traceHandler.get.split("\n")

  def mustLog(substring: String) = {
    assert(logLines().filter { _ contains substring }.size > 0)
  }

  def mustNotLog(substring: String) = {
    assert(logLines().filter { _ contains substring }.size == 0)
  }

  test("test debug logging through interface") {
    logger.setLevel(Level.DEBUG)
    loggerConnector.debug("test1", "msg1", None)
    mustLog("test1")
    mustLog("msg1")
  }

  test("test debug logging through interface with metadata") {
    logger.setLevel(Level.DEBUG)
    loggerConnector.debug("test2", "msg2", Some(Map("key" -> "val")))
    mustLog("test2")
    mustLog("msg2")
    mustLog(""""key":"val"""")
  }

  test("test info logging through interface") {
    logger.setLevel(Level.INFO)
    loggerConnector.info("test3", "msg3", None)
    mustLog("test3")
    mustLog("msg3")
  }

  test("test warn logging through interface") {
    logger.setLevel(Level.WARNING)
    loggerConnector.warn("test4", "msg4", None)
    mustLog("test4")
    mustLog("msg4")
  }

  test("test error logging through interface") {
    logger.setLevel(Level.ERROR)
    loggerConnector.error("test5", "msg5", None)
    mustLog("test5")
    mustLog("msg5")
  }

  test("test trace logging through interface") {
    logger.setLevel(Level.TRACE)
    loggerConnector.trace("test6", "msg6", None)
    mustLog("test6")
    mustLog("msg6")
  }

  test("test debug logging through interface with disabler on") {
    when(loggingEnabledFeature.isAvailable) thenReturn true
    logger.setLevel(Level.DEBUG)
    loggerConnectorWithDecider.debug("test1", "msg1", None)
    mustLog("test1")
    mustLog("msg1")
  }

  test("test debug logging through interface with disabler off") {
    when(loggingEnabledFeature.isAvailable) thenReturn false
    logger.setLevel(Level.DEBUG)
    loggerConnectorWithDecider.debug("test1", "msg1", None)
    mustNotLog("test1")
    mustNotLog("msg1")
  }
}
