ANN Quickstart
==============

.. admonition:: Use this document if you want to ...

  * Walk through an example workflow of running an Approximate Nearest Neighbor
    (ANN) search by hand.
  * Learn to debug queries against an ANN server via the ML Dashboard.

The goal
--------

Let's say a user has followed someone on Twitter and you want to recommend
additional accounts for them to follow. In this guide, we'll use an
*Approximate Nearest Neighbor (ANN)* search to efficiently find similar accounts to the one
that's just been followed.

Before you start
----------------

.. _prerequisites:

.. admonition:: Prerequisites
  :class: important

  * This guide assumes some **familiarity with Scala**.

  * This guide also assumes familiarity with the concept of **embeddings**. For
    more background on this topic, please see `Embeddings: Technical Overview
    <http://go/embeddings-technical-overview>`__.

  * You'll also need access to the `Hadoop Data Warehouse
    <http://go/hadoop-access>`__. (Your manager will need to approve these
    permissions if you don't already have them.)


Overview of steps
^^^^^^^^^^^^^^^^^

.. sidebar:: Serialized vs. in-memory index
  
  In this tutorial we build a serialized index and run a server with it. In
  your own scripts, however, you might find it more convenient to build an
  index in memory and query it in the same script. For more on this approach,
  see :ref:`hnsw_lib_in_memory`.

This guide will take you through the following steps:

#. First, you'll run an Aurora job to **build an index** that the ANN library
   will use to do its search. You'll build the index from embedding data that
   is available on HDFS.
#. Next, you'll **spin up a query server** in Aurora.
#. Finally, you'll **run an ANN query** via the debugging UI on the `ML
   Dashboard <http://go/ml-dashboard>`__.


Setup
^^^^^

* As mentioned in the prerequisites_, you will need access to the Hadoop Data
  Warehouse. You can verify you have the correct permissions with the
  `ldapsearch` tool:

  .. code-block:: bash

    $ ldapsearch -LLLxh ldap.local.twitter.com -b dc=ods,dc=twitter,dc=corp uid=$USER | grep hadoop-dw

* You will also need to `provision developer credentials
  <http://go/developer-cert>`__ for S2S authentication (budget ~20
  minutes for these certificates to propagate):

  .. code-block:: bash

    $ developer-cert-util --job ann-server-test

* Finally, this tutorial assumes you are working in source and have the
  `JOB_NAME` and `OUTPUT_PATH` environment variables set as follows:

  .. code-block:: bash
  
    $ cd ~/workspace/source
    $ export JOB_NAME=ann_index_builder
    $ export OUTPUT_PATH=/user/$USER/${JOB_NAME}_test


Tutorial
--------

Step 1: Build the Index
^^^^^^^^^^^^^^^^^^^^^^^

The `Cortex MLX team <http://go/mlx>`__ maintains a user embeddings dataset in
HDFS that we can use to compile an index for our search. The
`ann_index_builder` job (`source code
<http://go/code/ann/src/main/scala/com/twitter/ann/scalding/offline/indexbuilder>`__)
can read this embedding data from HDFS, convert it into the ANN format, and add
it to an ANN index, which it then serializes and saves to disk.

Run this job by running following in your terminal:

.. code-block:: bash
  :emphasize-lines: 4,6,8

  $ aurora job create smf1/$USER/devel/$JOB_NAME ann/src/main/aurora/index_builder/aurora_builder.aurora \
    --bind=profile.name=$JOB_NAME \
    --bind=profile.role=$USER \
    --bind=profile.output_dir=hdfs://$OUTPUT_PATH \
    --bind=profile.entity_kind=user \
    --bind=profile.embedding_args='--input.embedding_format tab --input.embedding_path /user/cortex-mlx/official_examples/ann/non_pii_random_user_embeddings_tab_format' \
    --bind=profile.num_dimensions=300 \
    --bind=profile.algo=hnsw \
    --bind=profile.ef_construction=200 \
    --bind=profile.max_m=16 \
    --bind=profile.expected_elements=10000000 \
    --bind=profile.metric=InnerProduct \
    --bind=profile.concurrency_level=32 \
    --bind=profile.hadoop_cluster=dw2-smf1

A few arguments of note are highlighted above:

* `profile.output_dir` specifies where on HDFS your build index will be
  serialized.
* `profile.embedding_args` specifies two things: (1) the format of the user
  embedding data, and (2) where the data is stored on HDFS.

  .. note::
  
    For user privacy reasons, the dataset we're using in this exercise contains
    randomly generated user IDs and embedding vectors. Unfortunately, this
    means that when you query your ANN server :ref:`the final step <step_3>`,
    the output won't map to real users.

* `profile.algo` specifies which ANN algorithm you want to use. In this
  example, we will be using the :ref:`HNSW <hnsw_lib>` technique.

See :ref:`index_building` for a complete description of the different arguments
you can pass to the *index_builder* script.

.. admonition:: Checking on your job's status

  When the command finishes, your terminal will provide a URL that you can
  paste into your browser to check on your job's status. (You can also access
  this page at **go/smf1/$USER/devel/ann_index_builder**.) The job should take
  around 10 minutes to run.

  Once the job finishes, the output data will be available in your own
  directory on HDFS. You can confirm this for yourself:

  .. code-block:: bash
  
      $ ssh hadoopnest1.smf1.twitter.com
      Last login: Mon Apr 29 19:56:53 2019 from 10.35.68.103
      ...
      $ hdfs dfs -ls /user/$USER/
      Found 1 item
      drwxr-xr-x   - $USER   perm-employee-group          0 2019-04-26 18:54 /user/$USER/ann_index_builder_test


Step 2: Start up you server
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Once the index builder job completes, start up your server:

