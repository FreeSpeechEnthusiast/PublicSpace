package com.twitter.ann.brute_force

import com.google.common.io.ByteStreams
import com.twitter.ann.common.EmbeddingType._
import com.twitter.ann.common.Distance
import com.twitter.ann.common.EntityEmbedding
import com.twitter.ann.common.Metric
import com.twitter.ann.serialization.thriftscala.PersistedEmbedding
import com.twitter.ann.serialization.PersistedEmbeddingInjection
import com.twitter.ann.serialization.ThriftIteratorIO
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.AbstractFile
import com.twitter.search.common.file.LocalFile
import com.twitter.util.FuturePool
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.beam.sdk.io.LocalResources
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito._
import org.mockito.stubbing.Answer
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import scala.util.Failure
import scala.util.Success
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

class TestDistance extends Distance[TestDistance] {
  override def compare(that: TestDistance): Int = ???
  override def distance: Float = ???
}

object BruteForceSerializationTestException extends Exception()

@RunWith(classOf[JUnitRunner])
class BruteForceSerializationTest extends AnyFunSuite with MockitoSugar with BeforeAndAfter {
  val metric = mock[Metric[TestDistance]]
  val embeddingInjection = mock[PersistedEmbeddingInjection[Int]]
  val futurePool = FuturePool.immediatePool
  val thriftIteratorIo = mock[ThriftIteratorIO[PersistedEmbedding]]

  val bruteForceIndex = mock[BruteForceIndex[Int, TestDistance]]

  val serializableIndex = new SerializableBruteForceIndex[Int, TestDistance](
    bruteForceIndex,
    embeddingInjection,
    thriftIteratorIo
  )

  val factory = mock[(
    Metric[TestDistance],
    FuturePool,
    Iterator[EntityEmbedding[Int]]
  ) => BruteForceIndex[Int, TestDistance]]

  val deserialization = new BruteForceDeserialization[Int, TestDistance](
    metric,
    embeddingInjection,
    futurePool,
    thriftIteratorIo,
    factory
  )

  after {
    reset(thriftIteratorIo, factory)
  }

  def setupThriftIteratorIoOuput(
    expected: Iterator[PersistedEmbedding],
    fakeContents: Array[Byte]
  ): OngoingStubbing[Unit] = {
    when(thriftIteratorIo.toOutputStream(any[Iterator[PersistedEmbedding]], any[OutputStream]))
      .thenAnswer(new Answer[Unit] {
        override def answer(invocationOnMock: InvocationOnMock): Unit = {
          val iterator = invocationOnMock.getArgument(0, classOf[Iterator[PersistedEmbedding]])
          assert(iterator sameElements expected)
          val outputStream = invocationOnMock.getArgument(1, classOf[OutputStream])
          outputStream.write(fakeContents)
        }
      })
  }

  def setupThriftIteratorIoInput(
    result: Iterator[PersistedEmbedding],
    expectedContents: Array[Byte]
  ): OngoingStubbing[Iterator[PersistedEmbedding]] = {
    when(thriftIteratorIo.fromInputStream(any[InputStream]))
      .thenAnswer(new Answer[Iterator[PersistedEmbedding]] {
        override def answer(invocationOnMock: InvocationOnMock): Iterator[PersistedEmbedding] = {
          val inputStream = invocationOnMock.getArgument(0, classOf[InputStream])
          assert(ByteStreams.toByteArray(inputStream) sameElements expectedContents)
          result
        }
      })
  }

  /**
   * @param expectedOption if this is present assert the assert that the expect embedding is what is
   *                       passed into the factory.
   * @return
   */
  def setupFactory(
    expectedOption: Option[Iterator[EntityEmbedding[Int]]] = None
  ): OngoingStubbing[BruteForceIndex[Int, TestDistance]] = {
    when(factory.apply(eqTo(metric), eqTo(futurePool), any[Iterator[EntityEmbedding[Int]]]))
      .thenAnswer(new Answer[BruteForceIndex[Int, TestDistance]] {
        override def answer(
          invocationOnMock: InvocationOnMock
        ): BruteForceIndex[Int, TestDistance] = {
          val embeddings =
            invocationOnMock.getArgument(2, classOf[Iterator[EntityEmbedding[Int]]])
          expectedOption
            .map { expected =>
              assert(embeddings sameElements expected)
            }.getOrElse {
              // Just consume the iterator. The code here is not important
              embeddings.toList
            }
          bruteForceIndex
        }
      })
  }

  def tempDirectory(): AbstractFile = {
    val tempFile =
      Files.createTempDirectory("brute_force_serialization_test_temp_dir").toFile
    tempFile.deleteOnExit()
    new LocalFile(tempFile)
  }

  val embedding1 = EntityEmbedding[Int](1, Embedding(Array(1.0f)))
  val embedding2 = EntityEmbedding[Int](2, Embedding(Array(2.0f)))

  val persistedEmbedding1 = PersistedEmbedding(
    ByteBuffer.wrap(Array(5)),
    embeddingSerDe.toThrift(Embedding(Array(5.0f)))
  )
  val persistedEmbedding2 = PersistedEmbedding(
    ByteBuffer.wrap(Array(6)),
    embeddingSerDe.toThrift(Embedding(Array(6.0f)))
  )
  val badPersistedEmbedding = PersistedEmbedding(
    ByteBuffer.wrap(Array(7)),
    embeddingSerDe.toThrift(Embedding(Array(7.0f)))
  )

