package com.twitter.ann.service.loadtest

import com.twitter.ann.common.{CosineDistance, Queryable, RuntimeParams}
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.ml.api.embedding.Embedding
import com.twitter.util.{Await, Duration, Future, MockTimer, Time}
import org.mockito.ArgumentMatchers._
import org.mockito.ArgumentMatchers.{eq => mEq}
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatestplus.mockito.MockitoSugar

case class TestParams(long: Long) extends RuntimeParams
class AnnLoadTestWorkerSpec extends FunSuite with MockitoSugar with BeforeAndAfter {

  val epsilon = 1e-3
  val statsReceiver = NullStatsReceiver
  val recorder = mock[StatsLoadTestQueryRecorder[Long]]

  val numNeighbor = 5
  val numOfNodesToExplore = 10

  val trueNeighbors1 = Seq(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L)
  val foundNeighbors1 = Seq(11L, 21L, 31L, 41L, 51L, 61L, 71L, 81L, 91L, 101L)

  val trueNeighbors2 = Seq(12L, 22L, 32L, 42L, 52L, 62L, 72L, 82L, 92L, 102L)
  val foundNeighbors2 = Seq(13L, 23L, 33L, 43L, 53L, 63L, 73L, 83L, 93L, 103L)

  val queries = Seq(
    Query(Embedding(Array(1.0f, 2.0f, 3.0f)), trueNeighbors1),
    Query(Embedding(Array(3.0f, 2.0f, 1.0f)), trueNeighbors2)
  )

  val queryable = mock[Queryable[Long, TestParams, CosineDistance]]
  val testRuntimeParams = mock[TestParams]

  val configuration = QueryTimeConfiguration(
    recorder,
    testRuntimeParams,
    numNeighbor,
    new InMemoryLoadTestQueryRecorder[Long]
  )

  before {
    reset(queryable, recorder)
  }

  test("query is selected correctly") {
    when(queryable.query(Embedding(Array(1.0f, 2.0f, 3.0f)), numNeighbor, testRuntimeParams))
      .thenReturn(
        Future.value(foundNeighbors1.toList)
      )
    when(queryable.query(Embedding(Array(3.0f, 2.0f, 1.0f)), numNeighbor, testRuntimeParams))
      .thenReturn(
        Future.value(foundNeighbors2.toList)
      )
    Time.withCurrentTimeFrozen { timeControl =>
      val timer = new MockTimer()

      val worker = new AnnLoadTestWorker(timer)

      val future = worker.runWithQps(
        queryable,
        queries,
        1,
        Duration.fromMilliseconds(1),
        configuration,
        1
      )
      // The test should only happen for 1 millisecond.
      // So at this point we should not allow any more queries.
      timeControl.advance(Duration.fromMilliseconds(1))
      timer.tick()
      // There is only 1 query per second. So this should make sure that a second request would have
      // been allowed.
      timeControl.advance(Duration.fromSeconds(1))
      timer.tick()
      // No more invocation should happen after this.
      timeControl.advance(Duration.fromSeconds(30))
      timer.tick()
      Await.result(future)
    }
    // Verify that the embedding methods are all invoked correctly and a limited number of times.
    verify(queryable, times(1))
      .query(Embedding(Array(1.0f, 2.0f, 3.0f)), numNeighbor, testRuntimeParams)
    verify(queryable, times(1))
      .query(Embedding(Array(3.0f, 2.0f, 1.0f)), numNeighbor, testRuntimeParams)
    verify(recorder, times(1))
      .recordQueryResult(trueNeighbors1, foundNeighbors1, Duration.Zero)
    verify(recorder, times(1))
      .recordQueryResult(trueNeighbors2, foundNeighbors2, Duration.Zero)
  }

  test("query without truth set") {
    val query = Query[Long](queries(0).embedding)

    when(queryable.query(query.embedding, numNeighbor, testRuntimeParams)).thenReturn(
      Future.value(foundNeighbors1.toList)
    )

    val worker = new AnnLoadTestWorker()
    Await.result(worker.performQuery(configuration, queryable, query))

    verify(queryable, times(1))
      .query(query.embedding, numNeighbor, testRuntimeParams)
    verify(recorder, times(1))
      .recordQueryResult(mEq(Seq.empty), mEq(foundNeighbors1), any[Duration])
  }

  test("a query failed") {
    val exception = new Exception("AnnLoadTestWorkerSpec")
    when(queryable.query(Embedding(Array(1.0f, 2.0f, 3.0f)), numNeighbor, testRuntimeParams))
      .thenReturn(
        Future.exception(exception)
      )

    when(queryable.query(Embedding(Array(3.0f, 2.0f, 1.0f)), numNeighbor, testRuntimeParams))
      .thenReturn(
        Future.exception(exception)
      )

    val res = Time.withCurrentTimeFrozen { timeControl =>
      val timer = new MockTimer()

      val worker = new AnnLoadTestWorker(timer)

      val future = worker.runWithQps(
        queryable,
        queries,
        1,
        Duration.fromMilliseconds(1),
        configuration,
        1
      )
      // The test should only happen for 1 millisecond.
      // So at this point we should not allow any more queries.
      timeControl.advance(Duration.fromMilliseconds(1))
      timer.tick()
      // First request. Queryable throws exception
      timeControl.advance(Duration.fromSeconds(1))
      timer.tick()
      // Second request. Queryable throws exception
      timeControl.advance(Duration.fromSeconds(30))
      timer.tick()
      Await.result(future)
    }

    assert(res == 2)
    verify(queryable, times(1))
      .query(Embedding(Array(1.0f, 2.0f, 3.0f)), numNeighbor, testRuntimeParams)
    verify(queryable, times(1))
      .query(Embedding(Array(3.0f, 2.0f, 1.0f)), numNeighbor, testRuntimeParams)
    verify(recorder, never())
      .recordQueryResult(any(), any(), any())
  }
}
