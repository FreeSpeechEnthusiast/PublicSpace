package com.twitter.ann.service.query_server.hnsw

import com.twitter.ann.common.thriftscala._
import com.twitter.ann.common.{Distance => SDistance, _}
import com.twitter.ann.hnsw.HnswCommon
import com.twitter.ann.hnsw.HnswParams
import com.twitter.ann.common.thriftscala.NearestNeighborQuery
import com.twitter.ann.common.thriftscala.NearestNeighborResult
import com.twitter.ann.common.thriftscala.{Distance => ServiceDistance}
import com.twitter.bijection.Injection
import com.twitter.finagle.Service
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.ml.api.embedding.Embedding
import com.twitter.search.common.file.AbstractFile
import com.twitter.search.common.file.LocalFile
import com.twitter.util.Await
import com.twitter.util.Future
import java.io.File
import org.scalactic.Equality
import org.scalactic.TolerantNumerics
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite

trait HnswQueryServerFeatureTestBase extends AnyFunSuite with BeforeAndAfter with Matchers {
  implicit val floatEquality: Equality[Float] = TolerantNumerics.tolerantFloatEquality(0.01f)
  // For more info regarding config and data in the index, refer:
  // ann/src/test/resources/service/query_server/hnsw/README
  // Code used to generate the index refer: com.twitter.ann.service.query_server.hnsw.IndexGenerator
  private[this] val queries =
    IndexGenerator.lines("queries.txt").map { line =>
      Embedding(line.split("\\t").map(_.toFloat))
    }

  protected def run[D <: SDistance[D]](
    metric: Metric[D] with Injection[D, ServiceDistance],
    metricString: String,
    indexDirectory: String,
    knnResultFile: String
  ): Unit = {
    // Querying for 2 neighbours
    val srv = server(metricString, indexDir(indexDirectory))
    val client = serviceClient(srv, metric)
    val result = knnResult(knnResultFile)
    (queries, result).zipped.foreach { (query, trueNeighbours) =>
      val nns = Await.result(client.queryWithDistance(query, 10, HnswParams(200)))

      val intersection = trueNeighbours.map(_._1).toSet.intersect(nns.map(_.neighbor).toSet)
      // Cannot expect to get a recall of 1. So just assuming at least 7 in 10 neighbours match
      assert(intersection.size >= 7)

      val trueNeighbourMap = trueNeighbours.toMap
      nns foreach { nn =>
        if (intersection.contains(nn.neighbor)) {
          val distance = trueNeighbourMap(nn.neighbor)
          assert(distance === nn.distance.distance)
        }
      }
    }

    srv.close()
  }

  def server(metricString: String, indexDir: AbstractFile): EmbeddedThriftServer = ???

  private[this] def serviceClient[D <: SDistance[D]](
    server: EmbeddedThriftServer,
    metric: Metric[D] with Injection[D, ServiceDistance]
  ): Queryable[Long, HnswParams, D] = {
    val client = server.thriftClient[AnnQueryService.MethodPerEndpoint](clientId = "testClient")
    val service = new Service[NearestNeighborQuery, NearestNeighborResult] {
      override def apply(request: NearestNeighborQuery): Future[NearestNeighborResult] =
        client.query(request)
    }
    new ServiceClientQueryable(
      service,
      HnswCommon.RuntimeParamsInjection,
      metric,
      AnnInjections.LongInjection
    )
  }

  def indexDir(dir: String) = new LocalFile(
    new File(s"ann/src/test/resources/service/query_server/hnsw/${dir}"))

  private[this] def knnResult(fileName: String): List[List[(Long, Float)]] = {
    IndexGenerator.lines(fileName).map { line =>
      line.split("\\t").toList.map { x =>
        val nn = x.split(":")
        (nn(0).toLong, nn(1).toFloat)
      }
    }
  }
}
