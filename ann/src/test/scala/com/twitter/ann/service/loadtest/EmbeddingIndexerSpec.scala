package com.twitter.ann.service.loadtest

import com.twitter.ann.common.{Appendable, CosineDistance, EntityEmbedding, Queryable}
import com.twitter.ml.api.embedding.Embedding
import com.twitter.util.{Await, Duration, Future, Time}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatestplus.mockito.MockitoSugar

class EmbeddingIndexerSpec extends FunSuite with MockitoSugar with BeforeAndAfter {
  val appendable = mock[Appendable[Long, TestParams, CosineDistance]]
  val queryable = mock[Queryable[Long, TestParams, CosineDistance]]

  val indexed1 = EntityEmbedding[Long](1, Embedding(Array(1.0f, 2.0f)))
  val indexed2 = EntityEmbedding[Long](2, Embedding(Array(3.0f, 4.0f)))

  val buildRecorder = mock[LoadTestBuildRecorder]

  val indexEmbeddings = Seq(
    indexed1,
    indexed2
  )

  before {
    reset(appendable, buildRecorder)
  }

  val indexer = new EmbeddingIndexer()
  test("indexEmbeddings") {
    val res = Time.withCurrentTimeFrozen { timeControl =>
      when(appendable.append(indexed1)).thenAnswer(new Answer[Future[Unit]] {
        override def answer(invocationOnMock: InvocationOnMock): Future[Unit] = {
          timeControl.advance(Duration.fromSeconds(1))
          Future.Unit
        }
      })
      when(appendable.append(indexed2)).thenAnswer(new Answer[Future[Unit]] {
        override def answer(invocationOnMock: InvocationOnMock): Future[Unit] = {
          timeControl.advance(Duration.fromSeconds(1))
          Future.Unit
        }
      })
      when(appendable.toQueryable).thenAnswer(
        new Answer[Queryable[Long, TestParams, CosineDistance]] {
          override def answer(
            invocationOnMock: InvocationOnMock
          ): Queryable[Long, TestParams, CosineDistance] = {
            timeControl.advance(Duration.fromNanoseconds(1))
            queryable
          }
        }
      )

      val res = indexer
        .indexEmbeddings(appendable, buildRecorder, indexEmbeddings, concurrencyLevel = 1)
      Await.result(res)
    }
    verify(appendable).append(indexed1)
    verify(appendable).append(indexed2)

    verify(appendable, times(1)).toQueryable

    verify(buildRecorder).recordIndexCreation(
      2,
      Duration.fromSeconds(2),
      Duration.fromNanoseconds(1)
    )
    assert(res === queryable)
  }

  test("indexEmbeddings fails when append fails") {
    val exception = new Exception("EmbeddingIndexerSpec")
    when(appendable.append(indexed1)).thenReturn(Future.Unit)
    when(appendable.append(indexed2)).thenReturn(Future.exception(exception))
    Time.withCurrentTimeFrozen { timeControl =>
      val res = indexer
        .indexEmbeddings(appendable, buildRecorder, indexEmbeddings, concurrencyLevel = 1)
      val actual = intercept[Exception] {
        Await.result(res)
      }
      assert(actual === exception)
    }
    verify(appendable).append(indexed1)
    verify(appendable).append(indexed2)
    verify(appendable, never()).toQueryable
  }
}
