We provide a local bazel repl and sample examples with right dependencies setup to support bunch of debug operations like
querying embedding from manhattan store, querying an ANN service with sample embeddings, querying an ANN service with an entity id whose embeddings are available in MH/Featurestore etc..

Starting the repl with the right dependencies and tunnels open.

.. code-block:: bash

  $ tweetypie/scripts/tunneled ./bazel repl \
    --jvm_flag="-Dcom.twitter.server.wilyns.disable=true" \
    ann/src/main/scala/com/twitter/ann/debug_util

After starting the repl, you can follow sample examples available for debugging at `Link <https://cgit.twitter.biz/source/tree/ann/src/main/scala/com/twitter/ann/debug_util/SampleExamples.scala>`_
One of the example to query closest tweets from an ANN index for an user whose embeddings is available in online feature store.

.. code-block:: bash

  $ :load ann/src/main/scala/com/twitter/ann/debug_util/SampleExamples.scala
  $ SampleExamples.sampleServiceQueryClientWithIdWithFeatureStore(userId=12L)
