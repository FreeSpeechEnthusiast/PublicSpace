package com.twitter.ann.scalding.offline.indexbuilder

import com.twitter.ann.common.Appendable
import com.twitter.ann.common.CosineDistance
import com.twitter.ann.common.Distance
import com.twitter.ann.common.RuntimeParams
import com.twitter.ann.common.{EntityEmbedding => ANNEntityEmbedding}
import com.twitter.ann.common.Serialization
import com.twitter.ann.hnsw.HnswParams
import com.twitter.cortex.ml.embeddings.common.EmbeddingFormat
import com.twitter.ml.api.embedding.Embedding
import com.twitter.ml.featurestore.lib.TokenEntity
import com.twitter.ml.featurestore.lib.embedding.EmbeddingWithEntity
import com.twitter.scalding.typed.TypedPipe
import com.twitter.scalding.Config
import com.twitter.scalding.Local
import com.twitter.search.common.file.AbstractFile
import com.twitter.util.Future
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.when
import org.mockito.Mockito.verify
import org.scalactic.TripleEquals
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.mockito.MockitoSugar

object IndexBuilderAppTestException extends Exception

trait MockableAppendableSerialization[T, P <: RuntimeParams, D <: Distance[D]]
    extends Appendable[T, P, D]
    with Serialization

case class EmbeddingMatcher(embedding: ANNEntityEmbedding[TokenEntity])
    extends ArgumentMatcher[ANNEntityEmbedding[TokenEntity]]
    with TripleEquals {
  override def matches(that: ANNEntityEmbedding[TokenEntity]): Boolean = {
    that match {
      case thatEmbedding: ANNEntityEmbedding[_] =>
        thatEmbedding.id == embedding.id &&
          thatEmbedding.embedding === embedding.embedding
      case _ => false
    }
  }
}

class IndexBuilderTest extends AnyFunSuite with MockitoSugar with BeforeAndAfter {

  val appendable = mock[MockableAppendableSerialization[TokenEntity, HnswParams, CosineDistance]]
  val outputDirectory = mock[AbstractFile]
  val format = mock[EmbeddingFormat[TokenEntity]]
  val one =
    EmbeddingWithEntity[TokenEntity](TokenEntity("one"), Embedding(Array(1.0f)))
  val two =
    EmbeddingWithEntity[TokenEntity](TokenEntity("two"), Embedding(Array(2.0f)))
  val oneEmbedding =
    ANNEntityEmbedding[TokenEntity](TokenEntity("one"), Embedding(Array(1.0f)))
  val twoEmbedding =
    ANNEntityEmbedding[TokenEntity](TokenEntity("two"), Embedding(Array(2.0f)))

  before {
    reset(appendable, outputDirectory, format)
  }

  def setupAppend(result: Future[Unit], expected: ANNEntityEmbedding[TokenEntity]): Unit = {
    when(appendable.append(argThat(EmbeddingMatcher(expected))))
      .thenReturn(result)
  }

  test("with zero embedding") {
    val embeddingsPipe = TypedPipe.from[EmbeddingWithEntity[TokenEntity]](Seq())
    when(format.getEmbeddings).thenReturn(embeddingsPipe)

    val result = IndexBuilder
      .run(
        format,
        embeddingLimit = None,
        appendable,
        concurrencyLevel = 5,
        outputDirectory,
        numDimensions = 1
      ).waitFor(Config.default, Local(true))

    result.get

    verify(appendable).toDirectory(outputDirectory)
  }

  test("with one embedding") {
    val embeddingsPipe = TypedPipe.from[EmbeddingWithEntity[TokenEntity]](
      Seq(
        one
      ))
    when(format.getEmbeddings).thenReturn(embeddingsPipe)
    setupAppend(Future.Unit, oneEmbedding)

    val result = IndexBuilder
      .run(
        format,
        embeddingLimit = None,
        appendable,
        concurrencyLevel = 5,
        outputDirectory,
        numDimensions = 1
      ).waitFor(Config.default, Local(true))

    result.get

    verify(appendable).toDirectory(outputDirectory)
    verify(appendable).append(argThat(EmbeddingMatcher(oneEmbedding)))
  }

  test("with a failed append ") {
    val embeddingsPipe = TypedPipe.from[EmbeddingWithEntity[TokenEntity]](
      Seq(
        one
      ))
    when(format.getEmbeddings).thenReturn(embeddingsPipe)
    setupAppend(Future.exception(IndexBuilderAppTestException), oneEmbedding)

    val result = IndexBuilder
      .run(
        format,
        embeddingLimit = None,
        appendable,
        concurrencyLevel = 5,
        outputDirectory,
        numDimensions = 1
      ).waitFor(Config.default, Local(true))

    verify(appendable, never()).toDirectory(outputDirectory)
    verify(appendable).append(argThat(EmbeddingMatcher(oneEmbedding)))

    intercept[IndexBuilderAppTestException.type](result.get)
  }

  test("with two embeddings") {
    val embeddingsPipe = TypedPipe.from[EmbeddingWithEntity[TokenEntity]](
      Seq(
        one,
        two
      ))
    when(format.getEmbeddings).thenReturn(embeddingsPipe)
    setupAppend(Future.Unit, oneEmbedding)
    setupAppend(Future.Unit, twoEmbedding)

    val result = IndexBuilder
      .run(
        format,
        embeddingLimit = None,
        appendable,
        concurrencyLevel = 5,
        outputDirectory,
        numDimensions = 1
      ).waitFor(Config.default, Local(true))

    result.get

    verify(appendable).toDirectory(outputDirectory)
    verify(appendable).append(argThat(EmbeddingMatcher(oneEmbedding)))
    verify(appendable).append(argThat(EmbeddingMatcher(twoEmbedding)))
  }

  test("limit the input embeddings") {
    val embeddingsPipe = TypedPipe.from[EmbeddingWithEntity[TokenEntity]](
      Seq(
        one,
        two
      ))
    when(format.getEmbeddings).thenReturn(embeddingsPipe)
    setupAppend(Future.Unit, oneEmbedding)

    val result = IndexBuilder
      .run(
        format,
        embeddingLimit = Some(1),
        appendable,
        concurrencyLevel = 5,
        outputDirectory,
        numDimensions = 1
      ).waitFor(Config.default, Local(true))

    result.get

    verify(appendable).toDirectory(outputDirectory)
    verify(appendable).append(argThat(EmbeddingMatcher(oneEmbedding)))
    verify(appendable, never()).append(argThat(EmbeddingMatcher(twoEmbedding)))
  }
}