  when(embeddingInjection.apply(embedding1)).thenReturn(persistedEmbedding1)
  when(embeddingInjection.apply(embedding2)).thenReturn(persistedEmbedding2)

  when(embeddingInjection.invert(persistedEmbedding1)).thenReturn(Success(embedding1))
  when(embeddingInjection.invert(persistedEmbedding2)).thenReturn(Success(embedding2))
  when(embeddingInjection.invert(badPersistedEmbedding))
    .thenReturn(Failure(BruteForceSerializationTestException))

  test("serializing an empty index") {
    val fakeContents = Array(5.toByte)
    when(bruteForceIndex.linkedQueue).thenReturn(new ConcurrentLinkedQueue[EntityEmbedding[Int]])
    setupThriftIteratorIoOuput(Iterator(), fakeContents)
    val temp = tempDirectory()
    serializableIndex.toDirectory(temp)
    val contents = temp.getChild(BruteForceIndex.DataFileName).getByteSource.read()
    assert(contents === fakeContents)
    temp.delete()
  }

  test("serializing an 1 element index") {
    val fakeContents = Array(6.toByte)
    val linkedQueue = new ConcurrentLinkedQueue[EntityEmbedding[Int]]
    linkedQueue.add(embedding1)
    when(bruteForceIndex.linkedQueue).thenReturn(linkedQueue)
    setupThriftIteratorIoOuput(Iterator(persistedEmbedding1), fakeContents)
    val temp = tempDirectory()
    serializableIndex.toDirectory(temp)
    val contents = temp.getChild(BruteForceIndex.DataFileName).getByteSource.read()
    assert(contents === fakeContents)
    temp.delete()
  }

  test("serializing a 2 element index") {
    val fakeContents = Array(7.toByte)
    val linkedQueue = new ConcurrentLinkedQueue[EntityEmbedding[Int]]
    linkedQueue.add(embedding1)
    linkedQueue.add(embedding2)
    when(bruteForceIndex.linkedQueue).thenReturn(linkedQueue)
    setupThriftIteratorIoOuput(Iterator(persistedEmbedding1, persistedEmbedding2), fakeContents)
    val temp = tempDirectory()
    serializableIndex.toDirectory(temp)
    val contents = temp.getChild(BruteForceIndex.DataFileName).getByteSource.read()
    assert(contents === fakeContents)
    temp.delete()
  }

  test("serializing a 2 element index into ResourceId") {
    val fakeContents = Array(7.toByte)
    val linkedQueue = new ConcurrentLinkedQueue[EntityEmbedding[Int]]
    linkedQueue.add(embedding1)
    linkedQueue.add(embedding2)
    when(bruteForceIndex.linkedQueue).thenReturn(linkedQueue)
    setupThriftIteratorIoOuput(Iterator(persistedEmbedding1, persistedEmbedding2), fakeContents)
    val tempFile =
      Files.createTempDirectory("brute_force_serialization_test_temp_dir").toFile
    tempFile.deleteOnExit()
    val temp = new LocalFile(tempFile)
    serializableIndex.toDirectory(LocalResources.fromFile(tempFile, true))
    val contents = temp.getChild(BruteForceIndex.DataFileName).getByteSource.read()
    assert(contents === fakeContents)
    temp.delete()
  }

  test("deserializing an empty index") {
    val fakeContents = Array(8.toByte)
    val temp = tempDirectory()
    temp.getChild(BruteForceIndex.DataFileName).getByteSink.write(fakeContents)
    setupThriftIteratorIoInput(Iterator(), fakeContents)
    setupFactory(Some(Iterator()))
    val result = deserialization.fromDirectory(temp)
    assert(result == bruteForceIndex)
  }

  test("deserializing an index with 1 entry") {
    val fakeContents = Array(9.toByte)
    val temp = tempDirectory()
    temp.getChild(BruteForceIndex.DataFileName).getByteSink.write(fakeContents)
    setupThriftIteratorIoInput(Iterator(persistedEmbedding1), fakeContents)
    setupFactory(Some(Iterator(embedding1)))
    val result = deserialization.fromDirectory(temp)
    assert(result == bruteForceIndex)
  }

  test("deserializing an index with 2 entries") {
    val fakeContents = Array(10.toByte)
    val temp = tempDirectory()
    temp.getChild(BruteForceIndex.DataFileName).getByteSink.write(fakeContents)
    setupThriftIteratorIoInput(Iterator(persistedEmbedding1, persistedEmbedding2), fakeContents)
    setupFactory(Some(Iterator(embedding1, embedding2)))
    val result = deserialization.fromDirectory(temp)
    assert(result == bruteForceIndex)
  }

  test("deserializing an index with bad persisted embedding") {
    val fakeContents = Array(11.toByte)
    val temp = tempDirectory()
    temp.getChild(BruteForceIndex.DataFileName).getByteSink.write(fakeContents)
    setupThriftIteratorIoInput(Iterator(badPersistedEmbedding), fakeContents)
    // Since the iterators are lazy you need to actually consume the iterator. This line causes the
    // iterator to be consumed.
    setupFactory()
    intercept[BruteForceSerializationTestException.type](deserialization.fromDirectory(temp))
  }

}
