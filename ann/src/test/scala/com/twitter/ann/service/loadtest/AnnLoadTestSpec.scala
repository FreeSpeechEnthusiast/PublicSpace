package com.twitter.ann.service.loadtest

import com.twitter.ann.common.{Appendable, CosineDistance, EntityEmbedding, Queryable}
import com.twitter.util.{Await, Duration, Future}
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatestplus.mockito.MockitoSugar

class AnnLoadTestSpec extends FunSuite with MockitoSugar {
  val buildRecorder = mock[LoadTestBuildRecorder]
  val worker = mock[AnnLoadTestWorker]
  val appendable = mock[Appendable[Long, TestParams, CosineDistance]]
  val queryable = mock[Queryable[Long, TestParams, CosineDistance]]
  val indexer = mock[EmbeddingIndexer]
  val duration = Duration.fromSeconds(1)

  val queryLoadTest = new AnnIndexQueryLoadTest(
    worker
  )

  val buildLoadTest = new AnnIndexBuildLoadTest(
    buildRecorder,
    indexer
  )

  val indexEmbeddings = mock[Seq[EntityEmbedding[Long]]]

  val queryEmbeddings = mock[Seq[Query[Long]]]

  val concurrencyLevel = 3

  val config = QueryTimeConfiguration[Long, TestParams](null, null, 3, null)

  val runtimeConfigurations = Seq(config)

  test("load test index query") {
    when(
      worker.runWithQps(
        queryable,
        queryEmbeddings,
        qps = 10,
        duration,
        config,
        concurrencyLevel
      )
    ).thenReturn(Future.value(1))
    val res = queryLoadTest.performQueries(
      queryable,
      qps = 10,
      duration,
      queryEmbeddings,
      concurrencyLevel,
      runtimeConfigurations
    )
    Await.result(res)
  }

  test("load test index build") {
    when(indexer.indexEmbeddings(appendable, buildRecorder, indexEmbeddings, concurrencyLevel))
      .thenReturn(Future.value(queryable))
    val res = Await.result(
      buildLoadTest.indexEmbeddings(
        appendable,
        indexEmbeddings,
        concurrencyLevel
      )
    )

    assert(res == queryable)
  }
}
