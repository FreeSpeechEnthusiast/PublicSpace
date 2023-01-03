.. _hnsw_lib:

HNSW ANN Library
=================

Hierarchical Navigable Small World graphs `HNSW <https://arxiv.org/abs/1603.09320>`_ is a state-of-the-art technique for
performing approximate nearest neighbour search based on navigable small world graphs with controllable hierarchy.
Hierarchical NSW incrementally builds a multi-layer structure consisting from hierarchical
set of proximity graphs (layers) for nested subsets of the stored elements.

Performance evaluation has demonstrated that the proposed general metric space search index is able
to strongly outperform previous opensource state-of-the-art vector-only approaches on various
datasets on different benchmarks (Recall/QPS/Latency/Storage).

`HNSW public benchmarks <https://erikbern.com/2018/06/17/new-approximate-nearest-neighbor-benchmarks.html>`_ on public dataset.

**Some offline benchmark performance on internal Twitter datasets** `Link <https://docs.google.com/document/d/1Hjy9ta2lTFtU24_itAoeywG6c9uTM9w8cz8JPGzPuBE/edit?ts=5bc604de#heading=h.wm6hifeym5ig>`_

Summary
-------

* Works well for high as well low-dimensional data.
* Works well for highly clustered data or sparsely distributed data.
* Index creation, update and querying can be done in memory or index can be serialized and loaded back to perform querying.
* Supports index serving in-memory or mmaped mode.
* Both index creation and index loading (non-mmapped version) are RAM bound.
* Both index creation and index lookup are highly CPU intensive.
* Can provide really high recall (~0.9+) within few ms (actual numbers vary and depends on dataset/distance metric/build and runtime params).
* Supports concurrent ANN queries/new element insertions/element updates.
* Concurrent writes highly speed up the index building. So we recommend using multiple threads alongside multi-core machines to build the index.

.. note::
  We do not recommend using memory-mapped mode for production use cases. Though it can be used in scalding, consult us once before using this mode.

Distance metric supported
--------------------------

* Inner Product/Dot Product
* Cosine
* L2

The method potentially can work with any distances (including non-symmetric ones).
Contact us if you need other distances.

API
---

* `HNSW API <https://cgit.twitter.biz/source/tree/ann/src/main/scala/com/twitter/ann/hnsw/TypedHnswIndex.scala>`_
* BUILD dependency (`ann/src/main/scala/com/twitter/ann/hnsw`)
     
Index Building/Querying/Serialization
-------------------------------------

Index can be built/queried in memory, and then can be serialized to disk (hdfs/local directory). Concurrent writes speeds up the index building. This process is CPU intensive and is bounded on RAM.

.. code-block:: scala

    import com.twitter.ann.hnsw.{TypedHnswIndex}
    import com.twitter.ann.common.{AnnInjections, InnerProduct, InnerProductDistance, EntityEmbedding}
    import com.twitter.ml.api.embedding.Embedding
    import com.twitter.search.common.file.FileUtils
    import com.twitter.util.{Await, FuturePool}
    import java.util.concurrent.Executors

    val embeddingDimension = 3
    val distanceMetric = InnerProduct
    val efConstruction = 200 // Bigger this number, better the quality and more the time to build
    val maxM = 16 // Bigger this number, better the quality and more the size of index
    val expectedElements = 2 // Approximate number of elements to be indexed.
    val concurrency = 30
    val ef = 10
    val numOfNeighbours = 1
    val exec = Executors.newFixedThreadPool(concurrency)
    val futurePool = ReadWriteFuturePool(FuturePool.apply(exec))
    val index = TypedHnswIndex.serializableIndex[Long, InnerProductDistance](
      embeddingDimension,
      distanceMetric,
      efConstruction,
      maxM,
      expectedElements,
      AnnInjections.LongInjection,
      futurePool
    )

    // Add entity to index
    val entity1 = EntityEmbedding(123L, Embedding(Array(1.0f, 2.0f, 3.0f)))
    val entity2 = EntityEmbedding(345L, Embedding(Array(2.0f, 3.0f, 4.0f)))
    Await.result(index.append(entity1))
    Await.result(index.append(entity2))

    //Query: Bigger 'ef' leads to better recall for the query at the expense of higher latency.
    val runtimeParams = HnswParams(ef)
    val neighbours = Await.result(
      index.queryWithDistance(
        Embedding(Array(1.0f, 2.0f, 7.0f)),
        numOfNeighbours,
        runtimeParams
      )
    )

    // Add new entity
    val entity3 = EntityEmbedding(123L, Embedding(Array(1.0f, 2.0f, 3.0f)))
    Await.result(index.append(entity3))

    //Serialize: Serialization directory can be local/hdfs directory
    val serializationDirectory = FileUtils.getFileHandle("hdfs:///user/something/index_directory")
    indexBuilder.toDirectory(serializationDirectory)

    exec.shutdown()


