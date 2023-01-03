package com.twitter.ann.service.query_server.hnsw

import com.google.inject.Stage
import com.twitter.ann.common.{Cosine, InnerProduct, L2}
import com.twitter.finatra.mtls.thriftmux.EmbeddedMtlsThriftServer
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.search.common.file.{AbstractFile, LocalFile}

class NonRefreshableIndexFeatureTest extends HnswQueryServerFeatureTestBase {

  val timestamp = "1514764800"

  override def server(metricString: String, indexDir: AbstractFile): EmbeddedThriftServer = {
    new EmbeddedMtlsThriftServer(
      twitterServer = new HnswQueryableServer,
      stage = Stage.DEVELOPMENT,
      flags = Map(
        "com.twitter.server.wilyns.disable" -> "true",
        "dtab.add" -> "/$/inet => /$/nil; /zk => /$/nil",
        "decider.base" -> "hnsw_query_server_decider.yml",
        "index_directory" -> indexDir.asInstanceOf[LocalFile].getPath,
        "metric" -> metricString,
        "id_type" -> IndexGenerator.idType,
        "dimension" -> IndexGenerator.dimen.toString,
        "environment" -> "devel",
        "threads" -> "1",
        // set refreshable flag
        "refreshable" -> "false"
      )
    )
  }

  // point index directory to "service/query_server/hnsw/$indexDirector/$timestamp" with
  // the timestamp suffix, a non refreshable index should start without exception
  test("Hnsw query server inner product e2e loading and querying") {
    run(InnerProduct, "InnerProduct", s"hnsw_inner_product/$timestamp", "knn_inner_product.txt")
  }

  test("Hnsw query server L2 e2e loading and querying") {
    run(L2, "L2", s"hnsw_l2/$timestamp", "knn_l2.txt")
  }

  test("Hnsw query server cosine e2e loading and querying") {
    run(Cosine, "Cosine", s"hnsw_cosine/$timestamp", "knn_cosine.txt")
  }
}
