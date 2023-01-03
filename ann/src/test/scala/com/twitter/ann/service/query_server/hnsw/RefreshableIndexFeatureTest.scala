package com.twitter.ann.service.query_server.hnsw

import com.google.inject.Stage
import com.twitter.ann.common.{Cosine, InnerProduct, L2}
import com.twitter.finatra.mtls.thriftmux.EmbeddedMtlsThriftServer
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.search.common.file.{AbstractFile, LocalFile}

class RefreshableIndexFeatureTest extends HnswQueryServerFeatureTestBase {

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
        "refreshable" -> "true"
      )
    )
  }

  // point index directory to "service/query_server/hnsw/$indexDirector" without
  // the timestamp suffix, a refreshable index should pick up timestamp subdirectory
  test("Hnsw query server inner product e2e loading and querying") {
    run(InnerProduct, "InnerProduct", "hnsw_inner_product", "knn_inner_product.txt")
  }

  test("Hnsw query server L2 e2e loading and querying") {
    run(L2, "L2", "hnsw_l2", "knn_l2.txt")
  }

  test("Hnsw query server cosine e2e loading and querying") {
    run(Cosine, "Cosine", "hnsw_cosine", "knn_cosine.txt")
  }
}
