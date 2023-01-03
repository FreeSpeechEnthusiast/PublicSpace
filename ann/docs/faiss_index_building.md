# Faiss index building

There are two ways for building faiss ANN indicies. Scalding based jobs run on-prem and dataflow is in GCP.

## Scalding

### Scala API

We provide high level API which take embeddings and outputs index file to location on hdfs.

```scala
trait FaissIndexer {
  /**
   * Produce faiss index file specified by factory string
   *
   * @param pipe Embeddings to be indexed
   * @param sampleRate Fraction of embeddings used for training. Regardless of this parameter, all embeddings are present in the output.
   * @param factoryString Faiss factory string, see https://github.com/facebookresearch/faiss/wiki/The-index-factory
   * @param metric Metric to use
   * @param outputDirectory Directory where _SUCCESS and faiss.index will be written.
   */
  def build[D <: Distance[D]](
    pipe: TypedPipe[EntityEmbedding[Long]],
    sampleRate: Float,
    factoryString: String,
    metric: Metric[D],
    outputDirectory: AbstractFile
  ): Execution[Unit]
}
```

[Source](https://sourcegraph.twitter.biz/git.twitter.biz/source/-/blob/ann/src/main/scala/com/twitter/ann/faiss/FaissIndexer.scala)

### Aurora setup

Faiss requires this aurora step to be executed before launching the job. This means added a new step to your config and adding a ordering constraint.

```python
native_libraries = Packer.install(
    name='gcc-12.0-stdlib',
    role='cassowary',
    version='latest'
)

task = Task(
  processes = [main, native_libraries, ...],
  constraints = order(native_libraries, main) + ...
  ...
),

```

[Full example](https://sourcegraph.twitter.biz/git.twitter.biz/source/-/blob/ann/scripts/recos-platform/follow2vec-ann-faiss/index-builder.aurora)

## Dataflow

Provided dataflow indexing job take input from BQ table and outputs index file to GCS.

### Prerequisites

- [GCP Onboarding](gcp_onboarding/user_onboarding.html)
- [d6w tool](d6w/index.html)
- For GPU support [Docker](kubeflow/user/gcr.html#pushing-and-pulling-docker-images-locally)

### Running the job

Using [example job](https://sourcegraph.twitter.biz/git.twitter.biz/source/-/tree/ann/src/main/python/dataflow) as a starting point:

- modify `bq.sql` to read from your dataset. Job expect columns `entityId, embedding` to be present, and be unique by `entityId`
- update `faiss_index_bq.d6w` to specify your GCP project and account name

`bazel bundle ann/src/main/python/dataflow:faiss_indexing_bin && bin/d6w create --pex dist/faiss_indexing_bin.pex $GCP_PROJECT/us-central1/faiss-index-bq-nogpu ann/src/main/python/dataflow/scripts/faiss_index_bq.d6w`

### GPU support

if you want to use GPU – build and upload image from `worker_harness/Dockerfile` to be accessible from GCP project.

`bazel bundle ann/src/main/python/dataflow:faiss_indexing_bin && bin/d6w create --pex dist/faiss_indexing_bin.pex $GCP_PROJECT/us-central1/faiss-index-bq-gpu ann/src/main/python/dataflow/scripts/faiss_index_bq.d6w`

Notice `-gpu` suffix.
