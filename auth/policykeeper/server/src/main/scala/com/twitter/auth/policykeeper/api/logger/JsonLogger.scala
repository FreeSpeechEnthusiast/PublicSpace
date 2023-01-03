package com.twitter.auth.policykeeper.api.logger

import com.twitter.logging.Logger
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

case class JsonLogger(logger: Logger, private val scope: List[String] = List()) {
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  private val Message = "message"
  private val Scope = "scope"
  private val JsonConversionErrorMessage =
    "Failed to convert message payload into json"

  def withScope(newScope: String): JsonLogger = {
    this.copy(logger = logger, scope = newScope :: scope)
  }

  private def catchJsonException(
    f: () => Unit
  ): Unit = {
    try {
      f()
    } catch {
      case e: Throwable =>
        logger.error(JsonConversionErrorMessage, e)
    }
  }

  private def addMetadata(
    message: Map[String, Any],
    metadata: Option[Map[String, Any]]
  ): Map[String, Any] = {
    metadata match {
      case Some(m) =>
        message ++ m
      case _ => message
    }
  }

  private def scopedMessage(
    message: String,
    metadata: Option[Map[String, Any]]
  ): String = {
    mapper.writeValueAsString(
      addMetadata(Map(Message -> message, Scope -> scope), metadata)
    )
  }

  def info(
    message: String,
    metadata: Option[Map[String, Any]]
  ): Unit = {
    catchJsonException(() => logger.info(scopedMessage(message, metadata)))
  }

  def warning(
    message: String,
    metadata: Option[Map[String, Any]]
  ): Unit = {
    catchJsonException(() => logger.warning(scopedMessage(message, metadata)))
  }

  def error(
    message: String,
    metadata: Option[Map[String, Any]]
  ): Unit = {
    catchJsonException(() => logger.error(scopedMessage(message, metadata)))
  }
}
