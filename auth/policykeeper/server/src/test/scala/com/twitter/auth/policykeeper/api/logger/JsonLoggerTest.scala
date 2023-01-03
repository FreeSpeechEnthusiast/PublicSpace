package com.twitter.auth.policykeeper.api.logger

import com.twitter.auth.policykeeper.api.storage.StorageTestBase
import com.twitter.logging.BareFormatter
import com.twitter.logging.Level
import com.twitter.logging.StringHandler
import org.junit.runner.RunWith
import org.scalatest.Assertion
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JsonLoggerTest extends StorageTestBase {

  private[this] val jsonLogger = JsonLogger(logger)
  private[this] val traceHandler = new StringHandler(BareFormatter, None)

  before {
    statsReceiver.clear()
    logger.addHandler(traceHandler)
  }

  after {
    logger.clearHandlers()
  }

  def logLines(): Seq[String] = traceHandler.get.split("\n")

  def mustLog(substring: String): Assertion = {
    assert(logLines().exists(_ contains substring))
  }

  def mustNotLog(substring: String): Assertion = {
    assert(!logLines().exists(_ contains substring))
  }

  test("test info logging through jsonlogger") {
    logger.setLevel(Level.DEBUG)
    jsonLogger.info("test1", None)
    mustLog("test1")
  }

  test("test info logging with scope through jsonlogger") {
    logger.setLevel(Level.DEBUG)
    jsonLogger.withScope("msg1").info("test1", None)
    mustLog("test1")
    mustLog("msg1")
  }

  test("test info logging with multiple scopes through jsonlogger") {
    logger.setLevel(Level.DEBUG)
    jsonLogger.withScope("msg1").withScope("msg2").withScope("msg3").info("test1", None)
    mustLog("test1")
    mustLog("msg1")
    mustLog("msg2")
    mustLog("msg3")
  }

  test("test error logging through jsonlogger") {
    logger.setLevel(Level.ERROR)
    jsonLogger.error("test2", Some(Map("key2" -> "val2")))
    mustLog("test2")
    mustLog("key2")
    mustLog("val2")
  }

  test("test warning logging through jsonlogger") {
    logger.setLevel(Level.WARNING)
    jsonLogger.error("test3", Some(Map("key4" -> "val4")))
    mustLog("test3")
    mustLog("key4")
    mustLog("val4")
  }

  test("test info logging through jsonlogger with complex metadata") {
    logger.setLevel(Level.DEBUG)
    jsonLogger.info("test1", Some(Map("ma" -> Map("mb" -> Map("mc" -> "md")))))
    mustLog("test1")
    mustLog("ma")
    mustLog("mb")
    mustLog("mc")
    mustLog("md")
  }

}
