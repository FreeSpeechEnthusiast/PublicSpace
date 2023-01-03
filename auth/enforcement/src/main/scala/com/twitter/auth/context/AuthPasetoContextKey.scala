package com.twitter.auth.context

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.stats.DefaultStatsReceiver
import com.twitter.io.Buf
import com.twitter.io.Buf.ByteArray.Owned
import com.twitter.util.Try
import java.nio.charset.StandardCharsets

object AuthPasetoContextKey
    extends Contexts.broadcast.Key[String](
      "com.twitter.server.auth.AuthPasetoContext"
    ) {

  private[this] val statsReceiver = DefaultStatsReceiver.scope("auth_paseto_context_key")
  private[this] val marshalCounter = statsReceiver.counter("marshal")
  private[this] val unmarshalCounter = statsReceiver.counter("unmarshal")

  def marshal(value: String): Buf = {
    marshalCounter.incr()
    Owned(value.getBytes(StandardCharsets.UTF_8))
  }

  def tryUnmarshal(buf: Buf): Try[String] = {
    unmarshalCounter.incr()
    Try(new String(Owned.extract(buf), StandardCharsets.UTF_8))
  }
}
