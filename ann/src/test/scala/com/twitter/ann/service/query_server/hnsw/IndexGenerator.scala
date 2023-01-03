package com.twitter.ann.service.query_server.hnsw

import com.twitter.ann.common._
import com.twitter.ann.hnsw.HnswParams
import com.twitter.ann.hnsw.TypedHnswIndex
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.FileUtils
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.FuturePool
import scala.io.Source
import scala.util.Using

object IndexGenerator {
  private[hnsw] val dimen = 3
  private[hnsw] val injection = AnnInjections.LongInjection
  private[hnsw] val metric = InnerProduct
  private[hnsw] val idType = "long"
  private[this] val maxM = 16
  private[this] val efConstruction = 200
  private[this] val timestamp = "1514764800"

  def main(args: Array[String]): Unit = {
    val embeddings = lines("embeddings.txt")
    val indexInnerProduct = index(InnerProduct, embeddings.size)
    val indexL2 = index(L2, embeddings.size)
    val indexCosine = index(Cosine, embeddings.size)

    val futures = embeddings.map { line =>
      val data = line.split("\\t")
      val id = data(0).toLong
      val vector = data.drop(1).map(_.toFloat)
      val embedding = EntityEmbedding(id, Embedding(vector))
      indexInnerProduct.append(embedding)
      indexL2.append(embedding)
      indexCosine.append(embedding)
    }

    Await.result(Future.collect(futures))
    indexInnerProduct.toDirectory(FileUtils.getFileHandle(s"hnsw_inner_product/$timestamp"))
    indexL2.toDirectory(FileUtils.getFileHandle(s"hnsw_l2/$timestamp"))
    indexCosine.toDirectory(FileUtils.getFileHandle(s"hnsw_cosine/$timestamp"))
  }

  private[hnsw] def lines(fileName: String): List[String] = {
    val path = s"ann/src/test/resources/service/query_server/hnsw/${fileName}"
    Using(Source.fromFile(path)) { _.getLines().toList }.get
  }

  private[this] def index[D <: Distance[D]](
    metric: Metric[D],
    size: Int
  ): Appendable[Long, HnswParams, D] with Serialization = {
    TypedHnswIndex.serializableIndex[Long, D](
      dimen,
      metric,
      efConstruction,
      maxM,
      size,
      AnnInjections.LongInjection,
      ReadWriteFuturePool(FuturePool.immediatePool)
    )
  }
}
