package com.twitter.ann.common

import com.twitter.ann.common.thriftscala.{
  CosineDistance => ServiceCosineDistance,
  Distance => ServiceDistance,
  L2Distance => ServiceL2Distance,
  InnerProductDistance => ServiceInnerProductDistance
}
import org.scalatest.FunSuite
import scala.util.Success

class DistanceInjectionsTest extends FunSuite {
  test("L2 apply") {
    val result = L2.apply(L2Distance(3.0f))
    assert(result == ServiceDistance.L2Distance(ServiceL2Distance(3.0)))
  }
  test("L2 invert") {
    val result = L2.invert(ServiceDistance.L2Distance(ServiceL2Distance(3.0)))
    assert(result == Success(L2Distance(3.0f)))
  }
  test("L2 invalid invert") {
    val result = L2.invert(
      ServiceDistance.CosineDistance(ServiceCosineDistance(3.0))
    )
    val exception = intercept[IllegalArgumentException](result.get)
    assert(
      exception.getMessage == "Expected an l2 distance but got CosineDistance(CosineDistance(3.0))"
    )
  }

  test("Cosine apply") {
    val result = Cosine.apply(CosineDistance(3.0f))
    assert(result == ServiceDistance.CosineDistance(ServiceCosineDistance(3.0f)))
  }
  test("Cosine invert") {
    val result = Cosine.invert(ServiceDistance.CosineDistance(ServiceCosineDistance(3.0)))
    assert(result == Success(CosineDistance(3.0f)))
  }
  test("Cosine invalid invert") {
    val result = Cosine.invert(
      ServiceDistance.L2Distance(ServiceL2Distance(3.0))
    )
    val exception = intercept[IllegalArgumentException](result.get)
    assert(
      exception.getMessage == "Expected a cosine distance but got L2Distance(L2Distance(3.0))"
    )
  }

  test("InnerProduct apply") {
    val result = InnerProduct.apply(InnerProductDistance(3.0f))
    assert(result == ServiceDistance.InnerProductDistance(ServiceInnerProductDistance(3.0f)))
  }
  test("InnerProduct invert") {
    val result =
      InnerProduct.invert(ServiceDistance.InnerProductDistance(ServiceInnerProductDistance(3.0)))
    assert(result == Success(InnerProductDistance(3.0f)))
  }
  test("InnerProduct invalid invert") {
    val result = InnerProduct.invert(
      ServiceDistance.L2Distance(ServiceL2Distance(3.0))
    )
    val exception = intercept[IllegalArgumentException](result.get)
    assert(
      exception.getMessage == "Expected a inner product distance but got L2Distance(L2Distance(3.0))"
    )
  }
}
