package com.twitter.ann.hnsw

import com.twitter.ann.common._
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.LocalFile
import com.twitter.util.{Await, Future, FuturePool}
import java.nio.file.Files
import org.junit.runner.RunWith
import org.scalactic.TolerantNumerics
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class TypedHnswIndexSpec extends FunSuite with MockitoSugar {
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
  val futurePool = ReadWriteFuturePool(FuturePool.immediatePool)

  test("TypedHnswIndex verify serializableIndex end to end") {
    val index = indexEntities()
    val tempFile = Files.createTempDirectory("test").toFile
    tempFile.deleteOnExit()
    val temp = new LocalFile(tempFile)
    index.toDirectory(temp)

    assert(temp.exists())
    assert(temp.getChild(HnswCommon.MetaDataFileName).exists())
    assert(temp.getChild(HnswCommon.EmbeddingMappingFileName).exists())
    assert(temp.getChild(HnswCommon.InternalIndexDir).exists())

    val result = Await.result(index.queryWithDistance(query, neighbours, runtimeParam))
    assert(expectedIds == result.map(_.neighbor))
    assert(expectedDistances === result.map(_.distance))
  }

  test("TypedHnswIndex verify index end to end") {
    val index = TypedHnswIndex.index[Int, InnerProductDistance](
      dimen,
      metric,
      efConstruction,
      maxM,
      expectedElements,
      futurePool
    )
    Await.result(Future.collect(entities.map(index.append(_))))

    val result = Await.result(index.queryWithDistance(query, neighbours, runtimeParam))
    assert(expectedIds == result.map(_.neighbor))
    assert(expectedDistances === result.map(_.distance))
  }

  test("TypedHnswIndex verify loadIndex end to end") {
    val index = indexEntities()
    val tempFile = Files.createTempDirectory("test").toFile
    tempFile.deleteOnExit()
    val temp = new LocalFile(tempFile)
    index.toDirectory(temp)

    val loadedIndex = TypedHnswIndex.loadIndex(
      dimen,
      metric,
      injection,
      futurePool,
      temp
    )

    val result = Await.result(loadedIndex.queryWithDistance(query, neighbours, runtimeParam))
    assert(expectedIds == result.map(_.neighbor))
    assert(expectedDistances === result.map(_.distance))
  }

  test("TypedHnswIndex verify loadBatchMMappedQueryableIndex end to end") {
    val index = indexEntities()
    val tempFile = Files.createTempDirectory("test").toFile
    tempFile.deleteOnExit()
    val temp = new LocalFile(tempFile)
    index.toDirectory(temp)

    val loadedIndex = TypedHnswIndex.loadMMappedIndex(
      dimen,
      metric,
      injection,
      futurePool,
      temp
    )

    val result = Await.result(loadedIndex.queryWithDistance(query, neighbours, runtimeParam))
    assert(expectedIds == result.map(_.neighbor))
    assert(expectedDistances === result.map(_.distance))
  }

  private[this] def indexEntities() = {
    val index = TypedHnswIndex.serializableIndex(
      dimen,
      metric,
      efConstruction,
      maxM,
      expectedElements,
      injection,
      futurePool
    )
    Await.result(Future.collect(entities.map(index.append(_))))
    index
  }
}
