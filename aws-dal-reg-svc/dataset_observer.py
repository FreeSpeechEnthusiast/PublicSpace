from random import shuffle
from threading import Event
from time import time
from typing import Any, Dict, Type

from twitter.common import app, log
from twitter.common.metrics import AtomicGauge, Observable, RootMetrics
from twitter.common.metrics.metrics import Metrics
from util import account_stage_key, envoy_proxy_connection_check, terminate_envoy_sidecar

from aws_arn import Arn
from aws_cloud_watch import (
  account_metric,
  CloudWatchReporter,
  get_cloud_watch_reporter,
  put_account_metric,
)
from aws_iam import AwsAuthenticator, get_authenticator
from aws_organizations import get_accounts
from botocore.exceptions import ClientError
from dataset_metastore import Metastore
from dataset_stagestore import Stagestore
from errors import MisconfiguredResourceScanner
from server_config import (
  AWS_EXCLUDED_REGIONS,
  AWS_ORGANIZATION_ACCOUNT_ID,
  DYNAMO_STAGES_TABLE_ACCOUNT_ID,
  RESOURCE_SCANNER_REQUIRED_ATTRS,
)
from server_state import ServerState


app.add_option("--aws-access-key", default=None, dest="access_key", help="AWS API access key.")
app.add_option(
  "--aws-secret-access-key", default=None, dest="secret_key", help="AWS API secret access key."
)
app.add_option("--aws-token", default=None, dest="token", help="AWS API token.")
app.add_option(
  "--cloud-watch-namespace",
  default="aws-pdp-dataset-observer",
  dest="cloud_watch_namespace",
  help="AWS CloudWatch namespace to be used with metrics emitted.",
)
app.add_option(
  "--envoy-proxy-url",
  default=None,
  dest="envoy_proxy_url",
  help="Envoy Proxy Url. Enable use of envoy proxy for requests to AWS APIs.",
)

app.add_option(
  "--load-env-creds",
  action="store_true",
  default=False,
  dest="load_env_creds",
  help="Load AWS credentials from env variables.",
)
app.add_option(
  "--load-tss-creds",
  action="store_true",
  default=False,
  dest="load_tss_creds",
  help="Load credentials from TSS.",
)
app.add_option(
  "--local-dynamodb-create-table",
  action="store_true",
  default=False,
  dest="local_dynamodb_create",
  help="Create a local dynamodb table to use for dataset metastore.",
)
app.add_option(
  "--local-dynamodb-endpoint",
  default=None,
  dest="local_dynamodb_endpoint",
  help="Define a local dynamodb endpoint to use for dataset metastore.",
)
app.add_option(
  "--log-resource-observations",
  action="store_true",
  default=False,
  dest="log_resource_observations",
  help="Enable log entries for all AWS resources observed.",
)
app.add_option(
  "--process-account",
  default=None,
  dest="process_account",
  help="Manually process a single AWS account.",
)
app.add_option(
  "--scan-exclusion-interval",
  default="21600",
  dest="scan_exclusion_interval",
  help="Number of seconds to withhold an account from being scanned since last processed.",
)


