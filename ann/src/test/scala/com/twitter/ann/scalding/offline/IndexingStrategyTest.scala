package com.twitter.ann.scalding.offline

import com.twitter.ann.common.{Cosine, CosineDistance}
import com.twitter.ann.hnsw.HnswParams
import com.twitter.scalding.{Args, ArgsException}
import org.scalatest.FunSuite

class IndexingStrategyTest extends FunSuite {
  test("parse brute_force") {
    val args = Args("--indexing_strategy.type brute_force --indexing_strategy.metric Cosine")
    val actual = IndexingStrategy.parse(args).asInstanceOf[IndexingStrategy[CosineDistance]]
    assert(actual === BruteForceIndexingStrategy(Cosine))
  }

  test("parse missing type") {
    val args = Args("--indexing_strategy.metric Cosine")
    assertThrows[ArgsException] {
      IndexingStrategy.parse(args).asInstanceOf[IndexingStrategy[CosineDistance]]
    }
  }

  test("parse missing metric") {
    val args = Args("--indexing_strategy.type brute_force")
    assertThrows[ArgsException] {
      IndexingStrategy.parse(args).asInstanceOf[IndexingStrategy[CosineDistance]]
    }
  }

  test("parse hnsw") {
    val args = Args(
      "--indexing_strategy.type hnsw " +
        "--indexing_strategy.metric Cosine " +
        "--indexing_strategy.dimension 300 " +
        "--indexing_strategy.ef_construction 20 " +
        "--indexing_strategy.max_m 30 " +
        "--indexing_strategy.ef_query 40"
    )
    val actual = IndexingStrategy.parse(args).asInstanceOf[IndexingStrategy[CosineDistance]]
    assert(
      actual === HnswIndexingStrategy(
        dimension = 300,
        metric = Cosine,
        efConstruction = 20,
        maxM = 30,
        hnswParams = HnswParams(40)
      ))
  }

  test("parse missing dimension") {
    val args = Args(
      "--indexing_strategy.type hnsw " +
        "--indexing_strategy.metric Cosine " +
        "--indexing_strategy.ef_construction 20 " +
        "--indexing_strategy.max_m 30 " +
        "--indexing_strategy.ef_query 40"
    )
    assertThrows[ArgsException] {
      IndexingStrategy.parse(args).asInstanceOf[IndexingStrategy[CosineDistance]]
    }
  }

  test("parse hnsw missing ef_construction") {
    val args = Args(
      "--indexing_strategy.type hnsw " +
        "--indexing_strategy.metric Cosine " +
        "--indexing_strategy.dimension 300 " +
        "--indexing_strategy.max_m 30 " +
        "--indexing_strategy.ef_query 40"
    )
    assertThrows[ArgsException] {
      IndexingStrategy.parse(args).asInstanceOf[IndexingStrategy[CosineDistance]]
    }
  }

  test("parse hnsw missing max_m") {
    val args = Args(
      "--indexing_strategy.type hnsw " +
        "--indexing_strategy.metric Cosine " +
        "--indexing_strategy.dimension 300 " +
        "--indexing_strategy.ef_construction 20 " +
        "--indexing_strategy.ef_query 40"
    )
    assertThrows[ArgsException] {
      IndexingStrategy.parse(args).asInstanceOf[IndexingStrategy[CosineDistance]]
    }
  }

  test("parse hnsw missing ef_query") {
    val args = Args(
      "--indexing_strategy.type hnsw " +
        "--indexing_strategy.metric Cosine " +
        "--indexing_strategy.dimension 300 " +
        "--indexing_strategy.ef_construction 20 " +
        "--indexing_strategy.max_m 30 "
    )
    assertThrows[ArgsException] {
      IndexingStrategy.parse(args).asInstanceOf[IndexingStrategy[CosineDistance]]
    }
  }
}
