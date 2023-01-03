package com.twitter.auth.audit

import com.twitter.guano.thriftscala.ScribeMessage
import com.twitter.logging.Logger
import com.twitter.servo.cache.ThriftSerializer
import org.apache.thrift.protocol.TBinaryProtocol

object AuthGuanoScriber {
  val TrustEngAuditCategory = "trust_eng_audit"

  private val guanoScribeLogger = Logger.get(TrustEngAuditCategory)
  private val binaryScribeMessageSerializer =
    new ThriftSerializer(ScribeMessage, new TBinaryProtocol.Factory)
}

/**
 * Threadsafe
 */
class AuthGuanoScriber {
  import AuthGuanoScriber._

  def scribe(scribeMessage: ScribeMessage): Unit = {
    guanoScribeLogger.info(
      binaryScribeMessageSerializer.toString(scribeMessage)
    )
  }
}
