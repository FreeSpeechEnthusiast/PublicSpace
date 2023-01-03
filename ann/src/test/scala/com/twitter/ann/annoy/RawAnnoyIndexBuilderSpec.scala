package com.twitter.ann.annoy

import com.twitter.ann.annoy.AnnoyCommon.{IndexFileName, MetaDataFileName}
import com.twitter.ann.common.thriftscala.{AnnoyIndexMetadata, DistanceMetric}
import com.twitter.ann.common.{L2, L2Distance}
import com.twitter.mediaservices.commons.codec.{ArrayByteBufferCodec, ThriftByteBufferCodec}
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.LocalFile
import com.twitter.util.{Await, FuturePool}
import java.nio.file.Files
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class RawAnnoyIndexBuilderSpec extends FunSuite with MockitoSugar {
  test("RawAnnoyIndexBuilder successfully appends vector and returns long id") {
    val dimension = 2
    val numOfTrees = 1
    val metric = L2

    val vector1 = Embedding(Array(1.0f, 2.0f))
    val vector2 = Embedding(Array(3.0f, 4.0f))

    val indexBuilder =
      RawAnnoyIndexBuilder[L2Distance](dimension, numOfTrees, metric, FuturePool.immediatePool)

    // Returns correct long id
    val id1 = Await.result(indexBuilder.append(vector1))
    assert(id1 == 1)

    // Returns correct incremented long id
    val id2 = Await.result(indexBuilder.append(vector2))
    assert(id2 == 2)
  }

  test("RawAnnoyIndexBuilder successfully serialize metadata and index") {
    val dimension = 2
    val numOfTrees = 1
    val metric = L2
    val vector = Embedding(Array(1.0f, 2.0f))

    val indexBuilder =
      RawAnnoyIndexBuilder[L2Distance](dimension, numOfTrees, metric, FuturePool.immediatePool)
    indexBuilder.append(vector)

    val temp = new LocalFile(Files.createTempDirectory("test").toFile)
    indexBuilder.toDirectory(temp)

    // Successfully stores annoy index
    val indexFile = temp.getChild(IndexFileName)
    assert(indexFile.exists())

    val metadataFile = temp.getChild(MetaDataFileName)
    val expectedMetadataThrift = AnnoyIndexMetadata(
      dimension,
      DistanceMetric.L2,
      numOfTrees,
      1
    )

    val codec = new ThriftByteBufferCodec(AnnoyIndexMetadata)
    val thrift = codec.decode(ArrayByteBufferCodec.encode(metadataFile.getByteSource.read()))

    // Successfully serialize index metadata
    assert(expectedMetadataThrift == thrift)

    temp.deleteDirectory()
  }
}
