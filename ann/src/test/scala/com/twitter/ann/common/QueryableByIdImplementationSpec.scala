package com.twitter.ann.common

import com.twitter.ml.api.embedding.Embedding
import com.twitter.stitch.Stitch
import com.twitter.util.{Await, Future}
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatestplus.mockito.MockitoSugar

class TestRuntimeParams extends RuntimeParams

class QueryableByIdImplementationSpec extends FunSuite with MockitoSugar {
  val mockQueryable = mock[Queryable[Long, TestRuntimeParams, L2Distance]]
  val mockEmbeddingProducer = mock[EmbeddingProducer[String]]
  val queryableByKey = new QueryableByIdImplementation(
    mockEmbeddingProducer,
    mockQueryable
  )

  val testEmbedding = Embedding[Float](Array(1.0F))
  val numNeighbors = 3
  val runtimeParams = new TestRuntimeParams
  val queryResults = List(1L, 2L, 3L)
  val queryWithDistanceResults = List(
    NeighborWithDistance(1L, L2Distance(10.0F)),
    NeighborWithDistance(2L, L2Distance(20.0F)),
    NeighborWithDistance(3L, L2Distance(30.0F))
  )

  val query = "input"

  test("queryById") {
    when(mockEmbeddingProducer.produceEmbedding(query))
      .thenReturn(Stitch.value(Some(testEmbedding)))

    when(mockQueryable.query(testEmbedding, numNeighbors, runtimeParams))
      .thenReturn(Future.value(queryResults))

    val future = queryableByKey.queryById(query, numNeighbors, runtimeParams)
    assert(Await.result(Stitch.run(future)) == queryResults)
  }

  test("queryById with None") {
    when(mockEmbeddingProducer.produceEmbedding(query))
      .thenReturn(Stitch.value(None))

    val future = queryableByKey.queryById(query, numNeighbors, runtimeParams)
    assert(Await.result(Stitch.run(future)) == List())
  }

  test("queryByIdWithDistance") {
    when(mockEmbeddingProducer.produceEmbedding(query))
      .thenReturn(Stitch.value(Some(testEmbedding)))

    when(mockQueryable.queryWithDistance(testEmbedding, numNeighbors, runtimeParams))
      .thenReturn(Future.value(queryWithDistanceResults))

    val future = queryableByKey.queryByIdWithDistance(query, numNeighbors, runtimeParams)
    assert(Await.result(Stitch.run(future)) == queryWithDistanceResults)
  }

  test("queryByIdWithDistance with None") {
    when(mockEmbeddingProducer.produceEmbedding(query))
      .thenReturn(Stitch.value(None))

    val future = queryableByKey.queryByIdWithDistance(query, numNeighbors, runtimeParams)
    assert(Await.result(Stitch.run(future)) == List())
  }
}
