package com.twitter.auth.pasetoheaders.finagle

import com.twitter.auth.pasetoheaders.encryption.{StatsCounter, StatsInterface}
import com.twitter.finagle.stats.{Counter, StatsReceiver}
import java.{lang, util}
import java.util.Optional

private case class FinagleStats(statsReceiver: StatsReceiver) {
  protected val signingServiceScope = statsReceiver.scope("paseto", "signingservice")
  private[this] lazy val signingRequestedCounter = FinagleStatsCounterProxy(
    signingServiceScope.counter("signing_requested"))
  private[this] lazy val privateKeyNotFoundCounter = FinagleStatsCounterProxy(
    signingServiceScope.counter("private_key_not_found"))
  private[this] lazy val signingCompletedCounter = FinagleStatsCounterProxy(
    signingServiceScope.counter("signing_completed"))
  private[this] lazy val tokenSizeCounter = FinagleStatsCounterProxy(
    signingServiceScope.counter("token_size"))

  protected val claimServiceScope = statsReceiver.scope("paseto", "claimservice")
  private[this] lazy val extractionRequestedCounter = FinagleStatsCounterProxy(
    claimServiceScope.counter("extraction_requested"))
  private[this] lazy val extractionSucceededCounter = FinagleStatsCounterProxy(
    claimServiceScope.counter("extraction_succeeded"))
  private[this] lazy val extractionFailedCounter = FinagleStatsCounterProxy(
    claimServiceScope.counter("extraction_failed"))
  private[this] lazy val unverifiedExtractionRequestedCounter = FinagleStatsCounterProxy(
    claimServiceScope.counter("unverified_extraction_requested"))
  private[this] lazy val unverifiedExtractionSucceededCounter = FinagleStatsCounterProxy(
    claimServiceScope.counter("unverified_extraction_succeeded"))
  private[this] lazy val unverifiedExtractionFailedCounter = FinagleStatsCounterProxy(
    claimServiceScope.counter("unverified_extraction_failed"))
  private[this] lazy val wrongKeyIdentifierCounter = FinagleStatsCounterProxy(
    claimServiceScope.counter("wrong_key_identifier"))
  private[this] lazy val publicKeyNotFoundCounter = FinagleStatsCounterProxy(
    claimServiceScope.counter("public_key_not_found"))
  private[this] lazy val nullCounter = NullStatsCounter()

  def counter(
    scope: String,
    name: String,
    metadata: Option[Map[String, String]]
  ): StatsCounter = {
    scope match {
      case "signing_service" if name == "signing_requested" =>
        signingRequestedCounter
      case "signing_service" if name == "private_key_not_found" =>
        privateKeyNotFoundCounter
      case "signing_service" if name == "signing_completed" =>
        signingCompletedCounter
      case "signing_service" if name == "token_size" =>
        tokenSizeCounter
      case "claim_service" if name == "extraction_requested" =>
        extractionRequestedCounter
      case "claim_service" if name == "extraction_succeeded" =>
        extractionSucceededCounter
      case "claim_service" if name == "extraction_failed" =>
        extractionFailedCounter
      case "claim_service" if name == "unverified_extraction_requested" =>
        unverifiedExtractionRequestedCounter
      case "claim_service" if name == "unverified_extraction_succeeded" =>
        unverifiedExtractionSucceededCounter
      case "claim_service" if name == "unverified_extraction_failed" =>
        unverifiedExtractionFailedCounter
      case "claim_service" if name == "wrong_key_identifier" =>
        wrongKeyIdentifierCounter
      case "claim_service" if name == "public_key_not_found" =>
        publicKeyNotFoundCounter
      case _ => nullCounter
    }
  }
}

private case class NullStatsCounter() extends StatsCounter {
  override def incr(delta: lang.Long): Unit = {}
}

private case class FinagleStatsCounter(counter: Counter) {
  def incr(delta: Long): Unit = {
    counter.incr(delta);
  }
}

/**
 * proxy classes provide compatibility with Java interfaces
 */
case class FinagleStatsProxy(statsReceiver: StatsReceiver) extends StatsInterface {
  lazy private val finagleStatsReceiver = FinagleStats(statsReceiver)

  import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
  import com.twitter.auth.pasetoheaders.javahelpers.MapConv._

  override def counter(
    scope: String,
    name: String,
    metadata: Optional[util.Map[String, String]]
  ): StatsCounter = {
    finagleStatsReceiver.counter(scope, name, metadata)
  }
}

private case class FinagleStatsCounterProxy(counter: Counter) extends StatsCounter {
  lazy private val finagleStatsCounter = FinagleStatsCounter(counter)

  override def incr(delta: lang.Long): Unit = {
    finagleStatsCounter.incr(delta)
  }
}
