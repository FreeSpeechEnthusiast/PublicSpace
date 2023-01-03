.. _hnsw_query_service:

HNSW Query Service
====================

This document is a generic runbook for HNSW Index based query service that helps you start serving a pre built HNSW index using the :ref:`hnsw_lib` or :ref:`index_building`. This document also explains how to set up dashboards, load test and configure alerts for your service.

Overview
---------

The typical workflow is:

* Create an ANN index using HNSW library.
* Use the server CLI tool to start a staging cluster with ANN index.
* Deploy monitoring dashboards for your staging cluster.
* Send your cluster traffic through a load testing tool (or dark traffic).
* Watch the Viz dashboards to see how your staging cluster performs.
* Make any necessary adjustments to your cluster size and runtime parameters.
* Modify as per your your findings in a fixed Aurora file for production generated via CLI or custom.
* Deploy a production cluster using a service account.
* Deploy monitoring dashboards for your production cluster.

The rest of this document describes these steps in more detail.

.. note::
  For storage requirements of the instances you can refer `Storage Requirements <https://docbird.twitter.biz/ann/hnsw.html#storage-requirements>`_

Start a Staging Cluster
------------------------

The typical ANN index creation results in a index serialized to some directory in HDFS. For staging servers, this is sufficient, since the server supports loading ANN indices from HDFS.

Take note of the HDFS path where your serialized the index.
Start the staging cluster:

.. code-block:: bash

  $ bazel run ann/src/main/python/service/query_server:hnsw-query-server-cli -- \
  --dimension=3 \
  --metric=Cosine \
  --id_type=int \
  --env=staging \
  --instances=1 \
  --service_role=$USER \
  --service_name=ann-server-test \
  --cluster=smf1 \
  --hadoop_cluster=dw2-smf1 \
  --index_dir=/user/$USER/hnsw_real \
  --cpu=8 \
  --disk=10 \
  --ram=17 \
  --heap=16 \
  --new_gen=2 \
  --packer_version={packer_version_of_hnsw_query_server}


`packer_version_of_hnsw_query_server` can be used as latest or you can fix it to particular version you want to test with.

For CLI Help options you can run the following:

.. code-block:: bash

  $ bazel run ann/src/main/python/service/query_server:hnsw-query-server-cli -- -h


Deploy Staging Dashboards
-----------------------------
CLI can be used to deploy staging/devel dashboards containing the commonly used monitors.

.. code-block:: bash

  $ ./bazel run monitoring-configs/ann/query_server/common:publish_adhoc_dashboard  -- --dc=<smf1|atla> --role=<role> --service=<service_name> --env=staging | mon upload

The output of this command shows the URL of the dashboard

Send Traffic to the Staging Cluster
------------------------------------

You may have some staging environment of the calling service which you may be able to use to send traffic to your staging ANN index cluster to see how it performs. If you do not have such a setup within your team, you can use the loadtest tool provided by us to send requests to the server to get rough idea how it performs.

You can refer :ref:`load_test` documentation for running load test.

This setup will help you figuring out your capacity needs for production and for more help you can contact MLX team.

Prepare a Production Configuration
-----------------------------------

After you have experimented with dark or synthetic traffic in your staging setup, you should have initial values for your production cluster configuration. For production servers, you should add/modify these values into an Aurora file that is specific to your team and ANN service. This enables you to fine-tune settings like JVM flags and other configuration options, and to persist that configuration and manage its versioning using Git in Source.

To generate an Aurora file with your server configuration, add the **--dry_run_aurora** flag.

.. code-block:: bash

  $ bazel run ann/src/main/python/service/query_server:hnsw-query-server-cli -- \
  --dimension=<Dimension of vector> \
  --metric=<L2|Cosine|InnerProduct> \
  --id_type=<int|long|string|word|user|tweet|tfwId> \
  --env=prod \
  --instances=<server instances> \
  --service_role=<role> \
  --service_name=<service_name> \
  --cluster=<smf1|atla> \
  --hadoop_cluster=<dw2-smf1|proc-atla> \
  --index_dir=<hdfs index directory without hdfs://> \
  --cpu=<cpu> \
  --disk=<disk in gb> \
  --ram=<ram in gb> \
  --heap=<heap in gb> \
  --new_gen=<new_gen in gb> \
  --packer_version=<packer_version_of_hnsw_query_server> \
  --dry_run_aurora > index.aurora

