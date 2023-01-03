.. _annoy_lib:

Annoy ANN Library
=================

`Annoy <https://github.com/spotify/annoy>`_ is a open source library based on `KD Tree <https://en.wikipedia.org/wiki/K-d_tree>`_ to search for points in space that are close to a given query point. It also creates large read-only file-based data structures that are mmapped into memory so that many processes may share the same data.

.. note::
  We recommend using :ref:`hnsw_lib` for most of your use cases.

Summary
-------

* Works better if you don't have too many dimensions (like <100).
* Index creation is separate from lookup (in particular you can not add more items once the tree has been created)
* Index creation is bounded by RAM
* Index lookup happens by memory mapping the index file.
* Good use case is building index in batch offline and then serving.

.. note::
  Annoy Index building cannot be used on scalding because of some JNI lib versions. But querying can be done on scalding. In order to use on scalding, annoy index can be created and serialized on aurora machine and then can be used only for querying on scalding.

Distance metric supported
--------------------------

* Cosine
* L2

API
---

* `Annoy API <https://cgit.twitter.biz/source/tree/ann/src/main/scala/com/twitter/ann/annoy/TypedAnnoyIndex.scala>`_
* BUILD dependency (`ann/src/main/scala/com/twitter/ann/annoy`)
     
Index Building
---------------

Build index in batch and serialize to disk (hdfs/local directory).
Index building is single threaded and require RAM for building.

.. code-block:: scala

  import com.twitter.ann.annoy.TypedAnnoyIndex
  import com.twitter.ann.common.{AnnInjections, Cosine, CosineDistance, EntityEmbedding}
  import com.twitter.ml.api.embedding.Embedding
  import com.twitter.search.common.file.FileUtils
  import com.twitter.util.{Await, Future, FuturePool}

  val embeddingDimension = 300
  val distanceMetric = Cosine
  val numOfTrees = 30 // Bigger this number, better the quality and bigger the index

  val indexBuilder = TypedAnnoyIndex.indexBuilder[String, CosineDistance](
    embeddingDimension,
    numOfTrees,
    distanceMetric,
    AnnInjections.StringInjection,
    FuturePool.interruptibleUnboundedPool
  )
  
  // Add entity to index
  val entity1 = EntityEmbedding("Some word1", Embedding(Array(1.0f, 2.0f, 3.0f)))
  val entity2 = EntityEmbedding("Some word2", Embedding(Array(2.0f, 3.0f, 4.0f)))
  Await.result(indexBuilder.append(entity1))
  Await.result(indexBuilder.append(entity2))

  // Serialization directory can be local/hdfs directory
  val serializationDirectory = FileUtils.getFileHandle("hdfs:///user/something/index_directory")
  indexBuilder.toDirectory(serializationDirectory)

Index Querying
---------------

Built index can be loaded from hdfs/local directory and gets memory mapped for performing ANN queries.
Queries can be done concurrently.

.. code-block:: scala

  import com.twitter.ann.annoy.{AnnoyRuntimeParams, TypedAnnoyIndex}
  import com.twitter.ann.common.{AnnInjections, Cosine, CosineDistance}
  import com.twitter.ml.api.embedding.Embedding
  import com.twitter.search.common.file.FileUtils
  import com.twitter.util.{Await, Future, FuturePool}

  // Directory path, where index is saved.
  val serializationDirectory = FileUtils.getFileHandle("hdfs:///user/something/index_directory")
  val embeddingDimension = 300
  val distanceMetric = Cosine
  val numOfNeighbours = 10

  // Bigger this number better the recall for the query on the expense of higher latency.
  val runtimeParams = AnnoyRuntimeParams(nodesToExplore = Some(3000))
  val queryable = TypedAnnoyIndex.loadQueryableIndex[String, CosineDistance](
    embeddingDimension,
    distanceMetric,
    AnnInjections.StringInjection,
    FuturePool.interruptibleUnboundedPool,
    serializationDirectory
  )

  val neighbours = Await.result(
    queryableInMemory.queryWithDistance(
      Embedding(Array(1.0f, 2.0, 6.0f)),
      numOfNeighbours,
      runtimeParams
    )
  )

Hyper params usage
-------------------

* **Build params**

  - `numOfTrees`: Provided during build time and affects the build time and the index size. A larger value will give more accurate results, but larger indexes. Emperically reasonable value to start with is 10.

* **Search params**

  - `AnnoyRuntimeParams(nodesToExplore)`: Provided during runtime and affects the search performance. A larger value will give more accurate results, but will take longer time to return. If not provided default value of `numOfTrees*numOfNeighbours` will be used.

.. note::
  You should use the ANN load test framework :ref:`load_test` to run benchmarks on your dataset with different runtime/build time params to figure out best ones for your use case taking into account DistanceMetric/Latency/Recall/Index Build Times/Storage/QPS requirements as per your product.
