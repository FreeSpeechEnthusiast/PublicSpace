package com.twitter.ann.common

import com.twitter.ml.api.embedding.Embedding
import org.scalactic.TolerantNumerics
import org.scalatest.FunSuite

class MetricTest extends FunSuite {
  implicit val floatEquality = TolerantNumerics.tolerantFloatEquality(0.01f)

  test("Test L2 distance 1 dimension") {
    val one = Embedding(Array(1.0f))
    val two = Embedding(Array(2.0f))
    val three = Embedding(Array(3.0f))
    assert(L2.distance(one, one).distance === L2Distance(0.0f).distance)
    assert(L2.distance(two, two).distance === L2Distance(0.0f).distance)
    assert(L2.distance(one, two).distance === L2Distance(1.0f).distance)
    assert(L2.distance(two, one).distance === L2Distance(1.0f).distance)
    assert(L2.distance(one, three).distance === L2Distance(2.0f).distance)
  }

  test("Test L2 distance 2 dimensions") {
    val origin = Embedding(Array(0.0f, 0.0f))
    val twoTwo = Embedding(Array(2.0f, 2.0f))
    val zeroThree = Embedding(Array(0.0f, 3.0f))
    assert(L2.distance(origin, origin).distance === L2Distance(0.0f).distance)
    assert(L2.distance(twoTwo, twoTwo).distance === L2Distance(0.0f).distance)
    assert(L2.distance(origin, twoTwo).distance === L2Distance(Math.sqrt(8.0).toFloat).distance)
    assert(L2.distance(twoTwo, origin).distance === L2Distance(Math.sqrt(8.0).toFloat).distance)
    assert(L2.distance(origin, zeroThree).distance === L2Distance(3.0f).distance)
    assert(L2.distance(twoTwo, zeroThree).distance === L2Distance(Math.sqrt(5.0).toFloat).distance)
  }

  test("L2 distance ordering") {
    val sortedL2Distances =
      Seq(L2Distance(0.0f), L2Distance(0.1f), L2Distance(1.0f), L2Distance(1000.0f))
    assert(sortedL2Distances === sortedL2Distances.sorted)
  }

  test("Test L2 distance with unequal dimesions") {
    val origin = Embedding(Array(0.0f, 0.0f))
    val two = Embedding(Array(2.0f))
    intercept[IllegalArgumentException](L2.distance(origin, two))
  }

  test("Test cosine distance 2 dimensions") {
    val eightSix = Embedding(Array(8.0f, 6.0f))
    val zeroThree = Embedding(Array(0.0f, 3.0f))
    val fourThree = Embedding(Array(4.0f, 3.0f)) // length is 5
    assert(Cosine.distance(eightSix, eightSix).distance === CosineDistance(0.0f).distance)
    assert(Cosine.distance(fourThree, fourThree).distance === CosineDistance(0.0f).distance)
    assert(Cosine.distance(eightSix, fourThree).distance === CosineDistance(0.0f).distance)
    assert(Cosine.distance(fourThree, eightSix).distance === CosineDistance(0.0f).distance)

    // 1 - (9 / (3 * 5)) = 6/15
    assert(Cosine.distance(zeroThree, fourThree).distance === CosineDistance(6.0f / 15.0f).distance)
  }

  test("Test cosine distance with unequal dimesions") {
    val oneOne = Embedding(Array(1.0f, 1.0f))
    val two = Embedding(Array(2.0f))
    intercept[IllegalArgumentException](Cosine.distance(oneOne, two))
  }

  test("Cosine distance ordering") {
    val sortedCosineDistances =
      Seq(CosineDistance(0.0f), CosineDistance(0.1f), CosineDistance(0.6f), CosineDistance(1.0f))
    assert(sortedCosineDistances === sortedCosineDistances.sorted)
  }

  test("Test inner product 2 dimensions") {
    val eightSix = Embedding(Array(8.0f, 6.0f))
    val zeroThree = Embedding(Array(0.0f, 3.0f))
    val fourThree = Embedding(Array(4.0f, 3.0f)) // length is 5
    assert(
      InnerProduct.distance(eightSix, eightSix).distance === InnerProductDistance(-99.0f).distance)
    assert(
      InnerProduct.distance(fourThree, fourThree).distance === InnerProductDistance(
        -24.0f).distance)
    assert(
      InnerProduct.distance(fourThree, eightSix).distance === InnerProductDistance(-49.0f).distance)
  }

  test("Test inner product with unequal dimesions") {
    val oneOne = Embedding(Array(1.0f, 1.0f))
    val two = Embedding(Array(2.0f))
    intercept[IllegalArgumentException](InnerProduct.distance(oneOne, two))
  }

  test("Inner product distance ordering") {
    val sortedInnerProductDistance =
      Seq(
        InnerProductDistance(0.0f),
        InnerProductDistance(0.1f),
        InnerProductDistance(0.6f),
        InnerProductDistance(1.0f))
    assert(sortedInnerProductDistance === sortedInnerProductDistance.sorted)
  }
}
