package com.twitter.ann.hnsw

import com.twitter.ann.common.EmbeddingType.EmbeddingVector
import com.twitter.ann.common._
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.LocalFile
import java.io.OutputStream
import java.nio.file.Files
import org.junit.runner.RunWith
import org.scalactic.TolerantNumerics
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class HnswIOUtilSpec extends FunSuite with MockitoSugar {
  implicit val floatEquality = TolerantNumerics.tolerantFloatEquality(0.01f)
  test("HnswIOUtil properly serialize/deserialize embeddings") {
    val entity1 = EntityEmbedding[Int](1, Embedding(Array(1.0f, 2.0f)))
    val entity2 = EntityEmbedding[Int](2, Embedding(Array(3.0f, 4.0f)))
    val embeddings =
      List((entity1.id, entity1.embedding), (entity2.id, entity2.embedding))
    val tempFile = Files.createTempDirectory("test").toFile
    tempFile.deleteOnExit()
    val temp = new LocalFile(tempFile).getChild("test")
    HnswIOUtil.saveEmbeddings(
      temp.getByteSink.openStream(),
      AnnInjections.IntInjection,
      embeddings.iterator
    )

    assert(temp.exists())

    val deserialized = mutable.ListBuffer.empty[(Int, EmbeddingVector)]
    val idEmbeddingMap = new IdEmbeddingMap[Int] {
      // Not thread safe
      override def putIfAbsent(id: Int, entity: EmbeddingVector): EmbeddingVector = {
        deserialized.append((id, entity))
        entity
      }
      override def put(id: Int, entity: EmbeddingVector): EmbeddingVector = {
        ???
      }
      override def get(id: Int): EmbeddingVector = ???
      override def iter(): Iterator[(Int, EmbeddingVector)] = deserialized.iterator
      override def size(): Int = deserialized.size
      override def toDirectory(embeddingFile: OutputStream): Unit = ???
    }

    val mapping = HnswIOUtil.loadEmbeddings(
      temp,
      AnnInjections.IntInjection,
      idEmbeddingMap
    )

    assert(mapping.size() == 2)
    assert(mapping.iter().toList === embeddings)
  }

  test("HnswIOUtil properly serialize/deserialize index metadata") {
    val metric = L2
    val dimension = 10
    val numElements = 1

    val tempDir = Files.createTempDirectory("test").toFile
    tempDir.deleteOnExit()
    val temp = new LocalFile(tempDir)
    val metadataFile = temp.getChild("test")
    HnswIOUtil.saveIndexMetadata(
      dimension,
      metric,
      numElements,
      metadataFile.getByteSink.openStream())

    assert(metadataFile.exists())

    val deserialized = HnswIOUtil.loadIndexMetadata(metadataFile)

    assert(deserialized.distanceMetric == Metric.toThrift(metric))
    assert(deserialized.dimension == dimension)
    assert(deserialized.numElements == numElements)
  }
}
