package com.twitter.ann.service.loadtest

import com.twitter.finagle.stats.{InMemoryStatsReceiver, MetricsBucketedHistogram, Snapshot}
import com.twitter.util.{Duration, Time}
import org.junit.Assert._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import org.scalatestplus.mockito.MockitoSugar

class LoadTestRecorderSpec extends FunSuite with BeforeAndAfter with Matchers with MockitoSugar {
  val statsReceiver = new InMemoryStatsReceiver
  val mockHistogram = mock[MetricsBucketedHistogram]
  val mockSnapshot = mock[Snapshot]

  val epsilon = 1e-3
  // MetricsBucketedHistogram produces estimates for the percentile values. Use a bigger epsilon on
  // the results to take this into account.
  val latencyForTest = Duration.fromMilliseconds(1)

  val statsQueryRecorder = new StatsLoadTestQueryRecorder[Int](statsReceiver)

  val statsBuildRecorder = new StatsLoadTestBuildRecorder(statsReceiver)
  val inMemoryBuildRecorder = new InMemoryLoadTestBuildRecorder

  before {
    statsReceiver.clear()
    reset(mockHistogram)
    when(mockHistogram.snapshot()).thenReturn(mockSnapshot)
  }

  test("number of results stats are increased correctly") {
    val foundNeighbors = Seq(1, 2, 3, 4, 5)
    val trueNeighbors = Seq(1, 2, 3, 4)
    statsQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
    statsReceiver.stats.get(Seq("number_of_results")) shouldBe Some(Seq(5.0))
  }

  test("recall is computed correctly") {
    val trueNeighbors = Seq(1, 2, 3, 4, 5)
    val foundNeighbors = Seq(2, 3, 4, 6, 7)
    val intersect = trueNeighbors.intersect(foundNeighbors)
    statsQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
    assertEquals(
      intersect.size.toFloat / foundNeighbors.size * 100,
      statsReceiver.stats(Seq("recall")).head,
      epsilon)
    assertEquals(0.0, statsReceiver.stats(Seq("top_1_recall")).head, epsilon)
    statsReceiver.stats.get(Seq("top_10_recall")) shouldBe None
  }

  test("top 1 recall is computed correctly") {
    val trueNeighbors = Seq(1, 2, 3, 4, 5)
    val foundNeighbors = Seq(1, 4, 6, 7, 8)
    val intersect = trueNeighbors.intersect(foundNeighbors)
    statsQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
    assertEquals(
      intersect.size.toFloat / foundNeighbors.size * 100,
      statsReceiver.stats(Seq("recall")).head,
      epsilon)
    assertEquals(100.0, statsReceiver.stats(Seq("top_1_recall")).head, epsilon)
    statsReceiver.stats.get(Seq("top_10_recall")) shouldBe None
  }

  test("top 10 recall is computed correctly") {
    val trueNeighbors = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
    val foundNeighbors = Seq(1, 2, 3, 4, 6, 11, 12, 13, 14, 15, 16)
    statsQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
    assertEquals(6.0 / 11 * 100, statsReceiver.stats(Seq("recall")).head, epsilon)
    assertEquals(100.0, statsReceiver.stats(Seq("top_1_recall")).head, epsilon)
    assertEquals(50.0, statsReceiver.stats(Seq("top_10_recall")).head, epsilon)
  }

  test("top 10 recall is computed correctly with fewer than 10 true neighbors") {
    val trueNeighbors = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val foundNeighbors = Seq(1, 2, 3, 4, 6, 11, 12, 13, 14, 15, 16)
    statsQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
    assertEquals(5.0 / 9 * 100, statsReceiver.stats(Seq("recall")).head, epsilon)
    assertEquals(100.0, statsReceiver.stats(Seq("top_1_recall")).head, epsilon)
    assert(!statsReceiver.stats.isDefinedAt(Seq("top_10_recall")))
  }

  test("top 10 recall is computed correctly with fewer than 10 found neighbors") {
    val trueNeighbors = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val foundNeighbors = Seq(1, 2, 3, 4, 6)
    statsQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
    assertEquals(80, statsReceiver.stats(Seq("recall")).head, epsilon)
    assertEquals(100.0, statsReceiver.stats(Seq("top_1_recall")).head, epsilon)
    assert(!statsReceiver.stats.isDefinedAt(Seq("top_10_recall")))
  }

  test("index latency is recorded correctly") {
    val indexLatency = Duration.fromMilliseconds(10)
    val toQueryableLatency = Duration.fromMilliseconds(20)
    val indexSize = 2
    statsBuildRecorder.recordIndexCreation(indexSize, indexLatency, toQueryableLatency)
    assertEquals(10.0, statsReceiver.gauges(Seq("index_latency_ms"))(), epsilon)
    assertEquals(20.0, statsReceiver.gauges(Seq("to_queryable_latency_ms"))(), epsilon)
    assertEquals(2.0, statsReceiver.gauges(Seq("index_size"))(), epsilon)
  }

