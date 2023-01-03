package com.twitter.ann.annoy

import com.twitter.ann.common._
import com.twitter.bijection._
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.{AbstractFile, LocalFile}
import com.twitter.util.{Await, FuturePool}
import java.nio.file.Files
import org.junit.runner.RunWith
import org.scalactic.TolerantNumerics
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class TypedAnnoyQueryIndexWithFileSpec extends FunSuite with MockitoSugar {
  implicit val floatEquality = TolerantNumerics.tolerantFloatEquality(0.01f)

  private[this] def buildDummyIndex[P, D <: Distance[D]](
    directory: AbstractFile,
    metric: Metric[D],
    entities: Seq[EntityEmbedding[P]],
    injection: Injection[P, Array[Byte]]
  ): Unit = {
    val dimension = entities(0).embedding.length
    val numOfTrees = 1

    val indexBuilder =
      TypedAnnoyIndexBuilderWithFile[P, D](
        dimension,
        numOfTrees,
        metric,
        injection,
        FuturePool.immediatePool)
    entities.foreach(indexBuilder.append(_))

    indexBuilder.toDirectory(directory)
  }

  test("TypedAnnoyQueryIndexWithFile successfully queries embeddings without distance") {
    val metric = Cosine
    val entity1 = EntityEmbedding[String]("emb1", Embedding(Array(3.0f, 4.0f)))
    val entity2 = EntityEmbedding[String]("emb2", Embedding(Array(1.0f, 1.0f)))
    val entity3 = EntityEmbedding[String]("emb3", Embedding(Array(1.0f, 0.0f)))
    val embVector = Embedding(Array(3.0f, 4.0f))
    val entities = Seq(entity1, entity2, entity3)

    val temp = new LocalFile(Files.createTempDirectory("test").toFile)
    buildDummyIndex(temp, metric, entities, Bufferable.injectionOf[String])

    val queryIndex = TypedAnnoyQueryIndexWithFile[String, CosineDistance](
      2,
      metric,
      Bufferable.injectionOf[String],
      FuturePool.immediatePool,
      temp
    )

    val result = Await.result(queryIndex.query(embVector, 2, new AnnoyRuntimeParams(Some(100))))

    assert(result.size == 2)
    assert(result(0) == "emb1")
    assert(result(1) == "emb2")

    temp.deleteDirectory()
  }

  test("TypedAnnoyQueryIndexWithFileSpec successfully queries embeddings with distance") {
    val metric = Cosine
    val entity1 = EntityEmbedding[String]("emb1", Embedding(Array(3.0f, 4.0f)))
    val entity2 = EntityEmbedding[String]("emb2", Embedding(Array(1.0f, 1.0f)))
    val entity3 = EntityEmbedding[String]("emb3", Embedding(Array(1.0f, 0.0f)))
    val embVector = Embedding(Array(3.0f, 4.0f))
    val entities = Seq(entity1, entity2, entity3)

    val temp = new LocalFile(Files.createTempDirectory("test").toFile)
    buildDummyIndex(temp, metric, entities, Bufferable.injectionOf[String])

    val queryIndex = TypedAnnoyQueryIndexWithFile[String, CosineDistance](
      2,
      metric,
      Bufferable.injectionOf[String],
      FuturePool.immediatePool,
      temp
    )

    val result =
      Await.result(queryIndex.queryWithDistance(embVector, 2, new AnnoyRuntimeParams(Some(100))))

    assert(result.size == 2)
    // Check typed id
    assert(result(0).neighbor == "emb1")
    assert(result(1).neighbor == "emb2")

    // Check distance
    assert(result(0).distance.distance === metric.distance(entity1.embedding, embVector).distance)
    assert(result(1).distance.distance === metric.distance(entity2.embedding, embVector).distance)
    temp.deleteDirectory()
  }
}
