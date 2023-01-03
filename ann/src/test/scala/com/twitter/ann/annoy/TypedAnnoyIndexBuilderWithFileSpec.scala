package com.twitter.ann.annoy

import com.twitter.ann.annoy.AnnoyCommon.IndexFileName
import com.twitter.ann.annoy.AnnoyCommon.IndexIdMappingFileName
import com.twitter.ann.annoy.AnnoyCommon.MetaDataFileName
import com.twitter.ann.common._
import com.twitter.bijection.Bufferable
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.LocalFile
import com.twitter.util.Await
import com.twitter.util.FuturePool
import java.nio.file.Files
import org.apache.beam.sdk.io.LocalResources
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class TypedAnnoyIndexBuilderWithFileSpec extends FunSuite with MockitoSugar {
  val dimension = 2
  val numOfTrees = 1
  val metric = L2
  val entity1 = EntityEmbedding[String]("embedding1", Embedding(Array(1.0f, 2.0f)))
  val entity2 = EntityEmbedding[String]("embedding2", Embedding(Array(3.0f, 4.0f)))

  test(
    "TypedAnnoyIndexBuilderWithFile successfully serialize metadata, index and indexIdMapping to file") {
    val indexBuilder = TypedAnnoyIndexBuilderWithFile[String, L2Distance](
      dimension,
      numOfTrees,
      metric,
      Bufferable.injectionOf[String],
      FuturePool.immediatePool
    )
    indexBuilder.append(entity1)
    indexBuilder.append(entity2)

    val temp = new LocalFile(Files.createTempDirectory("test").toFile)
    indexBuilder.toDirectory(temp)

    val indexFile = temp.getChild(IndexFileName)
    val indexIdFile = temp.getChild(IndexIdMappingFileName)
    val metadataFile = temp.getChild(MetaDataFileName)

    // Successfully stores annoy index, metadata, indexIdMapping in file
    assert(indexFile.exists())
    assert(indexIdFile.exists())
    assert(metadataFile.exists())

    temp.deleteDirectory()
  }

  test(
    "TypedAnnoyIndexBuilderWithFile successfully serialize metadata, index and indexIdMapping to ResouceId file") {
    val indexBuilder = TypedAnnoyIndexBuilderWithFile[String, L2Distance](
      dimension,
      numOfTrees,
      metric,
      Bufferable.injectionOf[String],
      FuturePool.immediatePool
    )
    indexBuilder.append(entity1)
    indexBuilder.append(entity2)

    val tempFile = Files.createTempDirectory("test").toFile
    val temp = new LocalFile(tempFile)
    indexBuilder.toDirectory(LocalResources.fromFile(tempFile, /* isDirectory */ true))

    val indexFile = temp.getChild(IndexFileName)
    val indexIdFile = temp.getChild(IndexIdMappingFileName)
    val metadataFile = temp.getChild(MetaDataFileName)

    // Successfully stores annoy index, metadata, indexIdMapping in file
    assert(indexFile.exists())
    assert(indexIdFile.exists())
    assert(metadataFile.exists())

    temp.deleteDirectory()
  }

  test("TypedAnnoyIndexBuilderWithFile converts to queryable properly") {
    val indexBuilder = TypedAnnoyIndexBuilderWithFile[String, L2Distance](
      dimension,
      numOfTrees,
      metric,
      Bufferable.injectionOf[String],
      FuturePool.immediatePool
    )
    indexBuilder.append(entity1)
    indexBuilder.append(entity2)
    val querable = indexBuilder.toQueryable
    val param = AnnoyRuntimeParams(Some(2))
    val res = Await.result(querable.query(entity1.embedding, 1, param))
    assert(Set("embedding1") == res.toSet)
  }
}
