package com.twitter.ann.hnsw

import com.twitter.ann.common.EmbeddingType.EmbeddingVector
import com.twitter.ann.common._
import com.twitter.ml.api.embedding.Embedding
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.FuturePool
import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalactic.Equality
import org.scalactic.TolerantNumerics
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.mockito.MockitoSugar

class HnswSpec extends AnyFunSuite with BeforeAndAfter with MockitoSugar {

  implicit val floatEquality: Equality[Float] = TolerantNumerics.tolerantFloatEquality(0.01f)

  private[this] val entity1 = EntityEmbedding[Int](1, Embedding(Array(1.0f)))
  private[this] val entity5 = EntityEmbedding[Int](5, Embedding(Array(5.0f)))
  private[this] val entity8 = EntityEmbedding[Int](8, Embedding(Array(8.0f)))
  private[this] val entity11 = EntityEmbedding[Int](11, Embedding(Array(11.0f)))
  private[this] val entities = List(entity1, entity5, entity8, entity11)

  private[this] val TestExpectedItem = 10
  private[this] val TestDim = 1
  private[this] val TestNumNeighbor = 2
  private[this] val TestEf = 3
  private[this] val TestMaxM = 5
  private[this] val TestMetric = L2
  private[this] val TestHnswParam = HnswParams(TestEf)

  private[this] val mockHnswIndex = mock[HnswIndex[Int, EmbeddingVector]]
  private[this] val idEmbeddingMap = mock[IdEmbeddingMap[Int]]

  after {
    reset(mockHnswIndex, idEmbeddingMap)
  }

  private[this] def buildHnsw = Hnsw[Int, L2Distance](
    TestDim,
    TestMetric,
    TestEf,
    TestMaxM,
    TestExpectedItem,
    ReadWriteFuturePool(FuturePool.immediatePool),
    JMapBasedIdEmbeddingMap.applyInMemory[Int](TestExpectedItem)
  )

  test("Hnsw verify append without normalization") {
    val hnsw = new Hnsw[Int, L2Distance](
      TestDim,
      TestMetric,
      mockHnswIndex,
      ReadWriteFuturePool(FuturePool.immediatePool),
      idEmbeddingMap,
      false
    )
    Await.result(hnsw.append(entity1))
    verify(mockHnswIndex, times(1)) insert (entity1.id)
    verify(idEmbeddingMap, times(1)).putIfAbsent(entity1.id, entity1.embedding)
  }

  test("Hnsw verify update if the element to be updated does not exist") {
    when(idEmbeddingMap.put(any[Int](), any[EmbeddingVector]())).thenReturn(null)
    val hnsw = new Hnsw[Int, L2Distance](
      TestDim,
      TestMetric,
      mockHnswIndex,
      ReadWriteFuturePool(FuturePool.immediatePool),
      idEmbeddingMap,
      false
    )
    Await.result(hnsw.update(entity1))
    verify(mockHnswIndex, times(1)) insert (entity1.id)
    verify(mockHnswIndex, never()).reInsert(entity1.id)
    verify(idEmbeddingMap, times(1)).put(entity1.id, entity1.embedding)
  }

  test("Hnsw verify update if the element to be updated exist") {
    when(idEmbeddingMap.put(any[Int](), any[EmbeddingVector]())).thenReturn(entity1.embedding)
    val hnsw = new Hnsw[Int, L2Distance](
      TestDim,
      TestMetric,
      mockHnswIndex,
      ReadWriteFuturePool(FuturePool.immediatePool),
      idEmbeddingMap,
      false
    )
    Await.result(hnsw.update(entity1))
    verify(mockHnswIndex, times(1)) reInsert (entity1.id)
    verify(mockHnswIndex, never()).insert(entity1.id)
    verify(idEmbeddingMap, times(1)).put(entity1.id, entity1.embedding)
  }

  test("verify append/update locked access") {
    val mockLock = mock[Lock]
    val lockedAccess = new LockedAccess[Int] {
      override def lockProvider(item: Int): Lock = mockLock
    }
    val hnsw = new Hnsw[Int, L2Distance](
      TestDim,
      TestMetric,
      mockHnswIndex,
      ReadWriteFuturePool(FuturePool.immediatePool),
      idEmbeddingMap,
      false,
      lockedAccess
    )

    Await.result(hnsw.query(entity1.embedding, TestNumNeighbor, TestHnswParam))
    verify(mockLock, never()).lock()
    Await.result(hnsw.append(entity1))
    verify(mockLock, times(1)).lock()
    Await.result(hnsw.update(entity1))
    verify(mockLock, times(2)).lock()
  }

  test("verify LockedAccess") {
    val mockLock = mock[Lock]
    val lockedAccess = new LockedAccess[Int] {
      override def lockProvider(item: Int) = mockLock
    }

    lockedAccess.lock(1) {}
    verify(mockLock, times(1)).lock()
    verify(mockLock, times(1)).unlock()
  }

  test("verify DefaultLockedAccess") {
    val map = mock[ConcurrentHashMap[Int, Lock]]
    val mockLock = mock[Lock]
    val lockedAccess = new DefaultLockedAccess(map)
    when(map.computeIfAbsent(any[Int](), any[java.util.function.Function[Int, Lock]]()))
      .thenReturn(mockLock)

    lockedAccess.lock(1) {}
    verify(mockLock, times(1)).lock()
    verify(mockLock, times(1)).unlock()
  }

  test("Hnsw verify assertion exception when append with dimension mismatch") {
    val hnsw = new Hnsw[Int, L2Distance](
      TestDim + 1,
      TestMetric,
      mockHnswIndex,
      ReadWriteFuturePool(FuturePool.immediatePool),
      idEmbeddingMap,
      false
    )
    intercept[AssertionError](Await.result(hnsw.append(entity1)))
  }

