package com.twitter.ann.common

import com.twitter.ann.common.EmbeddingType._
import com.twitter.ann.common.thriftscala.{
  NearestNeighbor,
  NearestNeighborQuery,
  NearestNeighborResult,
  CosineDistance => ServiceCosineDistance,
  Distance => ServiceDistance,
  RuntimeParams => ServiceRuntimeParams
}
import com.twitter.bijection.{Bufferable, Injection, InversionFailure}
import com.twitter.finagle.Service
import com.twitter.ml.api.embedding.Embedding
import com.twitter.util.{Await, Future}
import java.nio.ByteBuffer
import org.junit.Assert._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FunSuite}
import scala.util.{Failure, Success}
import org.scalatestplus.mockito.MockitoSugar

case class ServiceClientQueryableTestRuntimeParams(id: Long) extends RuntimeParams
case class ServiceClientQueryableTestDistance(id: Long)
    extends Distance[ServiceClientQueryableTestDistance] {
  override def compare(that: ServiceClientQueryableTestDistance): Int = ???
  override def distance: Float = ???
}
class ServiceClientQueryableTest extends FunSuite with MockitoSugar with BeforeAndAfter {

  val idInjection = Bufferable.injectionOf[Long]
  val id1 = 1L
  val id2 = 2L
  val id1ByteArray = idInjection.apply(id1)
  val id2ByteArray = idInjection.apply(id2)

  val distance1 = ServiceClientQueryableTestDistance(5)
  val distance2 = ServiceClientQueryableTestDistance(6)

  val serviceDistance1 = ServiceDistance.CosineDistance(ServiceCosineDistance(1.0))
  val serviceDistance2 = ServiceDistance.CosineDistance(ServiceCosineDistance(2.0))

  val thriftResultsWithoutDistance = NearestNeighborResult(
    Seq(
      NearestNeighbor(ByteBuffer.wrap(id1ByteArray)),
      NearestNeighbor(ByteBuffer.wrap(id2ByteArray))
    )
  )

  // invalid because we expect byte array of long for ID instead of int
  val invalidThriftResultsWithDistance = NearestNeighborResult(
    Seq(
      NearestNeighbor(ByteBuffer.wrap(Array(1.toByte)), Some(serviceDistance1)),
      NearestNeighbor(ByteBuffer.wrap(Array(2.toByte)), Some(serviceDistance2))
    )
  )

  val thriftResultsWithDistance = NearestNeighborResult(
    Seq(
      NearestNeighbor(ByteBuffer.wrap(id1ByteArray), Some(serviceDistance1)),
      NearestNeighbor(ByteBuffer.wrap(id2ByteArray), Some(serviceDistance2))
    )
  )

  val scalaVector = Embedding(Array(1.0f, 2.0f))
  val thriftVector = embeddingSerDe.toThrift(scalaVector)
  val neighbors = 32

  val service = mock[Service[NearestNeighborQuery, NearestNeighborResult]]
  val runtimeParamInjection =
    mock[Injection[ServiceClientQueryableTestRuntimeParams, ServiceRuntimeParams]]
  val distanceInjection = mock[Injection[ServiceClientQueryableTestDistance, ServiceDistance]]
  val thriftRuntimeParams = mock[ServiceRuntimeParams]
  val scalaRuntimeParams = mock[ServiceClientQueryableTestRuntimeParams]

  val client = new ServiceClientQueryable(
    service,
    runtimeParamInjection,
    distanceInjection,
    idInjection
  )

  before {
    reset(service, runtimeParamInjection, distanceInjection)
  }

  test("query") {
    when(
      service.apply(
        NearestNeighborQuery(
          thriftVector,
          withDistance = false,
          runtimeParams = thriftRuntimeParams,
          numberOfNeighbors = neighbors
        )))
      .thenReturn(Future.value(thriftResultsWithoutDistance))
    when(runtimeParamInjection.apply(scalaRuntimeParams))
      .thenReturn(thriftRuntimeParams)
    val result = client.query(scalaVector, neighbors, scalaRuntimeParams)
    val expected = List(id1, id2)
    assertEquals(expected, Await.result(result))
  }

