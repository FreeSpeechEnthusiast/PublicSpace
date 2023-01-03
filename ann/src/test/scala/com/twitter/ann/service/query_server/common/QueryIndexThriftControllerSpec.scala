package com.twitter.ann.service.query_server.common

import com.twitter.ann.common._
import com.twitter.ann.common.EmbeddingType._
import com.twitter.ann.common.thriftscala.AnnQueryService.Query
import com.twitter.ann.common.thriftscala.NearestNeighborQuery
import com.twitter.ann.common.thriftscala.{Distance => ServiceDistance}
import com.twitter.ann.common.thriftscala.{RuntimeParams => ServiceRuntimeParams}
import com.twitter.ann.common.Distance
import com.twitter.ann.common.Queryable
import com.twitter.ann.common.RuntimeParams
import com.twitter.bijection.Injection
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.ml.api.embedding.Embedding
import com.twitter.util.Await
import com.twitter.util.Future
import java.nio.ByteBuffer
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite
import scala.util.Try
import org.scalatestplus.mockito.MockitoSugar

case class TestParams(long: Long) extends RuntimeParams
case class TestDistance(long: Long) extends Distance[TestDistance] {
  override def compare(that: TestDistance): Int = ???
  override def distance: Float = ???
}
case class Id(long: Long)

@RunWith(classOf[JUnitRunner])
class QueryIndexThriftControllerSpec extends FunSuite with MockitoSugar with BeforeAndAfter {
  val statsReceiver = new InMemoryStatsReceiver
  val runtimeParamInjection = mock[Injection[TestParams, ServiceRuntimeParams]]
  val distanceInjection = mock[Injection[TestDistance, ServiceDistance]]
  val key: Option[String] = None
  val key2: Option[String] = Some("123-456")
  val index = mock[Queryable[Id, TestParams, TestDistance]]
  val indexMap = Map(key -> index, key2 -> index)
  val thriftRuntimeParam = mock[ServiceRuntimeParams]
  val scalaRuntimeParams = mock[TestParams]
  val idInjection = mock[Injection[Id, Array[Byte]]]
  val id1 = mock[Id]
  val id2 = mock[Id]
  val idByte1 = Array(1.toByte)
  val idByte2 = Array(2.toByte)
  val scalaDistance = mock[TestDistance]
  val thriftDistance = mock[ServiceDistance]

  val service = new QueryIndexThriftController[Id, TestParams, TestDistance](
    statsReceiver,
    index,
    runtimeParamInjection,
    distanceInjection,
    idInjection
  ).query

  val emb = Embedding(Array(1.0f, 2.0f, 3.0f))
  val thriftEmb = embeddingSerDe.toThrift(emb)

  before {
    reset(runtimeParamInjection, distanceInjection, index)

    when(distanceInjection.apply(scalaDistance)).thenReturn(thriftDistance)
    when(runtimeParamInjection.invert(thriftRuntimeParam)).thenReturn(Try(scalaRuntimeParams))
    when(idInjection.apply(id1)).thenReturn(idByte1)
    when(idInjection.apply(id2)).thenReturn(idByte2)
  }

  test("query without distance return nearest neighbour without distance from index") {
    when(index.query(emb, 1, scalaRuntimeParams)).thenReturn(
      Future.value(List(id1))
    )

    val result = Await.result(
      service.apply(Query.Args(NearestNeighborQuery(thriftEmb, false, thriftRuntimeParam, 1))))

    val nearestNeighbours = result.nearestNeighbors
    assert(nearestNeighbours.size == 1)
    assert(nearestNeighbours.head.id == ByteBuffer.wrap(idByte1))
    assert(nearestNeighbours.head.distance.isEmpty)
  }

  test("query with distance return nearest neighbour with distance from index") {
    when(index.queryWithDistance(emb, 1, scalaRuntimeParams)).thenReturn(
      Future.value(List(NeighborWithDistance(id1, scalaDistance)))
    )

    val result = Await.result(
      service.apply(Query.Args(NearestNeighborQuery(thriftEmb, true, thriftRuntimeParam, 1))))

    val nearestNeighbours = result.nearestNeighbors
    assert(nearestNeighbours.size == 1)
    assert(nearestNeighbours.head.id == ByteBuffer.wrap(idByte1))
    assert(nearestNeighbours.head.distance.get == thriftDistance)
  }

  test("query with distance and key return nearest neighbour with distance from index") {
    when(index.queryWithDistance(emb, 1, scalaRuntimeParams)).thenReturn(
      Future.value(List(NeighborWithDistance(id1, scalaDistance)))
    )

    val result = Await.result(
      service.apply(
        Query.Args(NearestNeighborQuery(thriftEmb, true, thriftRuntimeParam, 1, Some("123-456")))))

    val nearestNeighbours = result.nearestNeighbors
    assert(nearestNeighbours.size == 1)
    assert(nearestNeighbours.head.id == ByteBuffer.wrap(idByte1))
    assert(nearestNeighbours.head.distance.get == thriftDistance)
  }

  test("query with multiple neighbours without distance") {
    when(index.queryWithDistance(emb, 2, scalaRuntimeParams)).thenReturn(
      Future.value(
        List(NeighborWithDistance(id1, scalaDistance), NeighborWithDistance(id2, scalaDistance)))
    )

    val result = Await.result(
      service.apply(Query.Args(NearestNeighborQuery(thriftEmb, true, thriftRuntimeParam, 2))))

    val nearestNeighbours = result.nearestNeighbors
    assert(nearestNeighbours.size == 2)
    assert(nearestNeighbours(0).id == ByteBuffer.wrap(idByte1))
    assert(nearestNeighbours(1).id == ByteBuffer.wrap(idByte2))
    assert(nearestNeighbours(0).distance.get == thriftDistance)
    assert(nearestNeighbours(1).distance.get == thriftDistance)
  }

  test("query with multiple neighbours but index return 0 neighbours") {
    when(index.queryWithDistance(emb, 2, scalaRuntimeParams)).thenReturn(
      Future.value(List())
    )

    val result = Await.result(
      service.apply(Query.Args(NearestNeighborQuery(thriftEmb, true, thriftRuntimeParam, 2))))

    val nearestNeighbours = result.nearestNeighbors
    assert(nearestNeighbours.isEmpty)
  }
}