Querying from Serialized Index
-------------------------------

To perform ANN Queries index can be loaded back into memory (bounded by RAM) from disk or it can be memory mapped (query latency performance will degrade, so we recommend using in memory version for loading and using mmaped for extreme use cases).
Querying can be done concurrently and is CPU intensive.

.. code-block:: scala

    import com.twitter.ann.hnsw.{HnswParams, TypedHnswIndex}
    import com.twitter.ann.common.{AnnInjections, InnerProduct, InnerProductDistance}
    import com.twitter.ml.api.embedding.Embedding
    import com.twitter.search.common.file.FileUtils
    import com.twitter.util.{Await, FuturePool}
    import java.util.concurrent.Executors

    val embeddingDimension = 3
    val distanceMetric = InnerProduct
    val numOfNeighbours = 10
    val concurrency = 30
    val exec = Executors.newFixedThreadPool(concurrency)
    val futurePool = ReadWriteFuturePool(FuturePool.apply(exec))
    val serializationDirectory = FileUtils.getFileHandle("hdfs:///user/something/index_directory")
    val ef = 100

    // Bigger this number better the recall for the query on the expense of higher latency.
    val runtimeParams = HnswParams(ef)

    // Load index in memory for querying
    val queryableInMemory = TypedHnswIndex.loadIndex[Long, InnerProductDistance](
      embeddingDimension,
      distanceMetric,
      AnnInjections.LongInjection,
      futurePool,
      serializationDirectory
    )

    val neighbours = Await.result(
      queryableInMemory.queryWithDistance(
        Embedding(Array(1.0f, 2.0f, 6.0f)),
        numOfNeighbours,
        runtimeParams
      )
    )

    // Memory mapped Index Loading for querying.
    val queryableMemoryMapped = TypedHnswIndex.loadMMappedIndex[Long, InnerProductDistance](
      embeddingDimension,
      distanceMetric,
      AnnInjections.LongInjection,
      futurePool,
      serializationDirectory
    )

    val neighboursAfter = Await.result(
      queryableMemoryMapped.queryWithDistance(
        Embedding(Array(1.0f, 2.0f, 6.0f)),
        numOfNeighbours,
        runtimeParams
      )
    )

    exec.shutdown()


.. _hnsw_lib_in_memory:

In-memory Index Building and Querying
--------------------------------------

Index can be built in-memory and then can be used for querying without serialization.

