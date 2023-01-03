package com.twitter.ann.serialization

import com.twitter.ann.common.EmbeddingType._
import com.twitter.ann.common.EntityEmbedding
import com.twitter.ann.serialization.thriftscala.PersistedEmbedding
import com.twitter.bijection.StringCodec
import com.twitter.ml.api.embedding.Embedding
import java.nio.ByteBuffer
import org.scalatest.FunSuite

class PersistedEmbeddingInjectionTest extends FunSuite {
  val injection = new PersistedEmbeddingInjection(StringCodec.utf8)
  test("Converting from an embedding to a persisted embedding") {
    val embedding = EntityEmbedding[String]("a", Embedding(Array(1.0f, 2.0f, 3.0f)))
    val persistedEmbedding = injection(embedding)
    val expected = PersistedEmbedding(
      ByteBuffer.wrap("a".getBytes("UTF8")),
      embeddingSerDe.toThrift(Embedding(Array(1.0f, 2.0f, 3.0f)))
    )
    assert(persistedEmbedding == expected)
  }

  test("Converting from a persisted embedding to an embedding") {
    val persistedEmbedding = PersistedEmbedding(
      ByteBuffer.wrap("a".getBytes("UTF8")),
      embeddingSerDe.toThrift(Embedding(Array(1.0f, 2.0f, 3.0f)))
    )
    val embeddingTry = injection.invert(persistedEmbedding)
    val expected = EntityEmbedding[String]("a", Embedding(Array(1.0f, 2.0f, 3.0f)))

    assert(embeddingTry.get == expected)
  }

  test("Converting from an invalid persisted embedding") {
    val persistedEmbedding = PersistedEmbedding(
      ByteBuffer.wrap(Array(255.toByte)),
      embeddingSerDe.toThrift(Embedding(Array(1.0f, 2.0f, 3.0f)))
    )
    val embeddingTry = injection.invert(persistedEmbedding)

    assert(embeddingTry.isFailure)
  }
}
