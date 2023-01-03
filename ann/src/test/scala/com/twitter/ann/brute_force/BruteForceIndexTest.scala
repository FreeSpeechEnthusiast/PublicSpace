package com.twitter.ann.brute_force

import com.twitter.ann.common.EntityEmbedding
import com.twitter.ann.common.L2
import com.twitter.ann.common.L2Distance
import com.twitter.ann.common.NeighborWithDistance
import com.twitter.ml.api.embedding.Embedding
import com.twitter.util.Await
import com.twitter.util.FuturePool
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.mockito.MockitoSugar

class BruteForceIndexTest extends AnyFunSuite with MockitoSugar {

  // for simplicity use l2 distance with 1 dimension. The element of the vector and the id are the
  // same.
  def testEmbedding(idAndElement: Float): EntityEmbedding[Float] = {
    EntityEmbedding[Float](idAndElement, Embedding(Array(idAndElement)))
  }

  val one = testEmbedding(1.0f)
  val two = testEmbedding(2.0f)
  val three = testEmbedding(3.0f)
  val four = testEmbedding(4.0f)

  test("query for one element") {
    val index = BruteForceIndex[Float, L2Distance](L2, FuturePool.immediatePool)
    index.append(one)
    val future = index.query(four.embedding, 1, BruteForceRuntimeParams)
    val result = Await.result(future)
    assert(result == List(1.0f))
  }

  test("queryWithDistance for one element") {
    val index = BruteForceIndex[Float, L2Distance](L2, FuturePool.immediatePool)
    index.append(one)
    val future = index.queryWithDistance(four.embedding, 1, BruteForceRuntimeParams)
    val result = Await.result(future)
    assert(result == List(NeighborWithDistance(1.0f, L2Distance(3.0f))))
  }

  test("queryWithDistance for one element among 4") {
    val index = BruteForceIndex[Float, L2Distance](L2, FuturePool.immediatePool)
    index.append(one)
    index.append(two)
    index.append(three)
    index.append(four)
    val future = index.queryWithDistance(four.embedding, 1, BruteForceRuntimeParams)
    val result = Await.result(future)
    assert(result == List(NeighborWithDistance(4.0f, L2Distance(0.0f))))
  }

  test("queryWithDistance for 2 elements among 4") {
    val index = BruteForceIndex[Float, L2Distance](L2, FuturePool.immediatePool)
    index.append(one)
    index.append(two)
    index.append(three)
    index.append(four)
    val future = index.queryWithDistance(four.embedding, 2, BruteForceRuntimeParams)
    val result = Await.result(future)
    assert(
      result == List(
        NeighborWithDistance(4.0f, L2Distance(0.0f)),
        NeighborWithDistance(3.0f, L2Distance(1.0f))
      ))
  }

  test("initializing index with constructor") {
    val index = BruteForceIndex[Float, L2Distance](
      L2,
      FuturePool.immediatePool,
      Iterator(one, two, three, four)
    )
    val future = index.queryWithDistance(four.embedding, 2, BruteForceRuntimeParams)
    val result = Await.result(future)
    assert(
      result == List(
        NeighborWithDistance(4.0f, L2Distance(0.0f)),
        NeighborWithDistance(3.0f, L2Distance(1.0f))
      ))
  }
}