.. code-block:: scala

    import java.util.concurrent.Executors

    import com.twitter.ann.hnsw.{HnswParams, TypedHnswIndex}
    import com.twitter.ann.common.{AnnInjections, InnerProduct, InnerProductDistance, EntityEmbedding}
    import com.twitter.ml.api.embedding.Embedding
    import com.twitter.util.{Await, Future, FuturePool}

    val embeddingDimension = 3
    val distanceMetric = InnerProduct
    val efConstruction = 200 // Bigger this number, better the quality and more the time to build
    val maxM = 16 // Bigger this number, better the quality and more the size of index
    val expectedElements = 2 // Approximate number of elements to be indexed.
    val concurrency = 30
    val ef = 10
    val numOfNeighbours = 1


    val exec = Executors.newFixedThreadPool(concurrency)
    val futurePool = ReadWriteFuturePool(FuturePool.apply(exec))
    val index = TypedHnswIndex.index[Long, InnerProductDistance](
      embeddingDimension,
      distanceMetric,
      efConstruction,
      maxM,
      expectedElements,
      futurePool
    )

    // Add entity to index
    val entity1 = EntityEmbedding(123L, Embedding(Array(1.0f, 2.0f, 3.0f)))
    val entity2 = EntityEmbedding(345L, Embedding(Array(2.0f, 3.0f, 4.0f)))
    Await.result(index.append(entity1))
    Await.result(index.append(entity2))

    // Bigger this number better the recall for the query on the expense of higher latency.
    val runtimeParams = HnswParams(ef)
    val neighbours = Await.result(
      index.queryWithDistance(
        Embedding(Array(1.0f, 2.0f, 6.0f)),
        numOfNeighbours,
        runtimeParams
      )
    )

    exec.shutdown()

Element updates
--------------------------------------

Index can be built and updated in memory.

.. code-block:: scala

    import com.twitter.ann.common.{AnnInjections, EntityEmbedding, InnerProduct, InnerProductDistance}
    import com.twitter.ann.hnsw.HnswParams
    import com.twitter.ml.api.embedding.Embedding
    import com.twitter.util.{Await, FuturePool}
    import com.twitter.ann.common.ReadWriteFuturePool
    import com.twitter.ann.hnsw.TypedHnswIndex
    import java.util.concurrent.Executors

    val embeddingDimension = 3
    val distanceMetric = InnerProduct
    val efConstruction = 200 // Bigger this number, better the quality and more the time to build
    val maxM = 16 // Bigger this number, better the quality and more the size of index
    val expectedElements = 2 // Approximate number of elements to be indexed.
    val concurrency = 30
    val ef = 100
    val numOfNeighbours = 1

    val exec = Executors.newFixedThreadPool(concurrency)
    val pool = ReadWriteFuturePool(FuturePool.apply(exec))

    val index =
      TypedHnswIndex.serializableIndex[Long, InnerProductDistance](
        embeddingDimension,
        distanceMetric,
        efConstruction = efConstruction,
        maxM = maxM,
        expectedElements,
        AnnInjections.LongInjection,
        pool
      )


    // Add entity to index
    val entity1 = EntityEmbedding(123L, Embedding(Array(-1.0f, -2.0f, -6.0f)))
    val entity2 = EntityEmbedding(345L, Embedding(Array(0.0f, 0.0f, 0.0f)))
    Await.result(index.append(entity1))
    Await.result(index.append(entity2))

    // Update index elements
    val entity1_new = EntityEmbedding(123L, Embedding(Array(1.0f, 2.0f, 6.0f)))
    val entity2_new = EntityEmbedding(345L, Embedding(Array(2.0f, 3.0f, 4.0f)))
    Await.result(index.update(entity1_new))
    Await.result(index.update(entity2_new))


    // Bigger this number better the recall for the query on the expense of higher latency.
    val runtimeParams = HnswParams(ef)
    val neighbours = Await.result(
      index.queryWithDistance(
        Embedding(Array(1.0f, 2.0f, 6.0f)),
        numOfNeighbours,
        runtimeParams
      )
    )

    println(neighbours) // Output: List(NeighborWithDistance(123,InnerProductDistance(-40.0)))

    exec.shutdown()


Hyper params usage
-------------------

