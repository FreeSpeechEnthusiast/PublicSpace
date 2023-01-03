package com.twitter.ann.serialization

import com.twitter.ann.common.EmbeddingType._
import com.twitter.ann.serialization.thriftscala.PersistedEmbedding
import com.twitter.ml.api.embedding.Embedding
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import org.scalatest.FunSuite

class ThriftIteratorIOTest extends FunSuite {
  val one =
    PersistedEmbedding(ByteBuffer.wrap(Array(1)), embeddingSerDe.toThrift(Embedding(Array(1.0f))))
  val two =
    PersistedEmbedding(ByteBuffer.wrap(Array(2)), embeddingSerDe.toThrift(Embedding(Array(1.0f))))

  val io = new ThriftIteratorIO[PersistedEmbedding](PersistedEmbedding)
  test("read an empty file") {
    val embeddings = io.fromInputStream(new ByteArrayInputStream(Array()))
    assert(embeddings sameElements Iterator())
  }

  test("read and write one item") {
    val outputStream = new ByteArrayOutputStream()
    io.toOutputStream(Iterator(one), outputStream)
    val embeddings = io.fromInputStream(new ByteArrayInputStream(outputStream.toByteArray))
    assert(embeddings sameElements Iterator(one))
  }

  test("read and write two items") {
    val outputStream = new ByteArrayOutputStream()
    io.toOutputStream(Iterator(one, two), outputStream)
    val embeddings = io.fromInputStream(new ByteArrayInputStream(outputStream.toByteArray))
    assert(embeddings sameElements Iterator(one, two))
  }
}