  test("Hnsw verify query and queryWithDistance without normalization") {
    val hnsw = new Hnsw[Int, L2Distance](
      TestDim,
      TestMetric,
      mockHnswIndex,
      ReadWriteFuturePool(FuturePool.immediatePool),
      idEmbeddingMap,
      false
    )
    when(mockHnswIndex.searchKnn(entity1.embedding, TestNumNeighbor, TestEf))
      .thenReturn(new util.ArrayList[DistancedItem[Int]]())
    val result = Await.result(hnsw.query(entity1.embedding, TestNumNeighbor, TestHnswParam))
    assert(result.isEmpty)
    verify(mockHnswIndex, times(1)).searchKnn(entity1.embedding, TestNumNeighbor, TestEf)
    val resultWithDistance =
      Await.result(hnsw.queryWithDistance(entity1.embedding, TestNumNeighbor, TestHnswParam))
    assert(result.isEmpty)
    verify(mockHnswIndex, times(2)).searchKnn(entity1.embedding, TestNumNeighbor, TestEf)
  }

  test("Hnsw verify append with normalization") {
    val map = JMapBasedIdEmbeddingMap.applyInMemory[Int](TestExpectedItem)
    val hnsw = new Hnsw[Int, L2Distance](
      TestDim,
      TestMetric,
      mockHnswIndex,
      ReadWriteFuturePool(FuturePool.immediatePool),
      map,
      true
    )
    Await.result(hnsw.append(entity1))
    verify(mockHnswIndex, times(1)).insert(entity1.id)
    assert(map.get(entity1.id) == MetricUtil.norm(entity1.embedding))
  }

  test("Hnsw verify query and queryWithDistance with normalization") {
    val hnsw = new Hnsw[Int, L2Distance](
      TestDim,
      TestMetric,
      mockHnswIndex,
      ReadWriteFuturePool(FuturePool.immediatePool),
      idEmbeddingMap,
      true
    )
    val ef = 10
    val params = HnswParams(ef)
    when(mockHnswIndex.searchKnn(any[EmbeddingVector], any[Int], any[Int]))
      .thenAnswer(new Answer[util.ArrayList[DistancedItem[Int]]] {
        override def answer(
          invocationOnMock: InvocationOnMock
        ): util.ArrayList[DistancedItem[Int]] = {
          val embeddingVector = invocationOnMock.getArgument(0, classOf[EmbeddingVector])
          val neighbours = invocationOnMock.getArgument(1, classOf[java.lang.Integer])
          val efQ = invocationOnMock.getArgument(2, classOf[java.lang.Integer])
          val normEmb = MetricUtil.norm(entity1.embedding)
          assert(normEmb === embeddingVector)
          assert(neighbours == 2)
          assert(ef == efQ)
          new util.ArrayList[DistancedItem[Int]]()
        }
      })
    val result = Await.result(hnsw.query(entity1.embedding, 2, params))
    assert(result.isEmpty)
    verify(mockHnswIndex, times(1)).searchKnn(any[EmbeddingVector], any[Int], any[Int])
    val resultWithDistance =
      Await.result(hnsw.queryWithDistance(entity1.embedding, 2, params))
    assert(resultWithDistance.isEmpty)
    verify(mockHnswIndex, times(2)).searchKnn(any[EmbeddingVector], any[Int], any[Int])
  }

  test("Hnsw verify end to end append/query") {
    val index = buildHnsw
    Await.result(Future.collect(entities.map(index.append)))
    val result =
      Await.result(index.queryWithDistance(Embedding(Array(7.0f)), TestNumNeighbor, TestHnswParam))
    assert(List(8, 5) == result.map(_.neighbor))
    assert(List(1.0, 2.0) === result.map(_.distance.distance))
  }

  test("test query with only 1 element") {
    val hnsw = buildHnsw
    Await.result(hnsw.append(entity1))
    val res = Await.result(hnsw.query(Embedding(Array(7f)), TestNumNeighbor, TestHnswParam))
    assert(res.toSet == Set(1))
  }

  test("duplicate insertion of first element should fail") {
    val hnsw = buildHnsw
    Await.result(hnsw.append(entity1))
    intercept[IllegalDuplicateInsertException] {
      Await.result(hnsw.append(entity1))
    }
  }

  test("duplicate insertion of element should fail") {
    val hnsw = buildHnsw
    Await.result(hnsw.append(entity1))
    Await.result(hnsw.append(entity5))
    Await.result(hnsw.append(entity8))
    intercept[IllegalDuplicateInsertException] {
      Await.result(hnsw.append(entity5))
    }
  }

  test("verify readWritePool behaviour for query and append") {
    val readWritePool = new ReadWriteFuturePool {
      override def read[T](f: => T): Future[T] =
        Future.value(List(NeighborWithDistance(1, L2Distance(1.0f)))).asInstanceOf[Future[T]]
      override def write[T](f: => T): Future[T] = Future.Unit.asInstanceOf[Future[T]]
    }
    val hnsw = new Hnsw[Int, L2Distance](
      TestDim,
      TestMetric,
      mockHnswIndex,
      readWritePool,
      idEmbeddingMap,
      false
    )
    assert(hnsw.append(entity1) == Future.Unit)
    assert(Await.result(hnsw.query(Embedding(Array(7f)), TestNumNeighbor, TestHnswParam))(0) == 1)
    assert(
      Await.result(hnsw.queryWithDistance(Embedding(Array(7f)), TestNumNeighbor, TestHnswParam))(
        0) == NeighborWithDistance(1, L2Distance(1.0f)))
  }
}