* **Build params**

  - `maxM`: Provided during build time and it is the number of bi-directional links created for every new element during insertion. Reasonable range for `M` is 2-100. The range of `M` 12-48 is ok for the most of the use cases. Higher `M` work better on datasets with high intrinsic dimensionality and/or high recall, while low `M` work better for datasets with low intrinsic dimensionality and/or low recalls. The parameter also determines the algorithm's memory consumption, larger values of `M` lead to higher memory consumption. For high-dimensional datasets (word embeddings, good face descriptors), higher `M` is required (e.g. `M` = 48, 64) for optimal performance at high recall.

  - `efConstruction`: Provided during build time and controls the index build time/index accuracy. Bigger `efConstruction` leads to longer construction, but better index quality. At some point, increasing `efConstruction` does not improve the quality of the index. One way to check if the selection of `efConstruction` is ok is to measure the recall for `M` nearest neighbor search when `ef` (During querying) = `ef_constuction`: if the recall is lower than `0.9`, than there is room for improvement. Empirically starting value of `200` is good and should be increase/decrease according to your product needs.

* **Search params**

  - `HnswParams(ef)`: Provided during runtime and affects the search performance. Higher `ef` leads to more accurate but slower search. `ef` cannot be set lower than the number of nearest neighbors to be queried. The value `ef` of can be anything between number of neighbours requested and the size of the dataset.


Storage Requirements
---------------------

* **In Memory Index**

  - We recommend estimating rough memory requirements for in-memory index creation/serving based on this: Embedding Dimension is D, Total elements is N, Entity type is T with size T_SIZE (4 bytes for integer, 8 bytes for long, 2 bytes per character for String: You estimate using average size of the string to be indexed), value M hyper param used while creating the index. (4 * D * N * 1.8 + N * T_SIZE * M) bytes as heap, and young gen (10-20% of the heap, though this will be differ with usage in service, offline job etc and should be tuned in accordance with load test.). Factor of `1.8` comes from internal benchmarks we ran internally and `4` is size of Float as we use float for representing embeddings. Ex: Dataset : 10 million, 300 dimension with Long type as entity Id. Heap requirement can be estimated as 4 * 300 * 10^7 * 1.8 + 10^7 * 8 * 16 ~ 22 GB, young gen as 15% ~ 3GB, Metaspace ~ 512MB. So total RAM in order of ~ 23GB.

* **Disk**

  - We recommend estimating rough disk requirements for serializing index for creating and loading based on this: Embedding Dimension is D, Total elements is N, Entity type is T with size T_SIZE (4 bytes for integer, 8 bytes for long, 2 bytes per character for String: You estimate using average size of the string to be indexed), value M hyper param used while creating the index. (4 * D * N * 1.1 + N * T_SIZE * M) bytes. Factor of `1.1` is used to provide some headroom. Ex: Dataset : 10 million, 300 dimension with Long type as entity Id. Disk requirement can be estimated as 4 * 300 * 10^7 * 1.1 + 10^7 * 8 * 16 ~ 14GB.

* **Memory-mapped Index**

  - In this mode, the index can be served via offheap backed by disk and thus performance will vary according to amount of offheap available in RAM. The serving is not limited by the amount of memory available and is limited by the disk availability. According to our benchmarks the latency performance degrades highly even if the index can be fit in offheap. An index requiring 22GB in memory can be served in less RAM i.e you can even serve it in 8GB RAM (the heap size should be made small for this mode) and rest disk space. It can be used in scalding jobs where the instance are limited by 8GB of RAM. We do not recommend to use this if absolutely necessary, please consult once with Cortex MLX team before using this mode.

* **Type Size**
  
  +------------+--------------+
  | Type       | Size in bytes|
  +============+==============+ 
  | Integer    |      4       |
  +------------+--------------+
  | Character  |      2       | 
  +------------+--------------+
  | Long       |      8       |
  +------------+--------------+
  | String     | avg_len * 2  |
  +------------+--------------+


.. note::
  You should use the ANN load test framework :ref:`load_test` to run benchmarks on your dataset with different runtime/build time params to figure out best ones for your use case taking into account DistanceMetric/Latency/Recall/Index Build Times/Storage/QPS requirements as per your product.
  Currently the maximum memory machines available in Twitter DCs are 256 Gb RAM. The maximum items in an index is limited by the maximum memory available.


