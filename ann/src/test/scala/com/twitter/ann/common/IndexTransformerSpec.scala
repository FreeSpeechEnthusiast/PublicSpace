package com.twitter.ann.common

import com.twitter.ml.api.embedding.Embedding
import com.twitter.storehaus.Store
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class IndexTransformerSpec extends AnyFunSuite with MockitoSugar {
  class TestParams extends RuntimeParams
  trait RawAppendWithQuery
      extends RawAppendable[TestParams, L2Distance]
      with Queryable[Long, TestParams, L2Distance]

  val embedding = Embedding(Array(1.0f))
  val params = new TestParams

  test(
    "transformQueryable transforms a Raw Queryable(Long) to Typed Queryable using ReadableStore") {
    val queryable = mock[Queryable[Long, TestParams, L2Distance]]
    val readableStore = mock[Store[Long, String]]

    val indexIdMapping = Map(1L -> "key1", 2L -> "key2", 3L -> "key3")
    val neighbourDistance =
      Map(1L -> L2Distance(1.0f), 2L -> L2Distance(2.0f), 3L -> L2Distance(3.0f))
    val typedNeighbourDistance =
      neighbourDistance.map(nd => NeighborWithDistance(indexIdMapping(nd._1), nd._2))
    val neighbours = 3

    when(queryable.query(embedding, neighbours, params))
      .thenReturn(Future.value(indexIdMapping.keys.toList))
    when(readableStore.get(any[Long])).thenAnswer(new Answer[Future[Option[String]]] {
      override def answer(invocationOnMock: InvocationOnMock): Future[Option[String]] = {
        val longId = invocationOnMock.getArgument(0, classOf[java.lang.Long])
        Future.value(indexIdMapping.get(longId))
      }
    })

    val typedQueryable = IndexTransformer.transformQueryable(queryable, readableStore)

    // Test Query without distance
    val result1 = Await.result(typedQueryable.query(embedding, neighbours, params))
    assert(result1.size == neighbours)
    assert(result1 == indexIdMapping.values.toSeq)

    verify(queryable, times(1)).query(embedding, neighbours, params)
    verify(readableStore, times(neighbours)).get(any[Long])

    // Test Query with distance
    val nnWithDistance = neighbourDistance.map(nn => NeighborWithDistance(nn._1, nn._2)).toList
    when(queryable.queryWithDistance(embedding, neighbours, params))
      .thenReturn(Future.value(nnWithDistance))

    val result2 = Await.result(typedQueryable.queryWithDistance(embedding, neighbours, params))
    assert(result2.size == neighbours)
    assert(result2 == typedNeighbourDistance)

    verify(queryable, times(1)).queryWithDistance(embedding, neighbours, params)
    verify(readableStore, times(2 * neighbours)).get(any[Long])

    // Test Query with distance for 0 neighbours
    when(queryable.queryWithDistance(embedding, neighbours, params))
      .thenReturn(Future.value(List.empty))

    val result3 = Await.result(typedQueryable.queryWithDistance(embedding, neighbours, params))
    assert(result3.isEmpty)
  }

  test("transformAppendable transforms a RawAppendable to Typed Appendable using WritableStore") {
    val rawAppendable = mock[RawAppendable[TestParams, L2Distance]]
    val writableStore = mock[Store[Long, String]]

    val keyVal = 1L -> Some("key")
    val entity = EntityEmbedding[String](keyVal._2.get, embedding)

    when(rawAppendable.append(embedding)).thenReturn(Future.value(1L))
    when(writableStore.put(keyVal)).thenReturn(Future.Unit)

    val typedAppendable = IndexTransformer.transformAppendable[String, TestParams, L2Distance](
      rawAppendable,
      writableStore)

    Await.result(typedAppendable.append(entity))

    verify(rawAppendable, times(1)).append(embedding)
    verify(writableStore, times(1)).put(keyVal)
  }

  test("transformAppendable transforms a RawAppendable to queryable using WritableStore") {
    val rawAppendable = mock[RawAppendable[TestParams, L2Distance]]
    val rawQueryable = mock[Queryable[Long, TestParams, L2Distance]]
    val writableStore = mock[Store[Long, String]]
    val keyVal = 1L -> Some("key")
    val entity = EntityEmbedding[String](keyVal._2.get, embedding)
    when(rawAppendable.append(embedding)).thenReturn(Future.value(1L))
    when(rawAppendable.toQueryable).thenReturn(rawQueryable)
    when(rawQueryable.queryWithDistance(embedding, 1, params))
      .thenReturn(Future.value(List(NeighborWithDistance(keyVal._1, L2Distance(0.0f)))))
    when(writableStore.put(keyVal)).thenReturn(Future.Unit)
    when(writableStore.get(1L)).thenReturn(Future.value(Some("key")))
    val typedAppendable = IndexTransformer.transformAppendable[String, TestParams, L2Distance](
      rawAppendable,
      writableStore)
    Await.result(typedAppendable.append(entity))
    val queryable = typedAppendable.toQueryable
    val res = Await.result(queryable.queryWithDistance(embedding, 1, params)).head
    assert(res.neighbor == keyVal._2.get)
    assert(res.distance == L2Distance(0.0f))
  }

  test(
    "transform1 transforms a combination of RawAppendable with RawQueryable(Long) to Typed Appendable with Queryable using store"
  ) {
    val raw = mock[RawAppendWithQuery]
    val store = mock[Store[Long, String]]

    val keyVal = 1L -> Some("key")
    val entity = EntityEmbedding[String](keyVal._2.get, embedding)

    when(store.get(keyVal._1)).thenReturn(Future.value(keyVal._2))
    when(store.put(keyVal)).thenReturn(Future.Unit)
    when(raw.query(embedding, 1, params)).thenReturn(Future.value(List(keyVal._1)))
    when(raw.queryWithDistance(embedding, 1, params))
      .thenReturn(Future.value(List(NeighborWithDistance(keyVal._1, L2Distance(0.0f)))))
    when(raw.append(embedding)).thenReturn(Future.value(keyVal._1))

    val typed =
      IndexTransformer.transform1[RawAppendWithQuery, String, TestParams, L2Distance](raw, store)
    Await.result(typed.append(entity))

    val nn = Await.result(typed.query(embedding, 1, params)).head
    assert(nn == keyVal._2.get)

    val nnWithdistance = Await.result(typed.queryWithDistance(embedding, 1, params)).head
    assert(nnWithdistance.neighbor == keyVal._2.get)
    assert(nnWithdistance.distance == L2Distance(0.0f))

    verify(store, times(1)).put(keyVal)
    verify(store, times(2)).get(keyVal._1)
  }
}