class DatasetObserver(Observable):
  _EXEMPT_SCANNER_ATTRS = []

  def __init__(self, auth: AwsAuthenticator):
    self.account_id = auth.account_id
    self.authenticator = auth
    self.counter = 0
    self.metastore = Metastore(auth)
    self.scan_incomplete = Event()
    self._list_resources_errors = self.metrics.register(AtomicGauge("list_resources_errors"))
    self._metastore_set_observed_errors = self.metrics.register(
      AtomicGauge("metastore_set_observed_errors")
    )
    self._regions_scanned = self.metrics.register(AtomicGauge("regions_scanned"))
    self._scan_duration = self.metrics.register(AtomicGauge("scan_duration"))

  def log_sfx(self, region: str = None) -> str:
    sfx = f"acct: {self.account_id}"
    if region:
      return sfx + f" region: {region}"
    return sfx

  def process_response(self, region: str, resp: Dict[str, Any]):
    for entity in resp[self.entities_key]:
      name = (
        entity[self.nested_key]
        if isinstance(entity, dict) and getattr(self, "nested_key")
        else entity
      )
      if ServerState().options.get("log_resource_observations"):
        log.info(f"Observed {self.service} resource: {name} in {self.log_sfx(region)}.")
      self.counter += 1

      try:
        self.set_observed(name, region)
      except Exception as meta_ex:
        log.error(
          f"Unable to set_observed in metastore for {self.service} resource {name} in {self.log_sfx()}. err={meta_ex}"
        )
        self._metastore_set_observed_errors.increment()
        self.scan_incomplete.set()

  def scan(self):
    self.validate()
    session = self.authenticator.new_session()
    start_ts = int(time())
    for region in session.get_available_regions(self.service):
      self.scan_region(region)
      self._regions_scanned.increment()
    self._scan_duration.add(int(time()) - start_ts)
    log.info(
      "{} scan complete ({} resources). {}".format(
        self.__class__.__name__, self.counter, self.log_sfx()
      )
    )

  def scan_region(self, region: str):
    if region in AWS_EXCLUDED_REGIONS:
      log.info(
        f"Skipping {self.service} resource scanning in opt-in region: {self.log_sfx(region)}."
      )
      return
    else:
      log.info(f"Scanning {self.service} resources in {self.log_sfx(region)}.")

    try:
      client = self.authenticator.new_client(self.service, region)
      for resp in client.get_paginator(self.scan_method).paginate():
        if self.entities_key in resp.keys():
          self.process_response(region, resp)
    except ClientError as ex:
      log.error(f"unable to list {self.service} resources in {self.log_sfx(region)}. err={ex}")
      self._list_resources_errors.increment()
      self.scan_incomplete.set()

  def set_observed(self, name: str, region: str):
    meta_key = "/".join([region, self.service, name])
    log.info(f"Setting observed on meta_key {meta_key}")
    self.metastore.set_observed(meta_key)

  def validate(self):
    for attr in RESOURCE_SCANNER_REQUIRED_ATTRS:
      if attr not in self._EXEMPT_SCANNER_ATTRS:
        if not hasattr(self, attr):
          raise MisconfiguredResourceScanner(attr, self.__class__.__name__)


class DAXObserver(DatasetObserver):
  def __init__(self, auth: AwsAuthenticator):
    self.entities_key = "Clusters"
    self.scan_method = "describe_clusters"
    self.nested_key = "ClusterName"
    self.service = "dax"
    super().__init__(auth)


class DynamoDbObserver(DatasetObserver):
  def __init__(self, auth: AwsAuthenticator):
    self.entities_key = "TableNames"
    self.scan_method = "list_tables"
    self.service = "dynamodb"
    super().__init__(auth)


class KinesisObserver(DatasetObserver):
  def __init__(self, auth: AwsAuthenticator):
    self.entities_key = "StreamNames"
    self.scan_method = "list_streams"
    self.service = "kinesis"
    super().__init__(auth)


class ElastiCacheClusterObserver(DatasetObserver):
  def __init__(self, auth: AwsAuthenticator):
    self.entities_key = "CacheClusters"
    self.scan_method = "describe_cache_clusters"
    self.service = "elasticache"
    self.nested_key = "CacheClusterId"
    super().__init__(auth)


class ElastiCacheSnapShotObserver(DatasetObserver):
  def __init__(self, auth: AwsAuthenticator):
    self.entities_key = "CacheSnapshots"
    self.scan_method = "describe_snapshots"
    self.service = "elasticache"
    self.nested_key = "SnapshotName"
    super().__init__(auth)


class S3Observer(DatasetObserver):
  _SCAN_REGION = "us-west-2"

  def __init__(self, auth: AwsAuthenticator):
    self._EXEMPT_SCANNER_ATTRS = ["scan_method"]
    self.entities_key = "Buckets"
    self.service = "s3"
    super().__init__(auth)

  def bucket_region(self, client, name: str) -> str:
    region = client.get_bucket_location(Bucket=name, ExpectedBucketOwner=self.account_id)[
      "LocationConstraint"
    ]
    if region is None:
      region = "us-east-1"
    elif region == "EU":
      region = "eu-west-1"
    return region

  def scan(self):
    # The S3 API doesn't support regional resource listing or pagination
    # requiring the parent class `scan` method to be overridden.
    self.validate()
    client = self.authenticator.new_client(self.service, self._SCAN_REGION)
    start_ts = int(time())
    resp = client.list_buckets()
    if self.entities_key in resp.keys():
      for entity in resp[self.entities_key]:
        self.counter += 1

        # the S3 ListBuckets response data-structure differs
        # from the expected behaivor within the parent class.
        name = entity["Name"]
        if ServerState().options.get("log_resource_observations"):
          log.info(f"Observed {self.service} resource: {name} in {self.log_sfx()}.")

        try:
          self.set_observed(name, self.bucket_region(client, name))
        except Exception as meta_ex:
          log.error(
            f"Unable to set_observed in metastore for {self.service} resource {name} in {self.log_sfx()}. err={meta_ex}"
          )
          self._metastore_set_observed_errors.increment()
          self.scan_incomplete.set()

      self._regions_scanned.increment()
      self._scan_duration.add(int(time()) - start_ts)
      log.info(
        "{} scan complete ({} resources). {}".format(
          self.__class__.__name__, self.counter, self.log_sfx()
        )
      )


