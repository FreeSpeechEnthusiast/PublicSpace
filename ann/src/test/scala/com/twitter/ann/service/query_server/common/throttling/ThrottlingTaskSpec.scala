package com.twitter.ann.service.query_server.common.throttling

import com.twitter.ann.hnsw.HnswParams
import com.twitter.finagle.stats.InMemoryStatsReceiver
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ThrottlingTaskSpec extends AnyFunSuite with Matchers {
  test("Throttling is immediate") {
    val stats = new InMemoryStatsReceiver()
    val instrument = new ThrottlingInstrument {
      def sample(): Unit = ()
      def percentageOfTimeSpentThrottling(): Double = 1
      def disabled: Boolean = false
    }
    val task = new ThrottlingBasedQualityTask(stats, instrument)

    task.task()

    val params = HnswParams(ef = 100)
    val discountedParams = HnswParams(ef = 1)
    assert(task.discountParams(params) == discountedParams, "Params are discounted")
  }

  test("Full recovery takes a lot of time") {
    val stats = new InMemoryStatsReceiver()
    var throttling: Double = 1
    val instrument = new ThrottlingInstrument {
      def sample(): Unit = ()
      def percentageOfTimeSpentThrottling(): Double = throttling
      def disabled: Boolean = false
    }
    val task = new ThrottlingBasedQualityTask(stats, instrument)

    task.task()
    throttling = 0

    val params = HnswParams(ef = 100)

    (0 until 2600).foreach { _ => task.task() }

    val almostARecovery = HnswParams(ef = 99)
    assert(task.discountParams(params) == almostARecovery, "Params almost recovered")
  }

}
