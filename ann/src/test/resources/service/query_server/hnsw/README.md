HNSW indexes can be built from some [trivial embeddings](./embeddings.txt)
in a text file for testing and local development without access to any prod datasets.

Separate embeddings are built for L2, InnerProduct & Cosine distances. The generated indexes along
with queries are used for integration tests.

```bash
    $ bazel run ann/src/test/scala/com/twitter/ann/service/query_server/hnsw:index_generator
    $ ls ann/src/test/resources/service/query_server/hnsw/{hnsw_cosine,hnsw_inner_product,hnsw_l2}
```
