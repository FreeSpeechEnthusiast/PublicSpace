.. _ann_debugging:

ANN Debugging
==============

This documentation gives a overview of debugging tools and utilities that can be used to quickly debug various use cases related to ANN based recommendations based on entity embeddings. The utilities tools could be repl/scalding jobs/services etc..

Debug UI (MLDash)
------------------
Nearest neighbours and embeddings can be queried online using a hosted `Debug UI <https://mldashboard.twitter.biz/teams/mlx/annFS>`_ . This UI can be used for the following purpose :

* Querying for approximate nearest neighbours from a deployed ANN service directly using the entity (tweet/user/url/word..) whose embeddings are available in featurestore/manhattan or embeddings can be supplied directly
* Querying for entity embeddings (tweet/user/url/word..) from featurestore/manhattan

Scalding job for calculating nearest neighbors
-------------------------------------------------

This job will help you in calculating nearest neighbors exhaustively(bruteforce) for entities (user/tweet/url..) from embeddings dataset for queries, embeddings dataset of search space and debug entity ids(query embedding dataset) for which nearest neighbours are required from search space.

.. code-block:: bash

  $ ./bazel bundle ann/src/main/scala/com/twitter/ann/scalding/offline:ann-offline-deploy
  $ oscar hdfs \
    --screen --tee log.txt \
    --hadoop-client-memory 6000 \
    --hadoop-properties "yarn.app.mapreduce.am.resource.mb=6000;yarn.app.mapreduce.am.command-opts='-Xmx7500m';mapreduce.map.memory.mb=7500;mapreduce.reduce.java.opts='-Xmx6000m';mapreduce.reduce.memory.mb=7500;mapred.task.timeout=36000000;" \
    --bundle ann-offline-deploy \
    --min-split-size 284217728 \
    --host hadoopnest1.smf1.twitter.com \
    --tool com.twitter.ann.scalding.offline.KnnEntityRecoDebugJob -- \
    --neighbors 10 \
    --metric InnerProduct \
    --query_entity_kind user \
    --search_space_entity_kind user \
    --query.embedding_path /user/apoorvs/sample_embeddings \
    --query.embedding_format tab \
    --search_space.embedding_path /user/apoorvs/sample_embeddings \
    --search_space.embedding_format tab \
    --query_ids 974308319300149248 988871266244464640 2719685122 2489777564 \
    --output_path /user/apoorvs/adhochadoop/test \
    --reducers 100

The above job will exhaustively calculate 10 nearest entities (user in this case) for a few `query_ids` (space separated user ids) based on `InnerProduct` distance metric from search space of user embeddings (user to user NN). It require path to query embeddings, path to search space embeddings and entity kind of both these embeddings.
`query_ids` will be used to filter entity ids from query embeddings dataset and get their embeddings.
Both query and search space embeddings can be of different types i.e queries could be of type user and search space could be of type tweet.
It will generate a human readable text file at `output_path` which will contain the list of nearest neighbours sorted by distance for all the entity ids listed in `query_ids` argument.

.. note::
  `query_ids` should be of type `query_entity_kind`. The output format of the job given query entity of type Q and search space entity of type T would be line separated file with each line having a format of `entityId(Q) entityId(T):distance entityId(T):distance...` where entityId(T) are sorted based on distance from entityId(Q).

Debug repl
------------

.. include:: ../src/main/scala/com/twitter/ann/debug_util/README.rst

