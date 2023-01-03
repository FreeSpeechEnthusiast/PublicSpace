package com.twitter.auth.sso.util

import com.twitter.finagle.stats.{Counter, Stat, StatsReceiver}
import com.twitter.servo.util.ExceptionCounter
import com.twitter.stitch.Stitch

case class Monitor(private val scoped: StatsReceiver) {
  private lazy val attempt: Counter = scoped.counter("attempt")
  private lazy val success: Counter = scoped.counter("success")
  private lazy val size: Stat = scoped.stat("size")
  private lazy val exception: ExceptionCounter = new ExceptionCounter(scoped)

  private def incrAttempt(): Unit = attempt.incr()
  private def incrSuccess(): Unit = success.incr()
  private def addSize(length: Float): Unit = size.add(length)

  def trackStitchSeq[E](st: Stitch[Seq[E]]): Stitch[Seq[E]] = {
    incrAttempt()
    st.onSuccess { data => incrSuccess(); addSize(data.length) }
      .onFailure { e => exception(e) }
  }

  def trackStitch[E](st: Stitch[E]): Stitch[E] = {
    incrAttempt()
    st.onSuccess { _ => incrSuccess() }
      .onFailure { e => exception(e) }
  }
}
