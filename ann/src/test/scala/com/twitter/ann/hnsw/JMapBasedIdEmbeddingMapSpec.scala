package com.twitter.ann.hnsw

import com.twitter.ann.common._
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.LocalFile
import java.nio.file.Files
import org.junit.runner.RunWith
import org.scalactic.TolerantNumerics
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class JMapBasedIdEmbeddingMapSpec extends AnyFunSuite with MockitoSugar {
  implicit val floatEquality = TolerantNumerics.tolerantFloatEquality(0.01f)
  val entity1 = EntityEmbedding[Int](1, Embedding(Array(1.0f, 2.0f)))
  val entity2 = EntityEmbedding[Int](2, Embedding(Array(3.0f, 4.0f)))

  test("JMapBasedIdEmbeddingMap in memory get/putIfAbsent") {
    val map = JMapBasedIdEmbeddingMap.applyInMemory[Int](2)
    map.putIfAbsent(entity1.id, entity1.embedding)
    map.putIfAbsent(entity2.id, entity2.embedding)
    assert(map.size() == 2)
    assert(map.get(entity1.id) === entity1.embedding)
    assert(map.get(entity2.id) === entity2.embedding)
  }

  test("JMapBasedIdEmbeddingMap correctly serialize and deserialize embeddings data") {
    val map =
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization[Int](2, AnnInjections.IntInjection)
    map.putIfAbsent(entity1.id, entity1.embedding)
    map.putIfAbsent(entity2.id, entity2.embedding)
    val tempFile = Files.createTempDirectory("test").toFile
    tempFile.deleteOnExit()
    val temp = new LocalFile(tempFile).getChild("test")
    map.toDirectory(temp.getByteSink.openStream())
    val deserializedMap = JMapBasedIdEmbeddingMap.loadInMemory(temp, AnnInjections.IntInjection)
    assert(map.size() == deserializedMap.size())
    assert(map.get(entity1.id) === deserializedMap.get(entity1.id))
    assert(map.get(entity2.id) === deserializedMap.get(entity2.id))
  }

  test("putIfAbsent returns value properly") {
    val map =
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization[Int](2, AnnInjections.IntInjection)
    val r1 = map.putIfAbsent(entity1.id, entity1.embedding)
    assert(r1 == null)
    val r2 = map.putIfAbsent(entity1.id, entity2.embedding)
    assert(r2 == entity1.embedding)
    assert(map.get(1) == entity1.embedding)
  }

  test("put returns value properly") {
    val map =
      JMapBasedIdEmbeddingMap.applyInMemoryWithSerialization[Int](2, AnnInjections.IntInjection)
    val r1 = map.put(entity1.id, entity1.embedding)
    assert(r1 == null)
    val r2 = map.put(entity1.id, entity2.embedding)
    assert(r2 == entity1.embedding)
    assert(map.get(1) == entity2.embedding)
  }
}
