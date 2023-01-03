import com.twitter.ann.common._
import com.twitter.ann.common.thriftscala.{Distance => ServiceDistance}
import com.twitter.ann.debug_util.AnnThriftQueryServiceDebugUtil
import com.twitter.ann.featurestore.FeatureStoreEmbeddingProducer
import com.twitter.ann.hnsw.{HnswCommon, HnswParams}
import com.twitter.bijection.Injection
import com.twitter.cortex.ml.embeddings.common.EntityBytesInjection
import com.twitter.ml.api.embedding.Embedding
import com.twitter.ml.featurestore.catalog.datasets.embeddings.{
  ConsumerFollowEmbedding300Dataset,
  ConsumerNerEmbedding300Dataset,
  ProducerFollowEmbedding300Dataset
}
import com.twitter.ml.featurestore.catalog.entities.core.User
import com.twitter.ml.featurestore.catalog.features.embeddings.UserEmbeddings
import com.twitter.ml.featurestore.lib.entity.EntityWithId
import com.twitter.ml.featurestore.lib.{TweetId, UrlId, UserId}
import com.twitter.stitch.Stitch
import com.twitter.strato.client.Client
import com.twitter.strato.opcontext.Attribution.ManhattanAppId
import com.twitter.util.Await

/**
  Sample example for debugging ann recommendations in a local repl.
  In the repl, it can be used as following
  $ :load ann/src/main/scala/com/twitter/ann/debug_util/SampleExamples.scala
  $ SampleExamples.sampleServiceQueryClientWithIdWithFeatureStore(userId=12L)
 */
object SampleExamples {
  com.twitter.server.Init()

  val featureStoreAttributions = Seq(
    ManhattanAppId("athena", "cortex_follow_graph_user_embeddings"))

  // It builds a ann query service client to query ann index service which have news article url indexed.
  def sampleServiceQueryClient[D <: Distance[D]](
    service: String = "/srv#/staging/local/cortex-mlx/ann_news_article",
    embedding: Embedding[Float] = Embedding(Array.fill(300)(Math.random().toFloat)),
    neighbors: Int = 100,
    distanceInjection: Injection[_, ServiceDistance] = InnerProduct
  ) = {
    val runtimeParamInjection =
      HnswCommon.RuntimeParamsInjection // Scala runtime param to thrift runtime param injection
    val urlIdInjection = EntityBytesInjection.UrlIdInjection // Url id injection

    val client: Queryable[UrlId, HnswParams, D] =
      AnnThriftQueryServiceDebugUtil.buildServiceClient(
        service,
        runtimeParamInjection,
        distanceInjection.asInstanceOf[Injection[D, ServiceDistance]],
        urlIdInjection
      )

    // Query for 100 nearest news article url using random user embeddings
    val nearestNeighborUsers =
      client.queryWithDistance(embedding, neighbors, HnswParams(500))
    print(Await.result(nearestNeighborUsers))
  }

  // It queries an online feature store for producer embedding for an user based on dataset supplied.
  def sampleProducerEmbeddingQueryFromFeatureStore(
    userId: Long = 12L,
    datasetVersion: Long = 1547683275L
  ) = {
    val boundFeature = UserEmbeddings.ProducerFollow.bind(User)
    val producer = FeatureStoreEmbeddingProducer(
      ProducerFollowEmbedding300Dataset,
      version = datasetVersion,
      boundFeature,
      Client(),
      featureStoreAttributions = featureStoreAttributions
    )

    val someUserId = User.withId(UserId(userId))
    val embedding = Await.result(Stitch.run(producer.produceEmbedding(someUserId)))
    print(embedding.get)
  }

  // It queries an online feature store for consumer embedding for an user based on dataset supplied.
  def sampleConsumerEmbeddingQueryFromFeatureStore(
    userId: Long = 12L,
    datasetVersion: Long = 1551571282L
  ) = {
    val boundFeature = UserEmbeddings.ConsumerFollow.bind(User)
    val producer = FeatureStoreEmbeddingProducer(
      ConsumerFollowEmbedding300Dataset,
      version = datasetVersion,
      boundFeature,
      Client(),
      featureStoreAttributions = featureStoreAttributions
    )

    val someUserId = User.withId(UserId(userId))
    val embedding = Await.result(Stitch.run(producer.produceEmbedding(someUserId)))
    print(embedding.get)
  }