.. code-block:: bash

  $ ./bazel run ann/src/main/python/service/query_server:hnsw-query-server-cli -- \
  --dimension=300 \
  --metric=InnerProduct \
  --id_type=user \
  --env=devel \
  --instances=1 \
  --service_role=$USER \
  --service_name=ann-server-test \
  --cluster=smf1 \
  --hadoop_cluster=dw2-smf1 \
  --index_dir=$OUTPUT_PATH \
  --cpu=8 \
  --disk=10 \
  --ram=17 \
  --heap=16 \
  --new_gen=2 \
  --packer_version=latest

The command line will give you a URL where you can view the status of your
deployment. Once your server is up and running—it should only take a minute or
two—it will be ready to receive queries.

.. admonition:: Troubleshooting
  :class: hint

  * If your server failed to start, you can view the error logs by going to
    **go/$USER/devel/ann-server-test/0** and clicking *View Sandbox*.
  * Check that the parameters you used to start up your server (e.g., `metric`,
    `id_type`, `env`) match the ones you used to generate your index.
  

.. _step_3:

Step 3: Query your server with the web tool
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The debugging console for querying your ANN server is available at
`go/query-ann <http://go/query-ann>`__.

The left sidebar provides several different options for how to do ANN queries.
For this example we'll use the default **Embedding from Featurestore** option.

.. admonition:: What's the deal with the Feature Store?
  :class: note

  We want to provide recommendations to an account with some known Twitter user
  ID. However, our ANN server only understands one feature: user embedding
  vectors. Since it doesn't have any knowledge of any other features (namely,
  user IDs) we need to convert the user ID into an embedding.
  
  The debug console pings the Feature Store behind the scenes to do this
  conversion. If already we had the user embedding on hand, we could use the
  **Query ANN (Embeddings Directly)** version of the debug console instead.

  Learn more about about the Feature Store at `go/feature-store
  <http://go/feature-store>`__.

Fill out the form as follows:

* **Query ID:** A Twitter user ID. (You can find your user ID by searching your
  username at `go/whoami <http://go/whoami>`__.)

* **Query Entity Type:** `user`

* **Index Entity Type:** `tfwId` (see note below for why this isn't `user`)
  
* **Service Destination:** `/srv#/devel/smf1/$USER/ann-server-test` (Make sure
  to substitute $USER with your LDAP username.)

* **FS Dataset Name:** `ConsumerFollowEmbedding300Dataset` (In fact, any of the
  dataset options will work just fine; each will give different results. They
  only differ in the exact way they map users to embeddings.)

* **Feature store major version:** To find the most recent version of the
  feature store, SSH to *hadoopnest* and grab version number with the most
  recent timestamp. In the following example, this would be `1559588651`:
  
  .. code-block:: bash

    $ ssh hadoopnest1.smf1.twitter.com
    ...
    [hadoopnest] $ hdfs dfs -ls /smf1/dw2/user/cortex-mlx/featurestore/offline/user/consumer-producer-follow-svd/300/consumer
    Found 9 items
    drwxrwxrwx   - cortex-mlx cortex-hdfs          0 2019-01-03 09:25 /smf1/dw2/user/cortex-mlx/featurestore/offline/user/consumer-producer-follow-svd/300/consumer/1546473691
    drwxrwxrwx   - cortex-mlx cortex-hdfs          0 2019-01-17 09:16 /smf1/dw2/user/cortex-mlx/featurestore/offline/user/consumer-producer-follow-svd/300/consumer/1547683275
    ...
    drwxr-xr-x   - cortex-mlx cortex-hdfs          0 2019-06-04 12:36 /smf1/dw2/user/cortex-mlx/featurestore/offline/user/consumer-producer-follow-svd/300/consumer/1559588651

* **Distance Type:** `InnerProduct`

* **Number of Neighbors:** `10`

* **EF:** `400`

.. admonition:: Why not use `user` for the Index Entity Type?
  :class: note

  Because if we do, the debug console will attempt to look up the user
  profiles for the IDs returned by our ANN service, and embed them in the
  form response. However, the user IDs in our dataset are randomly
  generated—which means that almost none of them map to real users. If you
  use `user`, you'll get a load of **"ERROR"** responses to your query, which
  is no fun.

  By using `tfwID`, the console will display only the raw response to your
  query. However, this is just a hack to get around the fact that we're using
  dummy data; with real data, you won't want to use `tfwID`.

Once you've filled the form out with the values listed above, click **Search**.
You should see a response that looks something like this:

    .. image:: ann-query-response.png

Congratulations! You've just successfully queried your ANN server.


Next steps
----------

* Use the :ref:`ANN APIs <api>` for ANN index building, querying, and
  serialization
* :ref:`Loadtest <load_test>` your ANN server to better understand its
  performance profile.


Related articles
----------------

* `go/embeddings-technical-overview:
  <http://go/embeddings-technical-overview>`__ Background on embeddings and
  related concepts.
* `go/hadoop-access: <http://go/hadoop-access>`__ Directions to gain access to
  the Hadoop Data Warehouse.
* `go/ann-design-doc: <http://go/ann-design-doc>`__ Design doc for the ANN
  service and library
* `go/hnsw-updates: <http://go/hnsw-updates>`__ Design doc for the ANN
  updates
* `ANN concepts doc:
  <https://docs.google.com/document/d/1o_tZ2s_xBdhlczZNtBqOdvZBvqPaZLsGsMZo7_jHZkk/edit#heading=h.ry0fh35e1ybc>`__
  Starter doc for learning about the ANN problem space and system design.
* The `original research paper <https://arxiv.org/abs/1603.09320>`__ that
  describes the HNSW algorithm.


