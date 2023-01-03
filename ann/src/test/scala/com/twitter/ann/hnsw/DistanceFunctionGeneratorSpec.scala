package com.twitter.ann.hnsw

import com.twitter.ann.common._
import com.twitter.ml.api.embedding.Embedding
import org.junit.runner.RunWith
import org.scalactic.TolerantNumerics
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class DistanceFunctionGeneratorSpec extends FunSuite with MockitoSugar {
  implicit val floatEquality = TolerantNumerics.tolerantFloatEquality(0.01f)
  val entity1 = EntityEmbedding[Int](1, Embedding(Array(3.0f)))

  test(
    "DistanceFunctionGenerator use InnerProduct distance when Cosine specified"
  ) {
    val distFn = DistanceFunctionGenerator[Int, CosineDistance](
      Cosine,
      (id) => entity1.embedding
    )
    val indexFn = distFn.index
    val queryFn = distFn.query

    assert(
      indexFn.distance(1, 1) === InnerProduct.absoluteDistance(
        entity1.embedding,
        entity1.embedding
      )
    )
    assert(
      queryFn.distance(Embedding(Array(4.0f)), 1) === InnerProduct.absoluteDistance(
        Embedding(Array(4.0f)),
        entity1.embedding
      )
    )
  }

  test("DistanceFunctionGenerator use L2 distance when L2 specified") {
    val distFn = DistanceFunctionGenerator[Int, L2Distance](
      L2,
      (id) => entity1.embedding
    )
    val indexFn = distFn.index
    val queryFn = distFn.query

    assert(
      indexFn.distance(1, 1) === L2.absoluteDistance(
        entity1.embedding,
        entity1.embedding
      )
    )
    assert(
      queryFn.distance(Embedding(Array(4.0f)), 1) === L2.absoluteDistance(
        Embedding(Array(4.0f)),
        entity1.embedding
      )
    )
  }

  test(
    "DistanceFunctionGenerator use InnerProduct distance when InnerProduct specified"
  ) {
    val distFn = DistanceFunctionGenerator[Int, InnerProductDistance](
      InnerProduct,
      (id) => entity1.embedding
    )
    val indexFn = distFn.index
    val queryFn = distFn.query

    assert(
      indexFn.distance(1, 1) === InnerProduct.absoluteDistance(
        entity1.embedding,
        entity1.embedding
      )
    )
    assert(
      queryFn.distance(Embedding(Array(4.0f)), 1) === InnerProduct.absoluteDistance(
        Embedding(Array(4.0f)),
        entity1.embedding
      )
    )
  }
}
