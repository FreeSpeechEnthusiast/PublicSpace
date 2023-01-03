package com.twitter.ann.scalding.offline

import com.twitter.ann.common.{CosineDistance, NeighborWithDistance, Queryable}
import com.twitter.ann.hnsw.HnswParams
import com.twitter.ml.api.embedding.Embedding
import com.twitter.util.{Await, Future}
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatestplus.mockito.MockitoSugar

class ParameterlessQueryableTest extends FunSuite with MockitoSugar {
  val index = mock[Queryable[Int, HnswParams, CosineDistance]]
  val params = HnswParams(12)
  val parameterlessQueryable = ParameterlessQueryable(index, params)
  val embeddingVector = Embedding(Array(1.0F))
  val numNeighbors = 10

  test("ParameterlessQueryable.queryWithDistance") {
    val neighbors = List(NeighborWithDistance(1234, CosineDistance(0.5f)))
    when(index.queryWithDistance(embeddingVector, numNeighbors, params))
      .thenReturn(Future.value(neighbors))

    val result = parameterlessQueryable.queryWithDistance(
      embeddingVector,
      numNeighbors
    )
    assert(Await.result(result) === neighbors)
  }
}
