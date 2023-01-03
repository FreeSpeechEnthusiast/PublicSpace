.. _query_service_client:

Query Service Client
---------------------
For querying the deployed service you can use `ServiceClientQueryable`_.


Usage:

* Include the BUILD dependency of the algorithm. ex: For HNSW (`ann/src/main/scala/com/twitter/ann/hnsw`)

* Sample usage: 

.. code-block:: scala

  import com.twitter.ann.common.{AnnInjections, InnerProduct, InnerProductDistance, Queryable, ServiceClientQueryable}
  import com.twitter.ann.common.thriftscala.{AnnQueryService, NearestNeighborQuery, NearestNeighborResult}
  import com.twitter.ann.hnsw.{HnswCommon, HnswParams}
  import com.twitter.finagle.{Service, ThriftMux}
  import com.twitter.finagle.builder.ClientBuilder
  import com.twitter.finagle.service.RetryPolicy
  import com.twitter.finagle.stats.StatsReceiver
  import com.twitter.finagle.thrift.ClientId
  import com.twitter.util.Future

  class TestClientBuilder {
    // It builds a query service client with inner product distance for long entity id with hsnw algo
    def buildClient(
      statsReceiver: StatsReceiver,
      serviceDestination: String, // ANN query service wily destination 
      clientId: String // Your client id
    ) : Queryable[Long, HnswParams, InnerProductDistance] = {
      val metric = InnerProduct // Distance metric
      val distanceInjection = metric // Scala Distance to thrift distance injection
      val runtimeParamInjection = HnswCommon.RuntimeParamsInjection // Scala runtime param to thrift runtime param injection
      val idInjection = AnnInjections.LongInjection // Typed id to array of bytes injection

      val client: AnnQueryService.MethodPerEndpoint = new AnnQueryService.FinagledClient(
        service = ClientBuilder()
          .reportTo(statsReceiver)
          .dest(serviceDestination)
          .retryPolicy(RetryPolicy.tries(3, { case _ => true }))
          .stack(ThriftMux.client.withClientId(ClientId(clientId)))
          .build(),
        stats = statsReceiver
      )

      new ServiceClientQueryable[Long, HnswParams, InnerProductDistance](
          client,
          runtimeParamInjection,
          distanceInjection,
          idInjection
        )
    }
  }

.. warning:: There are no compile time checks for the client with respect to the configuration of deployed query server. So make sure to get the arguments right in the client with respect to service.


.. _ServiceClientQueryable: https://sourcegraph.twitter.biz/git.twitter.biz/source/-/blob/ann/src/main/scala/com/twitter/ann/common/ServiceClientQueryable.scala