class SqsObserver(DatasetObserver):
  def __init__(self, auth: AwsAuthenticator):
    self.entities_key = "QueueUrls"
    self.scan_method = "list_queues"
    self.service = "sqs"
    super().__init__(auth)

  def process_response(self, region: str, resp: Dict[str, Any]):
    client = self.authenticator.new_client(self.service, region)
    for queue_url in resp[self.entities_key]:
      if ServerState().options.get("log_resource_observations"):
        log.info(f"Observed {self.service} resource: {queue_url} in {self.log_sfx(region)}.")
      self.counter += 1

      try:
        # grab the arn
        q_attr_resp = client.get_queue_attributes(QueueUrl=queue_url, AttributeNames=["QueueArn"])
        q_arn = Arn(q_attr_resp["Attributes"]["QueueArn"])  # will throw key error if not exist
        self.set_observed(q_arn.resource, region)
      except Exception as meta_ex:
        log.error(
          f"Unable to set_observed in metastore for {self.service} resource {queue_url} in {self.log_sfx()}. err={meta_ex}"
        )
        self._metastore_set_observed_errors.increment()
        self.scan_incomplete.set()


def process_observer(
  account_id: str, observer: Type[DatasetObserver], metrics: Metrics, reporter: CloudWatchReporter
):
  metrics.register_observable("_".join([observer.service, "observer"]), observer)
  observer.scan()
  put_account_metric(
    account_metric(account_id, "_".join([observer.service, "datasets"])),
    reporter,
    observer.counter,
  )


def main(args, options):
  ServerState({}, options.__dict__)

  if options.envoy_proxy_url:
    envoy_proxy_connection_check(options.envoy_proxy_url)

  cw_reporter = get_cloud_watch_reporter(options.cloud_watch_namespace)
  scan_intvl = int(options.scan_exclusion_interval)
  stage_store = Stagestore(get_authenticator(DYNAMO_STAGES_TABLE_ACCOUNT_ID))

  if options.process_account:
    account_ids = [options.process_account]
  else:
    account_ids = get_accounts(get_authenticator(AWS_ORGANIZATION_ACCOUNT_ID))

  shuffle(account_ids)
  for account_id in account_ids:
    account_metrics = RootMetrics().scope(account_id)
    stage_key = account_stage_key(account_id, "observe")
    stage_record = stage_store.get(stage_key)
    if stage_record and "ts" in stage_record:
      ts = stage_record["ts"]
      if scan_intvl > (int(time()) - ts):
        log.info(f"Skipping recently scanned account: {account_id}, stage ts: {ts}.")
        continue
    log.info(f"Processing account: {account_id}...")

    try:
      auth = get_authenticator(account_id)
    except Exception as ex:
      log.error(f"Unable to create auth session to account: {account_id}. err={ex}")
      continue

    try:
      observers = [
        DAXObserver(auth),
        DynamoDbObserver(auth),
        KinesisObserver(auth),
        S3Observer(auth),
        SqsObserver(auth),
        ElastiCacheClusterObserver(auth),
        ElastiCacheSnapShotObserver(auth),
      ]
      for observer in observers:
        process_observer(account_id, observer, account_metrics, cw_reporter)
      if not any([o.scan_incomplete.is_set() for o in observers]):
        stage_store.put(stage_key)
        put_account_metric(account_metric(account_id, "processed"), cw_reporter)
    except Exception as ex:
      log.error(f"Unable to scan datasets in account: {account_id}. err={ex}")

  log.info("Processing complete, shutting down.")
  terminate_envoy_sidecar(options.envoy_proxy_url)


app.set_name("aws-pdp-dataset-observer")
app.main()
