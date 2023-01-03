.. \_faiss_query_service:

# Faiss query service

## HDFS directory structure

### Daily batches

```
.../
├─ 2022/
│  ├─ 09/
│  │  ├─ 21/
│  │  │  ├─ faiss.index
│  │  │  ├─ _SUCCESS
│  │  ├─ 22/
│  │  │  ├─ _SUCCESS
│  │  │  ├─ faiss.index
│  │  ├─ .../
```

ie "yyyy/MM/dd"

### Hourly batches

```
.../
├─ 2022/
│  ├─ 09/
│  │  ├─ 21/
│  │  │  ├─ 00/
│  │  │  │  ├─ _SUCCESS
│  │  │  │  ├─ faiss.index
│  │  │  ├─ 01/
│  │  │  │  ├─ _SUCCESS
│  │  │  │  ├─ faiss.index
```

ie "yyyy/MM/dd/HH"

## Aurora service

Starting with [example configuration](https://sourcegraph.twitter.biz/git.twitter.biz/source/-/blob/ann/scripts/recos-platform/follow2vec-ann-faiss/query-server.aurora), most likely you want to change this variables

```python
class Profile(Struct):
  log_level = Default(String, 'INFO')
  # Dimension of your embeddings
  dimension = Default(Integer, 200)
  # Root hdfs directory to load an index file
  index_directory = Default(String, '/user/cassowary/follow2vec-ann-faiss/pq100')
  refreshable = Default(Boolean, False)
  hadoop_cluster = Default(String, 'proc3-atla')
  jar = Default(String, 'faiss-query-server.jar')
  # Name of your cluster
  name = Default(String, 'follow2vec-ann-faiss')
  # Role
  role = Default(String, 'cassowary')
  id_type = Default(String, 'long')
  # Distance metric
  metric = Default(String, 'Cosine')
  packer_role = Default(String, 'cassowary')
  packer_package = Default(String, 'faiss-query-server')
  packer_version = Default(String, 'latest')

resources = Resources(
  cpu = 5,
  ram = 16 * GB, # Sum of index file sizes + 12GB
  disk = 6 * GB
)
```

## Deploy Staging Dashboards

CLI can be used to deploy staging/devel dashboards containing the commonly used monitors.

```bash
  $ ./bazel run monitoring-configs/ann/query_server/common:publish_adhoc_dashboard  -- --dc=<smf1|atla|pdxa> --role=<role> --service=<service_name> --env=staging | mon upload
```

The output of this command shows the URL of the dashboard

## Production dashboards

Using [example monitoring config](https://sourcegraph.twitter.biz/git.twitter.biz/source/-/tree/monitoring-configs/recos_platform/follow2vec_faiss) as a starting point, in `config.py` file, change corresponding variables

```python
CONFIG = {
  "team": "recos-platform",  # Name of your team
  "role": "cassowary",  # Aurora role running the server
  "service": "follow2vec-ann-faiss",  # Aurora job name running the server
  "info_email": rp_warn_email,  # Info email (usually mailing list)
  "alert_email": "follow2vec-ann-faiss@twittertcc.pagerduty.com",  # Alert email (usually Pagerduty)
  "pagerduty_id": "PE88PML",  # Pagerduty team ID
  "pagerduty_email": "follow2vec-ann-faiss@twittertcc.pagerduty.com",
  "slackroom_id": "recos-platform-bot",  # Slack room channel ID
  "canary_instances": 1,  # Number of canary instances
}
```

Change predicates to reflect your SLAs

```python
# Query service predicate configuration
PREDICATES = {
  "query_success_rate": {  # ANN Query Success rate
    "warn": "< 99.9 for 2 of 10m",
    "critical": "< 99.8 for 5 of 10m",
  },
  "query_qps": Empty,
  "query_latency_p99_ms": {  # ANN P99 Latency
    "warn": "> 60 for 2 of 10m",
    "critical": "> 85 for 5 of 10m",
  },
  "cluster_consistency": {  # ANN Cluster consistency
    "warn": "> 1.0 for 50 of 50m",
    "critical": "> 1.0 for 55 of 55m",
  },
  "index_build_stale_hours": {},  # ANN index build stale
  "index_load_error": {},  # ANN index reload failure
  "index_find_error": {},  # ANN index find failure
  "min_num_replica": {  # ANN Cluster Size
    "warn": "< 5 for 10 of 10m",
  },
  "serving_index_stale": {  # ANN Serving Stale Index
    "warn": "> 0 for 20 of 20m",
    "critical": "> 0 for 30 of 30m",
  },
  "latest_index_invalid": {
    "warn": "> 0 for 40 of 40m",
    "critical": "> 0 for 50 of 50m",
  },
  "ParNew-msec": {"predicates": Empty},
  "ConcurrentMarkSweep-msec": {"predicates": Empty},
  "msec": {"predicates": Empty},
}

```

## Serving a window of hourly batches with hot swapping

Faiss based service has capability to serve requests using sliding window of hourly batches from HDFS. Given this tree

```
.../
├─ 2022/
│  ├─ 09/
│  │  ├─ 21/
│  │  │  ├─ 00/
│  │  │  │  ├─ _SUCCESS
│  │  │  │  ├─ faiss.index
│  │  │  ├─ 01/
│  │  │  │  ├─ _SUCCESS
│  │  │  │  ├─ faiss.index
```

and these flags passed to the server binary

```
# Enable sharded index
-sharded true
# How many latest shards to load
-shardedHours 2
# How often to poll hdfs for new shards
-shardedWatchIntervalMinutes 5
# How many hours backwards to try before giving up
-shardedWatchLookbackIndexes 24
```

If current server time is 2022-09-21 09:41, we will find two shards at 2022/09/21/00 and 2022/09/21/01 and keep polling hdfs for any changes at 5 minute intervals. Once 2022/09/21/02 is found, it will be loaded and oldest shard (2022/09/21/00) will be unloaded synchronously. If 24 hours pass without
