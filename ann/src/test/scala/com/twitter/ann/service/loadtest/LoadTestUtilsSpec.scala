package com.twitter.ann.service.loadtest

import com.twitter.util.Duration
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.Matchers
import scala.collection.mutable
import org.scalatestplus.mockito.MockitoSugar

class LoadTestUtilsSpec extends FunSuite with Matchers with MockitoSugar {
  test("Knn format is load correctly into map") {
    val map = mutable.HashMap[Long, Seq[Int]]()
    val s = "2\t10\t20"
    LoadTestUtils.addToMapFromKnnString(s, arr => arr.map(_.toInt), map, str => str.toLong)
    map.toMap.size shouldBe 1
    map.toMap.get(2L).get.toSet shouldBe Set(10, 20)
  }

  test("printResults") {
    val buildRecorder = mock[InMemoryLoadTestBuildRecorder]

    val queryRecorder1 = mock[InMemoryLoadTestQueryRecorder[Long]]
    val queryRecorder2 = mock[InMemoryLoadTestQueryRecorder[Long]]

    val snapshot1 = mock[QueryRecorderSnapshot]
    val snapshot2 = mock[QueryRecorderSnapshot]

    when(queryRecorder1.computeSnapshot()).thenReturn(snapshot1)
    when(queryRecorder2.computeSnapshot()).thenReturn(snapshot2)

    val param1 = mock[TestParams]
    val param2 = mock[TestParams]

    when(param1.toString).thenReturn("param1")
    when(param2.toString).thenReturn("param2")

    when(buildRecorder.indexLatency).thenReturn(Duration.fromSeconds(1))
    when(buildRecorder.toQueryableLatency).thenReturn(Duration.fromSeconds(1))
    when(buildRecorder.indexSize).thenReturn(2)
    when(buildRecorder.toQueryableLatency).thenReturn(Duration.fromSeconds(1))

    val config1 = QueryTimeConfiguration(null, param1, 3, queryRecorder1)
    val config2 = QueryTimeConfiguration(null, param2, 4, queryRecorder2)

    when(queryRecorder1.top1Recall).thenReturn(5.5f)
    when(queryRecorder1.top10Recall).thenReturn(6.5f)
    when(queryRecorder1.recall).thenReturn(10f)
    when(snapshot1.avgQueryLatencyMicros).thenReturn(7.0f)
    when(snapshot1.p50QueryLatencyMicros).thenReturn(20f)
    when(snapshot1.p90QueryLatencyMicros).thenReturn(21f)
    when(snapshot1.p99QueryLatencyMicros).thenReturn(22f)
    when(queryRecorder1.avgRPS).thenReturn(11.5f)

    when(queryRecorder2.top1Recall).thenReturn(8.5f)
    when(queryRecorder2.top10Recall).thenReturn(9.5f)
    when(queryRecorder2.recall).thenReturn(12f)
    when(snapshot2.avgQueryLatencyMicros).thenReturn(10.0f)
    when(snapshot2.p50QueryLatencyMicros).thenReturn(13f)
    when(snapshot2.p90QueryLatencyMicros).thenReturn(14f)
    when(snapshot2.p99QueryLatencyMicros).thenReturn(15f)
    when(queryRecorder2.avgRPS).thenReturn(12.5f)

    val s = "2\t10\t20"
    val results = LoadTestUtils.printResults(buildRecorder, Seq(config1, config2))
    results shouldBe Seq(
      "Build results",
      "indexingTimeSecs\ttoQueryableTimeMs\tindexSize",
      "1\t1000\t2",
      "Query results",
      "params\tnumNeighbors\trecall@1\trecall@10\trecall\tavgLatencyMicros\tp50LatencyMicros\tp90LatencyMicros\tp99LatencyMicros\tavgRPS",
      "param1\t3\t5.5\t6.5\t10.0\t7.0\t20.0\t21.0\t22.0\t11.5",
      "param2\t4\t8.5\t9.5\t12.0\t10.0\t13.0\t14.0\t15.0\t12.5"
    )
  }
}
