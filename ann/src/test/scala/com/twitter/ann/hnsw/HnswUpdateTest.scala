package com.twitter.ann.hnsw

import com.twitter.ann.common._
import com.twitter.bijection.Injection
import com.twitter.ml.api.embedding.Embedding
import com.twitter.util.{Await, Future, FuturePool}
import java.util.Random
import java.util.concurrent.Executors
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatestplus.mockito.MockitoSugar

class HnswUpdateTest extends FunSuite with BeforeAndAfter with MockitoSugar {

  val dimension = 4
  val trainDataSetSize = 3000
  val dummyDataMultiplier = 3
  val efConstruction = 14
  val maxM = 5
  val threads = 1
  val ef = 8

  val rng = new Random(100500)

  def time[T](fn: => T): (T, Long) = {
    val start = System.currentTimeMillis()
    val result = fn
    val end = System.currentTimeMillis()
    (result, (end - start))
  }

  test("ANN functional update test") {

    val dummyData = (0 until dummyDataMultiplier).map { _ =>
      (0 until trainDataSetSize).toList map { id =>
        val vec = Array.fill(dimension)(rng.nextFloat() * 50)
        EntityEmbedding[Int](id, Embedding(vec))
      }
    }

    val final_embeddings = (0 until trainDataSetSize).toList map { id =>
      val vec = Array.fill(dimension)(rng.nextFloat() * 50)
      EntityEmbedding[Int](id, Embedding(vec))
    }

    val distance = L2
    val exec = Executors.newFixedThreadPool(threads)
    val pool = ReadWriteFuturePool(FuturePool.apply(exec))
    val injection = Injection.int2BigEndian

    val index = Hnsw[Int, L2Distance](
      dimension,
      distance,
      efConstruction,
      maxM,
      trainDataSetSize,
      pool,
      JMapBasedIdEmbeddingMap
        .applyInMemoryWithSerialization[Int](trainDataSetSize, injection)
    )

    val hnswMultiThread = SerializableHnsw[Int, L2Distance](
      index,
      injection
    )

    val (_, buildTime) = time {
      // Adding dummy data
      0 until dummyDataMultiplier map { i =>
        Await.result(Future.collect(dummyData(i).map { emb =>
          hnswMultiThread.update(emb)
        }))
      }
      // Adding final data
      Await.result(Future.collect(final_embeddings.map { emb =>
        hnswMultiThread.update(emb)
      }))
    }

    val hnswQueryable = hnswMultiThread.toQueryable

    val (result, searchTime) = time {
      val futures = (0 until final_embeddings.length).toList.map { id =>
        hnswQueryable
          .query(final_embeddings(id).embedding, 1, HnswParams(ef)).map(result => result(0) == id)
      }
      Await.result(Future.collect(futures))
    }
    val recall = (1.0f * result.count(_ == true)) / final_embeddings.length

    assert(recall > 0.995, "Recall is to small:" + recall)

    exec.shutdown()
  }
}