  test("in memory query recorder records stats") {
    val printableQueryRecorder = new InMemoryLoadTestQueryRecorder[Int](mockHistogram)
    val trueNeighbors = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
    val foundNeighbors = Seq(1, 2, 3, 4, 6, 11, 12, 13, 14, 15, 16)
    Time.withCurrentTimeFrozen { timeControl =>
      printableQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
      verify(mockHistogram, times(1)).add(1000)
      when(mockSnapshot.average).thenReturn(20)
      when(mockSnapshot.percentiles).thenReturn(
        IndexedSeq(Snapshot.Percentile(.5, 22)),
        IndexedSeq(Snapshot.Percentile(.9, 24)),
        IndexedSeq(Snapshot.Percentile(.99, 26))
      )
      val snapshot1 = printableQueryRecorder.computeSnapshot()
      assertEquals(20, snapshot1.avgQueryLatencyMicros, epsilon)
      assertEquals(22, snapshot1.p50QueryLatencyMicros, epsilon)
      assertEquals(24, snapshot1.p90QueryLatencyMicros, epsilon)
      assertEquals(26, snapshot1.p99QueryLatencyMicros, epsilon)
      assertEquals(54.545456, printableQueryRecorder.recall, epsilon)
      assertEquals(100, printableQueryRecorder.top1Recall, epsilon)
      assertEquals(50, printableQueryRecorder.top10Recall, epsilon)
      // 1 request in 1 millisecond
      assertEquals(1000, printableQueryRecorder.avgRPS, epsilon)
      timeControl.advance(Duration.fromSeconds(1))
      printableQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
      verify(mockHistogram, times(2)).add(1000)
      val snapshot2 = printableQueryRecorder.computeSnapshot()
      when(mockSnapshot.average).thenReturn(30)
      when(mockSnapshot.percentiles).thenReturn(
        IndexedSeq(Snapshot.Percentile(.5, 32)),
        IndexedSeq(Snapshot.Percentile(.9, 34)),
        IndexedSeq(Snapshot.Percentile(.99, 36))
      )
      assertEquals(54.545456, printableQueryRecorder.recall, epsilon)
      assertEquals(100, printableQueryRecorder.top1Recall, epsilon)
      assertEquals(50, printableQueryRecorder.top10Recall, epsilon)
      assertEquals(30, snapshot2.avgQueryLatencyMicros, epsilon)
      assertEquals(32, snapshot2.p50QueryLatencyMicros, epsilon)
      assertEquals(34, snapshot2.p90QueryLatencyMicros, epsilon)
      assertEquals(36, snapshot2.p99QueryLatencyMicros, epsilon)
      // 2 requests in 1.0001 seconds
      assertEquals(1.998001998, printableQueryRecorder.avgRPS, epsilon)
    }
  }

  test("in memory query recorder records stats with fewer than 10 true neighbors") {
    val printableQueryRecorder = new InMemoryLoadTestQueryRecorder[Int]
    val trueNeighbors = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val foundNeighbors = Seq(1, 2, 3, 4, 6, 11, 12, 13, 14, 15, 16)
    Time.withCurrentTimeFrozen { timeControl =>
      printableQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
      assertEquals(55.555555555, printableQueryRecorder.recall, epsilon)
      assertEquals(100.0, printableQueryRecorder.top1Recall, epsilon)
      assertEquals(0, printableQueryRecorder.top10Recall, epsilon)
      // 1 request in 1 millisecond
      assertEquals(1000, printableQueryRecorder.avgRPS, epsilon)
      timeControl.advance(Duration.fromSeconds(1))
      printableQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
      val snapshot = printableQueryRecorder.computeSnapshot()
      assertEquals(55.555555555, printableQueryRecorder.recall, epsilon)
      assertEquals(100.0, printableQueryRecorder.top1Recall, epsilon)
      assertEquals(0, printableQueryRecorder.top10Recall, epsilon)
      assertEquals(1000, snapshot.avgQueryLatencyMicros, epsilon)
      // 2 requests in 1.0001 seconds
      assertEquals(1.998001998, printableQueryRecorder.avgRPS, epsilon)
    }
  }

  test("in memory query recorder records stats with fewer than 10 found neighbors") {
    val printableQueryRecorder = new InMemoryLoadTestQueryRecorder[Int]
    val trueNeighbors = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val foundNeighbors = Seq(1, 2, 3, 4, 6)
    Time.withCurrentTimeFrozen { timeControl =>
      printableQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
      assertEquals(80, printableQueryRecorder.recall, epsilon)
      assertEquals(100.0, printableQueryRecorder.top1Recall, epsilon)
      assertEquals(0, printableQueryRecorder.top10Recall, epsilon)
      // 1 request in 1 millisecond
      assertEquals(1000, printableQueryRecorder.avgRPS, epsilon)
      timeControl.advance(Duration.fromSeconds(1))
      printableQueryRecorder.recordQueryResult(trueNeighbors, foundNeighbors, latencyForTest)
      val snapshot = printableQueryRecorder.computeSnapshot()
      assertEquals(80, printableQueryRecorder.recall, epsilon)
      assertEquals(100, printableQueryRecorder.top1Recall, epsilon)
      assertEquals(0, printableQueryRecorder.top10Recall, epsilon)
      assertEquals(1000, snapshot.avgQueryLatencyMicros, epsilon)
      // 2 requests in 1.0001 seconds
      assertEquals(1.998001998, printableQueryRecorder.avgRPS, epsilon)
    }
  }

  test("printable build recorder toString properly") {
    val indexLatency = Duration.fromMilliseconds(10)
    val toQueryableLatency = Duration.fromMilliseconds(20)
    val indexSize = 2
    inMemoryBuildRecorder.recordIndexCreation(indexSize, indexLatency, toQueryableLatency)
    inMemoryBuildRecorder.indexSize shouldBe 2
    inMemoryBuildRecorder.indexLatency shouldBe indexLatency
    inMemoryBuildRecorder.toQueryableLatency shouldBe toQueryableLatency
  }
}
