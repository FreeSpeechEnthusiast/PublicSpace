package com.twitter.auth.context

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.http.Message
import com.twitter.finagle.stats.DefaultStatsReceiver
import com.twitter.finagle.tracing.Trace

object AuthPasetoContext {
  private[auth] val AuthPasetoHeaderKey = "com.twitter.server.auth.AuthPasetoContext"

  private[this] val statsReceiver = DefaultStatsReceiver
  private[this] val scopedStatsReceiver = statsReceiver.scope("auth_paseto_context")

  private[this] def marshalAuthPasetoContext(value: String): Option[String] = {
    Some(value)
  }

  private[this] def unmarshalAuthPasetoContext(header: String): Option[String] = {
    Some(header)
  }

  /**
   * Retrieve from broadcast contexts
   */
  def getFromContexts: Option[String] = {
    Contexts.broadcast.get(AuthPasetoContextKey)
  }

  /**
   * Write to broadcast contexts and run `fn`.
   */
  def writeToContexts[R](ctx: String)(fn: => R): R = {
    Contexts.broadcast.let(AuthPasetoContextKey, ctx)(fn)
  }

  /**
   * Trace if tracing is enabled
   */
  def trace(): Unit = {
    if (Trace.isActivelyTracing) {
      if (getFromContexts.isDefined) {
        Trace.recordBinary("auth.paseto.context", getFromContexts.get)
      } else {
        Trace.recordBinary("auth.paseto.context", "null")
      }
    }
  }

  /**
   * Read AuthContext header from the given message and run `fn`.
   */
  private[auth] def readFromHeader[R](msg: Message)(fn: => R): R = {
    msg.headerMap.get(AuthPasetoHeaderKey).flatMap(unmarshalAuthPasetoContext) match {
      case Some(authPasetoContext) =>
        Contexts.broadcast.let(AuthPasetoContextKey, authPasetoContext)(fn)
      case None =>
        fn
    }
  }

  /**
   * Write AuthContext in request header of the given message:
   */
  private[auth] def writeToHeader(msg: Message): Unit = {
    Contexts.broadcast.get(AuthPasetoContextKey) match {
      case Some(authPasetoContext) =>
        marshalAuthPasetoContext(authPasetoContext) match {
          case Some(s) => msg.headerMap.set(AuthPasetoHeaderKey, s)
          case _ =>
        }
      case None =>
    }
  }

}
