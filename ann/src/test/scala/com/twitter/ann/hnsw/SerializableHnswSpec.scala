package com.twitter.ann.hnsw

import com.twitter.ann.common.EmbeddingType.EmbeddingVector
import com.twitter.ann.common._
import com.twitter.bijection.Injection
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.AbstractFile
import com.twitter.search.common.file.LocalFile
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.FuturePool
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import org.apache.beam.sdk.io.LocalResources
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.scalactic.TolerantNumerics
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import scala.util.Random
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class SerializableHnswSpec extends AnyFunSuite with MockitoSugar {
  implicit val floatEquality = TolerantNumerics.tolerantFloatEquality(0.01f)
  val entity1 = EntityEmbedding[Int](1, Embedding(Array(1.0f)))
  val entity2 = EntityEmbedding[Int](2, Embedding(Array(2.0f)))
  val entity3 = EntityEmbedding[Int](3, Embedding(Array(3.0f)))
  val entity4 = EntityEmbedding[Int](4, Embedding(Array(4.0f)))
  val entities = List(entity1, entity2, entity3, entity4)

  val metric = InnerProduct
  val query = Embedding(Array(5.0f))
  val expectedIds = List(4, 3, 2, 1)
  val expectedDistances = entities.map(entity => metric.distance(query, entity.embedding)).sorted

  val dimen = 1
  val efConstruction = 50
  val maxM = 10

  val injection = AnnInjections.IntInjection
  val expectedElements = entities.size
  val neighbours = entities.size
  val runtimeParam = HnswParams(entities.size)

  test("SerializableHnsw verify append/query/toQueryable") {
    val hnsw = mock[Hnsw[Int, InnerProductDistance]]
    val serializableHnsw = SerializableHnsw(hnsw, injection)
    val params = HnswParams(10)
    val neighbours = 2

    when(hnsw.query(entity1.embedding, neighbours, params)).thenReturn(Future.apply(List.empty))
    when(hnsw.queryWithDistance(entity1.embedding, neighbours, params))
      .thenReturn(Future.apply(List.empty))
    when(hnsw.toQueryable).thenReturn(hnsw)

    val nn = Await.result(serializableHnsw.query(entity1.embedding, neighbours, params))
    assert(nn.isEmpty)
    verify(hnsw, times(1)).query(entity1.embedding, neighbours, params)

    val nnWithDistance =
      Await.result(serializableHnsw.queryWithDistance(entity1.embedding, neighbours, params))
    assert(nnWithDistance.isEmpty)
    verify(hnsw, times(1))
      .queryWithDistance(entity1.embedding, neighbours, params)

    val queryable = serializableHnsw.toQueryable
    assert(queryable == hnsw)
    verify(hnsw, times(1)).toQueryable
  }

  test("SerializableHnsw verify index serialization toDirectory") {
    val javaHnswIndex = mock[HnswIndex[Int, EmbeddingVector]]
    val idEmbeddingMap = mock[IdEmbeddingMap[Int]]
    val hnsw = mock[Hnsw[Int, InnerProductDistance]]
    val serializableHnsw = SerializableHnsw(hnsw, injection)

    when(hnsw.getIndex).thenReturn(javaHnswIndex)
    when(hnsw.getIdEmbeddingMap).thenReturn(idEmbeddingMap)
    when(hnsw.getDimen).thenReturn(dimen)
    when(hnsw.getMetric).thenReturn(metric)
    when(idEmbeddingMap.size()).thenReturn(1)

    val tempFile = Files.createTempDirectory("test").toFile
    tempFile.deleteOnExit()
    val temp = new LocalFile(tempFile)

    val expectedContent = "embedding mapping"
    when(idEmbeddingMap.toDirectory(any[OutputStream]))
      .thenAnswer(new Answer[Unit] {
        override def answer(invocationOnMock: InvocationOnMock): Unit = {
          val outputStream = invocationOnMock.getArgument(0, classOf[OutputStream])
          outputStream.write(expectedContent.getBytes())
        }
      })

    when(javaHnswIndex.toDirectory(any[IndexOutputFile], any[Injection[Int, Array[Byte]]]))
      .thenAnswer(new Answer[Unit] {
        override def answer(invocationOnMock: InvocationOnMock): Unit = {
          val file =
            invocationOnMock
              .getArgument(0, classOf[IndexOutputFile]).abstractFile.asInstanceOf[LocalFile]
          assert(file.getName == HnswCommon.InternalIndexDir)
        }
      })

    serializableHnsw.toDirectory(temp)

    verify(hnsw, times(1)).getDimen
    verify(hnsw, times(1)).getMetric
    verify(hnsw, times(1)).getIndex

    verify(idEmbeddingMap, times(1)).size()
    verify(idEmbeddingMap, times(1)).toDirectory(any[OutputStream])
    verify(javaHnswIndex, times(1))
      .toDirectory(any[IndexOutputFile], any[Injection[Int, Array[Byte]]])
    assert(temp.getChild(HnswCommon.MetaDataFileName).exists())
    assert(temp.getChild(HnswCommon.EmbeddingMappingFileName).exists())
    val actualContent = temp.getChild(HnswCommon.EmbeddingMappingFileName).getByteSource.read()
    assert(actualContent === expectedContent.getBytes())
  }

  test("SerializableHnsw verify index serialization toDirectory with ResourceId") {
    val javaHnswIndex = mock[HnswIndex[Int, EmbeddingVector]]
    val idEmbeddingMap = mock[IdEmbeddingMap[Int]]
    val hnsw = mock[Hnsw[Int, InnerProductDistance]]
    val serializableHnsw = SerializableHnsw(hnsw, injection)

    when(hnsw.getIndex).thenReturn(javaHnswIndex)
    when(hnsw.getIdEmbeddingMap).thenReturn(idEmbeddingMap)
    when(hnsw.getDimen).thenReturn(dimen)
    when(hnsw.getMetric).thenReturn(metric)
    when(idEmbeddingMap.size()).thenReturn(1)

    val tempFile = Files.createTempDirectory("test").toFile
    tempFile.deleteOnExit()
    val temp = new LocalFile(tempFile)
    val tmpResource = LocalResources.fromFile(tempFile, /* isDirectory */ true)

    val expectedContent = "embedding mapping"
    when(idEmbeddingMap.toDirectory(any[OutputStream]))
      .thenAnswer(new Answer[Unit] {
        override def answer(invocationOnMock: InvocationOnMock): Unit = {
          val outputStream = invocationOnMock.getArgument(0, classOf[OutputStream])
          outputStream.write(expectedContent.getBytes())
          outputStream.close()
        }
      })

    when(javaHnswIndex.toDirectory(any[IndexOutputFile], any[Injection[Int, Array[Byte]]]))
      .thenAnswer(new Answer[Unit] {
        override def answer(invocationOnMock: InvocationOnMock): Unit = {
          val file =
            invocationOnMock.getArgument(0, classOf[IndexOutputFile]).resourceId
          assert(file.getFilename == HnswCommon.InternalIndexDir)
        }
      })

    serializableHnsw.toDirectory(tmpResource)

    verify(hnsw, times(1)).getDimen
    verify(hnsw, times(1)).getMetric
    verify(hnsw, times(1)).getIndex

    verify(idEmbeddingMap, times(1)).size()
    verify(idEmbeddingMap, times(1)).toDirectory(any[OutputStream])
    verify(javaHnswIndex, times(1))
      .toDirectory(any[IndexOutputFile], any[Injection[Int, Array[Byte]]])
    assert(temp.getChild(HnswCommon.MetaDataFileName).exists())
    assert(temp.getChild(HnswCommon.EmbeddingMappingFileName).exists())
    val actualContent = temp.getChild(HnswCommon.EmbeddingMappingFileName).getByteSource.read()
    assert(actualContent === expectedContent.getBytes())
  }

  test("SerializableHnsw verify end to end serialization") {
    val (index, dir) = createDummyIndex(
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization[Int](expectedElements, injection)
    )

    assert(dir.exists())
    assert(dir.getChild(HnswCommon.MetaDataFileName).exists())
    assert(dir.getChild(HnswCommon.EmbeddingMappingFileName).exists())
    assert(dir.getChild(HnswCommon.InternalIndexDir).exists())
    assert(dir.getChild("_SUCCESS").exists())

    val queryable = index.toQueryable

    val result = Await.result(queryable.queryWithDistance(query, neighbours, runtimeParam))
    assert(expectedIds == result.map(_.neighbor))
    assert(expectedDistances === result.map(_.distance))
  }

  test("SerializableHnsw verify end to end loading serialized index as in memory map") {
    val dir = createDummyIndex(
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization[Int](expectedElements, injection)
    )._2
    val queryable = SerializableHnsw.loadMapBasedQueryableIndex(
      dimen,
      metric,
      injection,
      ReadWriteFuturePool(FuturePool.immediatePool),
      dir
    )

    val result = Await.result(queryable.queryWithDistance(query, neighbours, runtimeParam))
    assert(expectedIds == result.map(_.neighbor))
    assert(expectedDistances === result.map(_.distance))
  }

  test("SerializableHnsw verify end to end loading serialized index as map db") {
    val dir = createDummyIndex(
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization[Int](expectedElements, injection)
    )._2
    val queryable = SerializableHnsw.loadMMappedBasedQueryableIndex(
      dimen,
      metric,
      injection,
      ReadWriteFuturePool(FuturePool.immediatePool),
      dir
    )

    val result = Await.result(queryable.queryWithDistance(query, neighbours, runtimeParam))
    assert(expectedIds == result.map(_.neighbor))
    assert(expectedDistances === result.map(_.distance))
  }

  test(
    "SerializableHnsw verify assert exception when loading in memory index with dimension mismatch"
  ) {
    val dir = createDummyIndex(
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization[Int](expectedElements, injection)
    )._2
    intercept[AssertionError] {
      SerializableHnsw.loadMapBasedQueryableIndex(
        dimen + 1,
        metric,
        injection,
        ReadWriteFuturePool(FuturePool.immediatePool),
        dir
      )
    }
  }

  test(
    "SerializableHnsw verify assert exception when loading in memory index with metric mismatch") {
    val dir = createDummyIndex(
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization[Int](expectedElements, injection)
    )._2
    intercept[AssertionError] {
      SerializableHnsw.loadMapBasedQueryableIndex(
        dimen,
        Cosine,
        injection,
        ReadWriteFuturePool(FuturePool.immediatePool),
        dir
      )
    }
  }

  test(
    "SerializableHnsw verify assert exception when loading mapdb index with dimension mismatch") {
    val dir = createDummyIndex(
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization[Int](expectedElements, injection)
    )._2
    intercept[AssertionError] {
      SerializableHnsw.loadMMappedBasedQueryableIndex(
        dimen + 1,
        metric,
        injection,
        ReadWriteFuturePool(FuturePool.immediatePool),
        dir
      )
    }
  }

  test("SerializableHnsw verify assert exception when loading mapdb index with metric mismatch") {
    val dir = createDummyIndex(
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization(expectedElements, injection)
    )._2
    intercept[AssertionError] {
      SerializableHnsw.loadMMappedBasedQueryableIndex(
        dimen,
        Cosine,
        injection,
        ReadWriteFuturePool(FuturePool.immediatePool),
        dir
      )
    }
  }

  test("toDirectory without mkdir first") {
    val index = Hnsw[Int, InnerProductDistance](
      dimen,
      metric,
      efConstruction,
      maxM,
      expectedElements,
      ReadWriteFuturePool(FuturePool.immediatePool),
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization[Int](expectedElements, injection)
    )
    val serializableHnsw = SerializableHnsw[Int, InnerProductDistance](
      index,
      injection
    )
    Await.result(Future.collect(entities.map(serializableHnsw.append(_))))
    // Make sure we don't need target dir to exist before hand
    val f =
      new File(System.getProperty("java.io.tmpdir") + "/test_tmp_file" + new Random().nextInt(1000))
    val temp = new LocalFile(f)
    f.deleteOnExit()
    assert(!temp.exists())
    serializableHnsw.toDirectory(temp)
    assert(temp.exists())
  }

  private[this] def createDummyIndex(
    idEmbeddingMap: IdEmbeddingMap[Int]
  ): (SerializableHnsw[Int, InnerProductDistance], AbstractFile) = {
    val index = Hnsw[Int, InnerProductDistance](
      dimen,
      metric,
      efConstruction,
      maxM,
      expectedElements,
      ReadWriteFuturePool(FuturePool.immediatePool),
      idEmbeddingMap
    )

    val serializableHnsw = SerializableHnsw[Int, InnerProductDistance](
      index,
      injection
    )

    Await.result(Future.collect(entities.map(serializableHnsw.append(_))))

    val tempFile = Files.createTempDirectory("test").toFile
    tempFile.deleteOnExit()
    val temp = new LocalFile(tempFile)
    serializableHnsw.toDirectory(temp)

    (serializableHnsw, temp)
  }
}
