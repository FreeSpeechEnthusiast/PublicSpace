.. _api:

ANN APIs
==========

Summarizing the generic scala APIs for ANN Index `Building and Querying <https://cgit.twitter.biz/source/tree/ann/src/main/scala/com/twitter/ann/common/Api.scala>`_ and `Serialization <https://cgit.twitter.biz/source/tree/ann/src/main/scala/com/twitter/ann/common/Serialization.scala>`_.


Algorithms supported
---------------------

We provide different implementations of Query API shown below. Note that Faiss is a collection of algorithms while other libraries are single algorithm.

* Internal, JVM based implementation of HNSW :ref:`hnsw_lib`
* Opensource Faiss C++ library :ref:`faiss_lib`
* Opensource Annoy C++ library :ref:`annoy_lib`

Implementation and Open Source Strategy
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
We only have a relatively small set of ANN libraries and algorithms implemented at this time. We have selected these based on ease of integration along with the features they provide. We will likely want to support new algorithms and software packages in the ANN library in the future. Here are some of the factors that would help us to decide how to improve the software in the future. 

* Customer Requirements

  * Recall/Latency tradeoff

  * Concurrency support

  * Memory efficiency

  * Support for additions

  * Support for updates

  * Scaling capabilities

* Technical Considerations

  * Ease of integration

  * Open source support
  
  * Test Coverage

  * Proven use in other production systems


Embeddings API
---------------
.. code-block:: scala

  type EmbeddingVector = Embedding[Float] // Usage Embedding(Array(1.0f, 2.0f, 3.0f))
  
  /**
  * Typed entity with an embedding associated with it.
  * @param id : Unique Id for an entity.
  * @param embedding : Embedding/Vector of an entity.
  * @tparam T: Type of id.
  */
  case class EntityEmbedding[T](id: T, embedding: EmbeddingVector)


Query API
----------
.. code-block:: scala

  trait Queryable[T, P <: RuntimeParams, D <: Distance[D]] {

    /**
      * ANN query for ids.
      * @param embedding: Embedding/Vector to be queried with.
      * @param numOfNeighbors: Number of neighbours to be queried for.
      * @param runtimeParams: Runtime params associated with index to control accuracy/latency etc.
      * @return List of approximate nearest neighbour ids.
      */
    def query(
        embedding: EmbeddingVector,
        numOfNeighbors: Int,
        runtimeParams: P
    ): Future[List[T]]

    /**
      * ANN query for ids with distance.
      * @param embedding: Embedding/Vector to be queried with.
      * @param numOfNeighbors: Number of neighbours to be queried for.
      * @param runtimeParams: Runtime params associated with index to control accuracy/latency etc.
      * @return List of approximate nearest neighbour ids with distance from the query embedding.
      */
    def queryWithDistance(
        embedding: EmbeddingVector,
        numOfNeighbors: Int,
        runtimeParams: P
    ): Future[List[NeighborWithDistance[T, D]]]
  }