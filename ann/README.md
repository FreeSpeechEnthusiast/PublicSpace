This document is available at [go/ann-docs](http://go/ann-docs)

ANN (Approximate nearest neighbour)
=====================================

Overview
--------
This project provides a library and API for nearest neighbors search.

Nearest neighbor search ([NNS](https://en.wikipedia.org/wiki/Nearest_neighbor_search)), is an optimization problem of finding the point in a given set that is closest (or most similar) to a given point. Closeness is typically expressed in terms of a dissimilarity function: the less similar the objects, the larger the function values. Formally, the nearest-neighbor (NN) search problem is defined as follows: given a set S of points in a space M and a query point q ∈ M, find the closest point in S to q.

Example of Distance/Dissimilarity function: euclidean, dot product, cosine.

*Approximate nearest neighbour search* : In some applications it may be acceptable to retrieve a "good guess" of the nearest neighbor. In those cases, we can use an algorithm which doesn't guarantee to return the actual nearest neighbor in every case, in return for improved speed or memory savings. Often such an algorithm will find the nearest neighbor in a majority of cases, but this depends strongly on the dataset being queried.

How To Use ANN
--------------
You can use the [ANN library](api.html) on it's own to build and use indices in your service. 

You can use the [index building job](index_building.html) to build your index from data on HDFS and serialize to disk. You can deserialize and use this index using just our library or you can use it with the [query service](hnsw_query_service.html) that we have built.

You can [loadtest](loadtest.html) the embedded index or the query service using our load test tool. This can help you find the best settings for recall and latency.

Finally, you can use the [service client](query_service_client.html) to query the service.