`packer_version_of_hnsw_query_server` can be used as latest or you can fix it to particular version you want to test with.

.. note::
  Do not use the server CLI tool to facilitate deploy for production clusters. You must have a fixed Aurora configuration file for your production ANN query servers.

Deploy a Production Cluster
----------------------------

At this point you are ready to deploy a production cluster. You need:

* Fixed Aurora configuration file
* Directory of serialized ANN index in HDFS
* Service account
* Capacity
* Valid service identifier for s2s auth (S2S certificates pre-requested)

Once you have all these things you can deploy the service as follows:

`aurora update start {cluster}/{role}/prod/{service_name} {aurora_file_path}`


S2S Auth
----------------------
ANN service supports s2s auth by default and requires valid s2s certificates for startup in devel/staging/prod environment. For more info refer `s2s <http://go/s2s>`_

* The default service identifier that will be used for retrieving the s2s certificates is `{{service_role}}:{{service_name}}:{{env}}:{{cluster}}`
* For provisioning certificates refer this `Link <https://docbird.twitter.biz/service_authentication/howto/credentials.html>`_

.. warning:: Service startup will fail if certificates are not available or invalid.

.. note::  Certificates should be requested beforehand for the specific role/service/env/cluster. It generally takes 30 minutes for the certificates to propagate in dcs.


Deploy Mon2 Alerts
-------------------
The directory `monitoring-configs/ann/query_server/template` contains a template that sets up some commonly used monitors and alerts, and you can customize the predicates and other team related config in a file.

Steps to setup Mon2 alerts:

* Create mon2 directory custom to your team and service.

.. code-block:: bash

  $ mkdir -p monitoring-configs/<yourteam>/<service_directory>

* Copy the template.

.. code-block:: bash

  $ cp -r monitoring-configs/ann/query_server/template/* monitoring-configs/<yourteam>/<service_directory>/

* Customize the team related config i.e role/pager_duty/slack/emails etc and the predicates for alerting thresholds by editing `CONFIG` and `PREDICATE` in:

.. code-block:: bash

  $ monitoring-configs/<yourteam>/<service_directory>/dashboard_config.py

* Test the alerts by running

.. code-block:: bash

  $ ./bazel test monitoring-configs/<yourteam>/<service_directory>:test

* Validate the alerts by running

.. code-block:: bash

  $ ./bazel run monitoring-configs/<yourteam>/<service_directory>:create | mon upload --validate


* Deploy the alerts by running

.. code-block:: bash

  $ ./bazel run monitoring-configs/<yourteam>/<service_directory>:create | mon upload


Hot Swapping an Index
----------------------
It is possible to swap the index we are serving with a new index without rolling the cluster.

* This requires all ann indices to be generated within a single base directory.
* The directory name for individual ann index within the base directory should be a timestamp/number (anything that matches the regex `[0-9]+$`).
* The service will automatically pick up the latest ann directory based on numeric sort order.

Add `--refreshable` flag (default to `false`) when you use cli to generate aurora file, and the deploy the service.
This flag will have the service check the `index_dir` periodically, and load new index if there is a newer version of valid index available.

A "validated" index directory must have _SUCCESS file and the size is validated in between min and max index size defined in `ValidatedIndexPathProvider <https://cgit.twitter.biz/source/tree/ann/src/main/scala/com/twitter/ann/service/query_server/common/IndexPathProvider.scala#n15>`_

.. note:: the `index_dir` should point to the base directory containing all the ann indices rather than the actual index path.

.. warning:: the service might run into some GC spikes when we swap index, please do check GC time after you enable this feature

