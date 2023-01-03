package com.twitter.ann.hnsw

import com.twitter.ann.common._
import com.twitter.bijection.Bufferable
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.LocalFile
import java.nio.file.Files
import org.junit.runner.RunWith
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.scalactic.TolerantNumerics
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class MapDbBasedIdEmbeddingMapSpec extends FunSuite with MockitoSugar {
  implicit val floatEquality = TolerantNumerics.tolerantFloatEquality(0.01f)
  val entity1 = EntityEmbedding[Int](1, Embedding(Array(1.0f, 2.0f)))
  val entity2 = EntityEmbedding[Int](2, Embedding(Array(3.0f, 4.0f)))

  test(
    "MapDbBasedIdEmbeddingMap correctly deserialize thrift embeddings and provides mapdb based embedding mapping") {
    val embeddings = List((entity1.id, entity1.embedding), (entity2.id, entity2.embedding))
    val tempFile = Files.createTempDirectory("test").toFile
    tempFile.deleteOnExit()
    val temp = new LocalFile(tempFile).getChild("test")
    HnswIOUtil.saveEmbeddings(
      temp.getByteSink.openStream(),
      AnnInjections.IntInjection,
      embeddings.iterator)
    val deserializedMap = MapDbBasedIdEmbeddingMap.loadAsReadonly(temp, AnnInjections.IntInjection)
    assert(deserializedMap.size() == embeddings.size)
    assert(deserializedMap.get(entity1.id) === entity1.embedding)
    assert(deserializedMap.get(entity2.id) === entity2.embedding)
  }

  test("putIfAbsent returns value properly") {
    val mapDb = DBMaker
      .memoryDB().make()
      .hashMap("test_map", Serializer.BYTE_ARRAY, Serializer.FLOAT_ARRAY)
      .create()
    val map = new MapDbBasedIdEmbeddingMap[Int](mapDb, Bufferable.injectionOf[Int])
    val r1 = map.putIfAbsent(1, entity1.embedding)
    assert(r1 == null)
    val r2 = map.putIfAbsent(1, entity2.embedding)
    assert(r2 == entity1.embedding)
    assert(map.get(1) == entity1.embedding)
  }

  test("put returns value properly") {
    val mapDb = DBMaker
      .memoryDB().make()
      .hashMap("test_map", Serializer.BYTE_ARRAY, Serializer.FLOAT_ARRAY)
      .create()
    val map = new MapDbBasedIdEmbeddingMap[Int](mapDb, Bufferable.injectionOf[Int])
    val r1 = map.put(1, entity1.embedding)
    assert(r1 == null)
    val r2 = map.put(1, entity2.embedding)
    assert(r2 == entity1.embedding)
    assert(map.get(1) == entity2.embedding)
  }
}