  // It builds a ann query service client which queries user embedding from feature store and then use it to query for its nearest tweets from an ANN index.
  def sampleServiceQueryClientWithIdWithFeatureStore[D <: Distance[D]](
    userId: Long = 12L,
    neighbors: Int = 100,
    datasetVersion: Long = 1547683275L,
    service: String = "/srv#/staging/local/apoorvs/ann-server-without-filter",
    distanceInjection: Injection[_, ServiceDistance] = InnerProduct
  ) = {
    val runtimeParamInjection =
      HnswCommon.RuntimeParamsInjection // Scala runtime param to thrift runtime param injection
    val tweetIdInjection = EntityBytesInjection.TweetIdInjection // Tweet id injection

    // Create user embedding producer backed by feature store
    val boundFeature = UserEmbeddings.ConsumerFollow.bind(User)
    val dataset = ConsumerFollowEmbedding300Dataset

    val queryClient: QueryableById[EntityWithId[UserId], TweetId, HnswParams, D] =
      AnnThriftQueryServiceDebugUtil.buildServiceClientQueryByIdWithFeatureStore(
        service,
        dataset,
        datasetVersion,
        boundFeature,
        runtimeParamInjection,
        distanceInjection.asInstanceOf[Injection[D, ServiceDistance]],
        tweetIdInjection
      )

    val someUserId = User.withId(UserId(userId))
    // Query for nearest 100 tweets for user
    val nearestNeighborTweets =
      queryClient.queryByIdWithDistance(someUserId, neighbors, HnswParams(500))
    print(Await.result(Stitch.run(nearestNeighborTweets)))
  }

  // It builds a ann query service client which queries consumer embedding from feature store and then use it to query for its nearest producers from an ANN index.
  def sampleServiceQueryClientWithIdWithFeatureStore_1[D <: Distance[D]](
    userId: Long = 12L,
    neighbors: Int = 100,
    datasetVersion: Long = 1547683275L,
    service: String = "/srv#/prod/local/cortex/ann-server-producer-cosine",
    distanceInjection: Injection[_, ServiceDistance] = Cosine
  ) = {
    val runtimeParamInjection =
      HnswCommon.RuntimeParamsInjection // Scala runtime param to thrift runtime param injection
    val producerIdInjection = EntityBytesInjection.UserIdInjection // Tweet id injection

    // Create user embedding producer backed by feature store
    val boundFeature = UserEmbeddings.ConsumerFollow.bind(User)
    val dataset = ConsumerFollowEmbedding300Dataset

    val queryClient: QueryableById[EntityWithId[UserId], UserId, HnswParams, D] =
      AnnThriftQueryServiceDebugUtil.buildServiceClientQueryByIdWithFeatureStore(
        service,
        dataset,
        datasetVersion,
        boundFeature,
        runtimeParamInjection,
        distanceInjection.asInstanceOf[Injection[D, ServiceDistance]],
        producerIdInjection
      )

    val someUserId = User.withId(UserId(userId))
    // Query for nearest 100 tweets for user
    val nearestNeighbors =
      queryClient.queryByIdWithDistance(someUserId, neighbors, HnswParams(500))
    print(Await.result(Stitch.run(nearestNeighbors)))
  }

  /**
   * Builds a ann query service client which queries ConsumerNer embedding from feature store and then use
   * it to query for its nearest URLs from an ANN index.
   */
  def sampleServiceQueryClientWithIdWithFeatureStoreForUrl[D <: Distance[D]](
    userId: Long = 12L,
    neighbors: Int = 100,
    datasetVersion: Long = 1549909202L,
    service: String = "/s/news-article-recs/ann_news_article",
    distanceInjection: Injection[_, ServiceDistance] = InnerProduct
  ) = {
    val runtimeParamInjection =
      HnswCommon.RuntimeParamsInjection // Scala runtime param to thrift runtime param injection
    val urlIdInjection = EntityBytesInjection.UrlIdInjection

    // Create user embedding producer backed by feature store
    val boundFeature = UserEmbeddings.ConsumerNer.bind(User)
    val dataset = ConsumerNerEmbedding300Dataset

    val queryClient: QueryableById[EntityWithId[UserId], UrlId, HnswParams, D] =
      AnnThriftQueryServiceDebugUtil.buildServiceClientQueryByIdWithFeatureStore(
        service,
        dataset,
        datasetVersion,
        boundFeature,
        runtimeParamInjection,
        distanceInjection.asInstanceOf[Injection[D, ServiceDistance]],
        urlIdInjection
      )

    val someUserId = User.withId(UserId(userId))
    // Query for nearest 100 URLs for user
    val nearestNeighbors =
      queryClient.queryByIdWithDistance(someUserId, neighbors, HnswParams(500))
    print(Await.result(Stitch.run(nearestNeighbors)))
  }
}
