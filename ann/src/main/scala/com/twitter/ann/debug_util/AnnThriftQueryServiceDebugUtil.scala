package com.twitter.ann.debug_util

import com.twitter.ann.common.thriftscala.{
  AnnQueryService,
  NearestNeighborQuery,
  NearestNeighborResult,
  Distance => ServiceDistance,
  RuntimeParams => ServiceRuntimeParams
}
import com.twitter.ann.common._
import com.twitter.ann.featurestore.FeatureStoreEmbeddingProducer
import com.twitter.bijection.Injection
import com.twitter.finagle.{Service, ThriftMux}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.ml.api.thriftscala.Embedding
import com.twitter.ml.featurestore.lib.{EntityId, RawFloatTensor}
import com.twitter.ml.featurestore.lib.dataset.online.VersionedOnlineAccessDataset
import com.twitter.ml.featurestore.lib.entity.EntityWithId
import com.twitter.ml.featurestore.lib.feature.BoundFeature
import com.twitter.strato.client.Client
import com.twitter.strato.opcontext.Attribution.ManhattanAppId
import com.twitter.util.Future

/**
 * This class contains sample code to query an ANN service with embedding or query an ann service with embedding available in Manhattan.
 * DO NOT USE THIS IN PRODUCTION CODE
 */
object AnnThriftQueryServiceDebugUtil {

  /**
   * Builds a client to query a ann service
   * @param destination service wily destination
   * @param runtimeParamInjection runtime param injection for algorithm
   * @param distanceInjection distance injection
   * @param indexIdInjection id injection for the type of entity indexed in ANN
   * @param clientId caller client id
   * @tparam T type of entity indexed in ANN
   * @tparam P type of runtime parameter
   * @tparam D type of distance metric
   */
  def buildServiceClient[T, P <: RuntimeParams, D <: Distance[D]](
    destination: String,
    runtimeParamInjection: Injection[P, ServiceRuntimeParams],
    distanceInjection: Injection[D, ServiceDistance],
    indexIdInjection: Injection[T, Array[Byte]],
    clientId: String = "client_id"
  ): Queryable[T, P, D] = {
    val statsReceiver = NullStatsReceiver
    val client: AnnQueryService.MethodPerEndpoint = new AnnQueryService.FinagledClient(
      service = ClientBuilder()
        .reportTo(statsReceiver)
        .dest(destination)
        .retryPolicy(RetryPolicy.tries(3, { case _ => true }))
        .stack(ThriftMux.client.withClientId(ClientId(clientId)))
        .build(),
      stats = statsReceiver
    )

    val service = new Service[NearestNeighborQuery, NearestNeighborResult] {
      override def apply(request: NearestNeighborQuery): Future[NearestNeighborResult] =
        client.query(request)
    }

    new ServiceClientQueryable[T, P, D](
      service,
      runtimeParamInjection,
      distanceInjection,
      indexIdInjection
    )
  }

  /**
   * Builds a client to query an ANN service with an entity id whose embedding is fetched from feature store.
   * @param serviceDestination service wily destination
   * @param featureStoreDatasetId feature store dataset id
   * @param featureStoreDatasetVersion feature store dataset version
   * @param boundFeature bound feature
   * @param runtimeParamInjection runtime param injection for algorithm
   * @param distanceInjection distance injection
   * @param indexIdInjection id injection for the type of entity indexed in ANN
   * @param clientId caller client id
   * @tparam T1 type of entity id
   * @tparam T2 type of id indexed in an ANN service
   * @tparam P type of runtime parameter
   * @tparam D type of distance metric
   */
  def buildServiceClientQueryByIdWithFeatureStore[
    T1 <: EntityId,
    T2,
    P <: RuntimeParams,
    D <: Distance[D]
  ](
    serviceDestination: String,
    featureStoreDataset: VersionedOnlineAccessDataset[T1, Embedding],
    featureStoreDatasetVersion: Long,
    boundFeature: BoundFeature[T1, RawFloatTensor],
    runtimeParamInjection: Injection[P, ServiceRuntimeParams],
    distanceInjection: Injection[D, ServiceDistance],
    indexIdInjection: Injection[T2, Array[Byte]],
    clientId: String = "client_id"
  ): QueryableById[EntityWithId[T1], T2, P, D] = {
    val serviceClient = AnnThriftQueryServiceDebugUtil.buildServiceClient(
      serviceDestination,
      runtimeParamInjection,
      distanceInjection,
      indexIdInjection,
      clientId
    )

    val featureStoreAttributions = Seq(
      ManhattanAppId("athena", "cortex_follow_graph_user_embeddings"))
    val embeddingProducer = FeatureStoreEmbeddingProducer(
      featureStoreDataset,
      featureStoreDatasetVersion,
      boundFeature,
      Client(),
      featureStoreAttributions = featureStoreAttributions
    )

    val queryClient = new QueryableByIdImplementation(
      embeddingProducer,
      serviceClient
    )

    queryClient
  }
}
