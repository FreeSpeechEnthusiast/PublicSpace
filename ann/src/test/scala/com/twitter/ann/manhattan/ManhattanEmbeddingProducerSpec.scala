package com.twitter.ann.manhattan

import com.twitter.ann.common.EmbeddingType.EmbeddingVector
import com.twitter.ml.api.embedding.Embedding
import com.twitter.stitch.Stitch
import com.twitter.storage.client.manhattan.kv.impl.{DescriptorP1L0, ValueDescriptor}
import com.twitter.storage.client.manhattan.kv.{ManhattanKVEndpoint, ManhattanValue}
import com.twitter.util.Await
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatestplus.mockito.MockitoSugar

class ManhattanEmbeddingProducerSpec extends FunSuite with MockitoSugar {
  val mockKeyDescriptor = mock[DescriptorP1L0.DKey[Int]]
  val mockFullKey = mock[DescriptorP1L0.FullKey[Int]]
  val valueDescriptor = mock[ValueDescriptor.EmptyValue[EmbeddingVector]]
  val manhattanEndpoint = mock[ManhattanKVEndpoint]
  val embedding = Embedding.apply(Array(10.0F))
  val manhattanValue = new ManhattanValue[EmbeddingVector](embedding)

  val manhattanEmbeddingProducer = new ManhattanEmbeddingProducer(
    mockKeyDescriptor,
    valueDescriptor,
    manhattanEndpoint
  )

  when(mockKeyDescriptor.withPkey(5))
    .thenReturn(mockFullKey)

  when(manhattanEndpoint.get(mockFullKey, valueDescriptor))
    .thenReturn(Stitch.apply(Some(manhattanValue)))

  test("produceEmbedding") {
    val result = manhattanEmbeddingProducer.produceEmbedding(5)
    assert(Await.result(Stitch.run(result)) == Some(embedding))
  }
}
