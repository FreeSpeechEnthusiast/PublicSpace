package com.twitter.ann.common

import com.twitter.ml.api.embedding.Embedding
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ComposedQueryableSpec extends AnyFunSuite with MockitoSugar {
  class TestParams extends RuntimeParams

  test(
    "query method successfully query all the indices and then merge the result to get top k neighbours without distance") {
    val queryable1 = mock[Queryable[Int, TestParams, L2Distance]]
    val queryable2 = mock[Queryable[Int, TestParams, L2Distance]]
    val indices = Seq(queryable1, queryable2)

    val neighbours = 2
    val params = new TestParams
    val vector = Embedding(Array(1.0f))

    when(queryable1.queryWithDistance(vector, neighbours, params))
      .thenReturn(Future.value(
        List(NeighborWithDistance(1, L2Distance(1.0f)), NeighborWithDistance(2, L2Distance(3.0f)))))

    when(queryable2.queryWithDistance(vector, neighbours, params))
      .thenReturn(Future.value(
        List(NeighborWithDistance(3, L2Distance(2.0f)), NeighborWithDistance(4, L2Distance(4.0f)))))

    val composedQueryable = new ComposedQueryable[Int, TestParams, L2Distance](indices)
    val nn = Await.result(composedQueryable.query(vector, 2, params))
    assert(nn.size == 2)

    assert(nn(0) == 1)
    assert(nn(1) == 3)
  }

  test(
    "queryWithDistance method successfully query all the indices and then merge the result to get top k neighbours with distance") {
    val queryable1 = mock[Queryable[Int, TestParams, L2Distance]]
    val queryable2 = mock[Queryable[Int, TestParams, L2Distance]]
    val indices = Seq(queryable1, queryable2)

    val neighbours = 2
    val params = new TestParams
    val vector = Embedding(Array(1.0f))

    when(queryable1.queryWithDistance(vector, neighbours, params))
      .thenReturn(Future.value(
        List(NeighborWithDistance(1, L2Distance(1.0f)), NeighborWithDistance(2, L2Distance(3.0f)))))

    when(queryable2.queryWithDistance(vector, neighbours, params))
      .thenReturn(Future.value(
        List(NeighborWithDistance(3, L2Distance(2.0f)), NeighborWithDistance(4, L2Distance(4.0f)))))

    val composedQueryable = new ComposedQueryable[Int, TestParams, L2Distance](indices)
    val nn = Await.result(composedQueryable.queryWithDistance(vector, 2, params))
    assert(nn.size == 2)

    assert(nn(0).neighbor == 1)
    assert(nn(0).distance == L2Distance(1.0f))

    assert(nn(1).neighbor == 3)
    assert(nn(1).distance == L2Distance(2.0f))
  }

  test("query method fails when querying any of the index fails") {
    val queryable1 = mock[Queryable[Int, TestParams, L2Distance]]
    val queryable2 = mock[Queryable[Int, TestParams, L2Distance]]
    val indices = Seq(queryable1, queryable2)

    val neighbours = 2
    val params = new TestParams
    val vector = Embedding(Array(1.0f))

    when(queryable1.queryWithDistance(vector, neighbours, params))
      .thenReturn(Future.value(
        List(NeighborWithDistance(1, L2Distance(1.0f)), NeighborWithDistance(2, L2Distance(3.0f)))))

    when(queryable2.queryWithDistance(vector, neighbours, params))
      .thenReturn(Future.exception(new RuntimeException("")))

    val composedQueryable = new ComposedQueryable[Int, TestParams, L2Distance](indices)
    intercept[RuntimeException] {
      Await.result(composedQueryable.query(vector, 2, params))
    }
  }
}
