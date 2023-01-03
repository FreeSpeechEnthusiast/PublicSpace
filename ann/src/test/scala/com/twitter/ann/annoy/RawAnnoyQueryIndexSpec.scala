package com.twitter.ann.annoy

import com.twitter.ann.common.{Distance, L2, L2Distance, Metric}
import com.twitter.ann.common.EmbeddingType.EmbeddingVector
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.{AbstractFile, LocalFile}
import com.twitter.util.{Await, FuturePool}
import java.nio.file.Files
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalactic.TolerantNumerics
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class RawAnnoyQueryIndexSpec extends FunSuite with MockitoSugar {
  implicit val floatEquality = TolerantNumerics.tolerantFloatEquality(0.01f)

  private[this] def buildDummyIndex[D <: Distance[D]](
    directory: AbstractFile,
    metric: Metric[D],
    embeddings: Seq[EmbeddingVector]
  ): Unit = {
    val dimension = embeddings(0).length
    val numOfTrees = 1

    val indexBuilder =
      RawAnnoyIndexBuilder[D](dimension, numOfTrees, metric, FuturePool.immediatePool)
    embeddings.foreach(indexBuilder.append(_))

    indexBuilder.toDirectory(directory)
  }

  test("RawAnnoyQueryIndex successfully queries vector without distance") {
    val metric = L2
    val emb1 = Embedding(Array(5.0f, 6.0f)) // Annoy long Id 1
    val emb2 = Embedding(Array(1.0f, 2.0f)) // Annoy long Id 2
    val emb3 = Embedding(Array(1.0f, 4.0f)) // Annoy long Id 3
    val embVector = Embedding(Array(1.0f, 2.0f))
    val embs = Seq(emb1, emb2, emb3)

    val temp = new LocalFile(Files.createTempDirectory("test").toFile)
    buildDummyIndex(temp, metric, embs)

    val queryIndex = RawAnnoyQueryIndex[L2Distance](2, metric, FuturePool.immediatePool, temp)

    val result = Await.result(queryIndex.query(embVector, 2, new AnnoyRuntimeParams(Some(100))))

    assert(result.size == 2)
    assert(result(0) == 2)
    assert(result(1) == 3)

    temp.deleteDirectory()
  }

  test("RawAnnoyQueryIndexDeserializer successfully queries vector with distance") {
    val metric = L2
    val emb1 = Embedding(Array(5.0f, 6.0f)) // Annoy long Id 1
    val emb2 = Embedding(Array(1.0f, 2.0f)) // Annoy long Id 2
    val emb3 = Embedding(Array(1.0f, 4.0f)) // Annoy long Id 3
    val embVector = Embedding(Array(1.0f, 2.0f))
    val embs = Seq(emb1, emb2, emb3)

    val temp = new LocalFile(Files.createTempDirectory("test").toFile)
    buildDummyIndex(temp, metric, embs)

    val queryIndex = RawAnnoyQueryIndex[L2Distance](2, metric, FuturePool.immediatePool, temp)

    val result =
      Await.result(queryIndex.queryWithDistance(embVector, 2, new AnnoyRuntimeParams(Some(100))))

    assert(result.size == 2)
    // Check id
    assert(result(0).neighbor == 2)
    assert(result(1).neighbor == 3)

    // Check distances
    assert((result(0).distance.distance === metric.distance(embVector, emb2).distance))
    assert((result(1).distance.distance === metric.distance(embVector, emb3).distance))
    temp.deleteDirectory()
  }
}
