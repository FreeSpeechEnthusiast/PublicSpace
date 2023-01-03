package com.twitter.auth.pasetoheaders.finagle

import com.twitter.decider.Feature
import com.twitter.logging.Logger
import com.twitter.auth.pasetoheaders.encryption.LoggingInterface
import java.util
import java.util.Optional
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

private case class LoggerConnector(logger: Logger, loggingEnabledDecider: Option[Feature] = None) {
  private val MESSAGE: String = "message"
  private val SCOPE: String = "scope"
  private val JSON_CONVERSION_ERROR_MESSAGE =
    "PasetoHeaders logging: Failed to convert to Json logger message."

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  private def scopedMessage(
    scope: String,
    message: String,
    metadata: Option[Map[String, String]]
  ): String = {
    mapper.writeValueAsString(
      addMetadata(Map(MESSAGE -> message, SCOPE -> scope), metadata)
    )
  }

  private def catchJsonException(
    f: () => Unit
  ): Unit = {
    try {
      f()
    } catch {
      case e: Throwable =>
        logger.error(JSON_CONVERSION_ERROR_MESSAGE, e)
    }
  }

  private def checkIfLoggingEnabledInDecider(
    f: () => Unit
  ): Unit = {
    loggingEnabledDecider match {
      case Some(feature) if feature.isAvailable => f()
      case None => f()
      case _ =>
    }
  }

  private def addMetadata(
    message: Map[String, String],
    metadata: Option[Map[String, String]]
  ): Map[String, String] = {
    metadata match {
      case Some(m) =>
        message ++ m
      case _ => message
    }
  }

  def debug(
    scope: String,
    message: String,
    metadata: Option[Map[String, String]]
  ): Unit = {
    catchJsonException(() =>
      checkIfLoggingEnabledInDecider(() => logger.debug(scopedMessage(scope, message, metadata))))
  }

  def info(
    scope: String,
    message: String,
    metadata: Option[Map[String, String]]
  ): Unit = {
    catchJsonException(() =>
      checkIfLoggingEnabledInDecider(() => logger.info(scopedMessage(scope, message, metadata))))
  }

  def warn(
    scope: String,
    message: String,
    metadata: Option[Map[String, String]]
  ): Unit = {
    catchJsonException(() =>
      checkIfLoggingEnabledInDecider(() => logger.warning(scopedMessage(scope, message, metadata))))
  }

  def error(
    scope: String,
    message: String,
    metadata: Option[Map[String, String]]
  ): Unit = {
    catchJsonException(() =>
      checkIfLoggingEnabledInDecider(() => logger.error(scopedMessage(scope, message, metadata))))
  }

  def trace(
    scope: String,
    message: String,
    metadata: Option[Map[String, String]]
  ): Unit = {
    catchJsonException(() =>
      checkIfLoggingEnabledInDecider(() => logger.trace(scopedMessage(scope, message, metadata))))
  }
}

/**
 * proxy classes provide compatibility with Java interfaces
 */
case class FinagleLoggerProxy(logger: Logger, loggingEnabledDecider: Option[Feature] = None)
    extends LoggingInterface {

  import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
  import com.twitter.auth.pasetoheaders.javahelpers.MapConv._

  lazy private val loggerConnector = LoggerConnector(logger, loggingEnabledDecider);

  override def debug(
    scope: String,
    message: String,
    metadata: Optional[util.Map[String, String]]
  ): Unit = {
    loggerConnector.debug(scope, message, metadata)
  }

  override def info(
    scope: String,
    message: String,
    metadata: Optional[util.Map[String, String]]
  ): Unit = {
    loggerConnector.info(scope, message, metadata)
  }

  override def warn(
    scope: String,
    message: String,
    metadata: Optional[util.Map[String, String]]
  ): Unit = {
    loggerConnector.warn(scope, message, metadata)
  }

  override def error(
    scope: String,
    message: String,
    metadata: Optional[util.Map[String, String]]
  ): Unit = {
    loggerConnector.error(scope, message, metadata)
  }

  override def trace(
    scope: String,
    message: String,
    metadata: Optional[util.Map[String, String]]
  ): Unit = {
    loggerConnector.trace(scope, message, metadata)
  }
}