  test("query with invalid id") {
    when(
      service.apply(
        NearestNeighborQuery(
          thriftVector,
          withDistance = false,
          runtimeParams = thriftRuntimeParams,
          numberOfNeighbors = neighbors
        )))
      .thenReturn(Future.value(invalidThriftResultsWithDistance))
    when(runtimeParamInjection.apply(scalaRuntimeParams))
      .thenReturn(thriftRuntimeParams)
    val result = client.query(scalaVector, neighbors, scalaRuntimeParams)
    // service return a byte array of int, inversion should fail
    intercept[InversionFailure](Await.result(result))
  }

  test("query with service exception") {
    val exception = new Exception("service exception")
    when(
      service.apply(
        NearestNeighborQuery(
          thriftVector,
          withDistance = false,
          runtimeParams = thriftRuntimeParams,
          numberOfNeighbors = neighbors
        )))
      .thenReturn(Future.exception(exception))
    when(runtimeParamInjection.apply(scalaRuntimeParams))
      .thenReturn(thriftRuntimeParams)
    val result = client.query(scalaVector, neighbors, scalaRuntimeParams)
    val actualException = intercept[Exception](Await.result(result))
    assert(actualException == exception)
  }

  test("queryWithDistance") {
    when(
      service.apply(
        NearestNeighborQuery(
          thriftVector,
          withDistance = true,
          runtimeParams = thriftRuntimeParams,
          numberOfNeighbors = neighbors
        )))
      .thenReturn(Future.value(thriftResultsWithDistance))
    when(runtimeParamInjection.apply(scalaRuntimeParams))
      .thenReturn(thriftRuntimeParams)
    when(distanceInjection.invert(serviceDistance1))
      .thenReturn(Success(distance1))
    when(distanceInjection.invert(serviceDistance2))
      .thenReturn(Success(distance2))

    val result = client.queryWithDistance(scalaVector, neighbors, scalaRuntimeParams)
    val expected = List(NeighborWithDistance(id1, distance1), NeighborWithDistance(id2, distance2))
    assert(Await.result(result) == expected)
  }

  test("queryWithDistance with invalid id") {
    when(
      service.apply(
        NearestNeighborQuery(
          thriftVector,
          withDistance = true,
          runtimeParams = thriftRuntimeParams,
          numberOfNeighbors = neighbors
        )))
      .thenReturn(Future.value(invalidThriftResultsWithDistance))
    when(runtimeParamInjection.apply(scalaRuntimeParams))
      .thenReturn(thriftRuntimeParams)

    val result = client.queryWithDistance(scalaVector, neighbors, scalaRuntimeParams)
    intercept[InversionFailure](Await.result(result))
  }

  test("queryWithDistance with invalid distance") {
    val exception = new Exception("invalid distance")
    when(
      service.apply(
        NearestNeighborQuery(
          thriftVector,
          withDistance = true,
          runtimeParams = thriftRuntimeParams,
          numberOfNeighbors = neighbors
        )))
      .thenReturn(Future.value(thriftResultsWithDistance))
    when(runtimeParamInjection.apply(scalaRuntimeParams))
      .thenReturn(thriftRuntimeParams)
    when(distanceInjection.invert(serviceDistance1))
      .thenReturn(Failure(exception))
    when(distanceInjection.invert(serviceDistance2))
      .thenReturn(Success(distance2))

    val result = client.queryWithDistance(scalaVector, neighbors, scalaRuntimeParams)
    val actualException = intercept[Exception](Await.result(result))
    assert(actualException == exception)
  }

  test("queryWithDistance with service exception") {
    val exception = new Exception("service exception")
    when(
      service.apply(
        NearestNeighborQuery(
          thriftVector,
          withDistance = true,
          runtimeParams = thriftRuntimeParams,
          numberOfNeighbors = neighbors
        )))
      .thenReturn(Future.exception(exception))
    when(runtimeParamInjection.apply(scalaRuntimeParams))
      .thenReturn(thriftRuntimeParams)
    val result = client.queryWithDistance(scalaVector, neighbors, scalaRuntimeParams)
    val actualException = intercept[Exception](Await.result(result))
    assert(actualException == exception)
  }
}
