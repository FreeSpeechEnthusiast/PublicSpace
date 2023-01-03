import concurrent.futures
from datetime import datetime
import math
from typing import Any, Dict, Optional

from twitter.common import log
from twitter.common.metrics import AtomicGauge, Observable
from util import CompletionQueue, copy_completion_queue, response_tags

from aws_arn import Arn, S3Arn
from aws_iam import AwsAuthenticator
from aws_resource import Resource
from botocore.exceptions import ClientError
from dataset_metastore import Metastore
from errors import AwsMissingTTL, MisconfiguredResourceScanner
from server_config import AWS_EXCLUDED_REGIONS, FUTURE_TIMEOUTS, RESOURCE_SCANNER_REQUIRED_ATTRS
from server_state import ServerState


class ResourceScanner(Observable):
  _EXEMPT_SCANNER_ATTRS = []

  def __init__(self, account_id: str, authenticator: AwsAuthenticator, snk: CompletionQueue):
    self.account_id = account_id
    self.authenticator = authenticator
    self.counter = 0
    self.metastore = Metastore(authenticator)
    self.snk = snk
    self._list_resources_errors = self.metrics.register(AtomicGauge("list_resources_errors"))
    self._metastore_set_observed_errors = self.metrics.register(
      AtomicGauge("metastore_set_observed_errors")
    )
    self._process_resource_errors = self.metrics.register(AtomicGauge("process_resource_errors"))
    self._process_retention_errors = self.metrics.register(AtomicGauge("process_retention_errors"))
    self._regions_scanned = self.metrics.register(AtomicGauge("regions_scanned"))

  def handler(self, client, region: str, resource_name: str) -> Resource:
    raise NotImplementedError

  def log_sfx(self, region: str = None) -> str:
    sfx = ""
    if region:
      sfx = f" region: {region}"
    return f"acct: {self.account_id}{sfx}"

  def process_response(self, client, region: str, resp: Dict[str, Any]):
    for name in resp[self.entities_key]:
      try:
        resource = self.handler(client, region, name)
        self.snk.put(resource)
        self.counter += 1

        try:
          self.metastore.set_observed(self.metastore.key(resource.arn))
        except Exception as meta_ex:
          self._metastore_set_observed_errors.increment()
          log.error(
            f"unable to set_observed in metastore for {self.service} resource {name} in {self.log_sfx(region)}. err={meta_ex}"
          )
      except Exception as ex:
        if isinstance(ex, ClientError):
          if ex.response["Error"]["Code"] == "ResourceNotFoundException":
            continue
        self._process_resource_errors.increment()
        log.error(
          f"unable to process {self.service} resource {name} in {self.log_sfx(region)}. err={ex}"
        )
        raise ex

  def scan(self):
    try:
      self.validate()
      session = self.authenticator.new_session()
      for region in session.get_available_regions(self.service):
        self.scan_region(region)
        self._regions_scanned.increment()
    finally:
      log.info(
        "{} scan complete ({} resources). {}".format(
          self.__class__.__name__, self.counter, self.log_sfx()
        )
      )
      self.snk.set_completed()

  def scan_region(self, region: str):
    self.validate()
    if region in AWS_EXCLUDED_REGIONS:
      log.info(
        f"skipping {self.service} resource scanning in opt-in region: {self.log_sfx(region)}."
      )
      return
    else:
      log.info(f"scanning {self.service} resources in {self.log_sfx(region)}.")

    try:
      client = self.authenticator.new_client(self.service, region)
      for resp in client.get_paginator(self.scan_method).paginate():
        if self.entities_key in resp.keys():
          self.process_response(client, region, resp)
    except ClientError as ex:
      log.error(f"unable to list {self.service} resources in {self.log_sfx(region)}. err={ex}")
      self._list_resources_errors.increment()

  def validate(self):
    for attr in RESOURCE_SCANNER_REQUIRED_ATTRS:
      if attr not in self._EXEMPT_SCANNER_ATTRS:
        if not hasattr(self, attr):
          raise MisconfiguredResourceScanner(attr, self.__class__.__name__)


# Describe SnapShots
class ElastiCacheSnapshotScanner(ResourceScanner):
  def __init__(self, account_id: str, authenticator: AwsAuthenticator):
    self.entities_key = "Snapshots"
    self.scan_method = "describe_snapshots"
    self.service = "elasticache"
    super().__init__(account_id, authenticator, CompletionQueue())
    self._observed_elasticache_snapshots = self.metrics.register(
      AtomicGauge("observed_elasticache_snapshots")
    )

  def handler(self, client, region: str, snapshot: Dict[str, Any]) -> Resource:
    arn = snapshot["ARN"]
    snapshot_name = snapshot["SnapshotName"]
    engine_type = snapshot["Engine"]
    engine_version = snapshot["EngineVersion"]

    tags = []
    if not ServerState().options.get("disable_tag_queries"):
      tags = response_tags(client.list_tags_for_resource(ResourceName=arn))

    if ServerState().options.get("log_resource_observations"):
      log.info(f"Observed resource: {arn} in {self.log_sfx(region)}.")
    self._observed_elasticache_snapshots.increment()

    properties = {
      "Tags": tags,
      "Retention": snapshot["SnapshotRetentionLimit"],
      "ElastiCacheEngineVersion": engine_version,
      "ElastiCacheEngineType": engine_type,
    }
    # If there are multiple snapshots per cluster, pick the earliest snapshot available
    snapshot_create_time = min(snapshot["NodeSnapshots"], key=lambda x: x["SnapshotCreateTime"])[
      "SnapshotCreateTime"
    ]
    return Resource(
      snapshot_name, Arn(arn), snapshot_create_time, snapshot["SnapshotStatus"], properties
    )


class ElastiCacheClusterScanner(ResourceScanner):
  def __init__(self, account_id: str, authenticator: AwsAuthenticator):
    self.entities_key = "CacheClusters"
    self.scan_method = "describe_cache_clusters"
    self.service = "elasticache"
    super().__init__(account_id, authenticator, CompletionQueue())
    self._observed_elasticache_clusters = self.metrics.register(
      AtomicGauge("observed_elasticache_clusters")
    )

  def handler(self, client, region: str, cluster: Dict[str, Any]) -> Resource:
    arn = cluster["ARN"]
    cluster_id = cluster["CacheClusterId"]
    engine_type = cluster["Engine"]
    engine_version = cluster["EngineVersion"]
    at_rest_encryption = cluster["AtRestEncryptionEnabled"]
    transit_encryption = cluster["TransitEncryptionEnabled"]
    engine_version = cluster["EngineVersion"]

    tags = []
    if not ServerState().options.get("disable_tag_queries"):
      tags = response_tags(client.list_tags_for_resource(ResourceName=arn))

    if ServerState().options.get("log_resource_observations"):
      log.info(f"Observed resource: {arn} in {self.log_sfx(region)}.")
    self._observed_elasticache_clusters.increment()

    properties = {
      "Tags": tags,
      "ElastiCacheEngineVersion": engine_version,
      "ElastiCacheEngineType": engine_type,
      "ElastiCacheEncryptionAtRestEnabled": at_rest_encryption,
      "ElastiCacheEncryptionAtTransitEnabled": transit_encryption,
    }

    return Resource(
      cluster_id,
      Arn(arn),
      cluster["CacheClusterCreateTime"],
      cluster["CacheClusterStatus"],
      properties,
    )


class DAXScanner(ResourceScanner):
  def __init__(self, account_id: str, authenticator: AwsAuthenticator):
    self.entities_key = "Clusters"
    self.scan_method = "describe_clusters"
    self.service = "dax"
    super().__init__(account_id, authenticator, CompletionQueue())
    self._observed_clusters = self.metrics.register(AtomicGauge("observed_clusters"))

  def retention(self, client, cluster_name: str, region: str, parameter_group_name: str) -> dict:
    try:
      parameter_group = client.describe_parameters(
        ParameterGroupName=parameter_group_name, MaxResults=100, Source="user"
      )
      for parameter in parameter_group.get("Parameters", []):
        if "query-ttl-millis" == parameter["ParameterName"]:
          query_ttl_millis = int(parameter["ParameterValue"])
        elif "record-ttl-millis" == parameter["ParameterName"]:
          record_ttl_millis = int(parameter["ParameterValue"])

      if all((query_ttl_millis, record_ttl_millis)):
        # If Item Cache is 0, then retention is infinite.
        if record_ttl_millis == 0:
          retention_in_days = 0
        else:
          retention_in_days = math.ceil(max(query_ttl_millis, record_ttl_millis) / 86400000)

        return {
          "RetentionInDays": retention_in_days,
          "ItemCacheTtlInMs": record_ttl_millis,
          "QueryCacheTtlInMs": query_ttl_millis,
        }
      else:
        raise AwsMissingTTL(
          "Could not find query-ttl-millis and record-ttl-millis in Parameter Group {parameter_group_name}"
        )
    except Exception as ex:
      self._process_retention_errors.increment()
      log.error(
        f"unable to list {self.service} ttl status for {cluster_name} in {self.log_sfx(region)}. err={ex}"
      )

  def handler(self, client, region: str, cluster: Dict[str, Any]) -> Resource:
    arn = cluster["ClusterArn"]
    name = cluster["ClusterName"]
    encryption_status = "Unknown"
    if "SSEDescription" in cluster.keys() and "Status" in cluster["SSEDescription"].keys():
      if cluster["SSEDescription"]["Status"] == "ENABLED":
        encryption_status = "Enabled"
      elif cluster["SSEDescription"]["Status"] == "DISABLED":
        encryption_status = "Disabled"

    tags = []
    if not ServerState().options.get("disable_tag_queries"):
      tags = response_tags(client.list_tags(ResourceName=arn))
    properties = {
      "EncryptionStatus": encryption_status,
      "Tags": tags,
    }

    if not ServerState().options.get("disable_retention_processing"):
      retention = self.retention(
        client, name, region, cluster["ParameterGroup"]["ParameterGroupName"]
      )
      if retention:
        properties["Retention"] = retention

    if ServerState().options.get("log_resource_observations"):
      log.info(f"Observed resource: {arn} in {self.log_sfx(region)}.")

    return Resource(
      name,
      Arn(arn),
      datetime.min,  # hardcode the creation time as it is not exposed via the API
      "ACTIVE",  # hardcode the status as the one exposed via the API does not appear to be useful
      properties,
    )


class DynamoDbScanner(ResourceScanner):
  def __init__(self, account_id: str, authenticator: AwsAuthenticator):
    self.entities_key = "TableNames"
    self.scan_method = "list_tables"
    self.service = "dynamodb"
    super().__init__(account_id, authenticator, CompletionQueue())
    self._observed_tables = self.metrics.register(AtomicGauge("observed_tables"))

  def retention_enabled(self, client, name: str, region: str) -> Optional[Dict[str, str]]:
    try:
      resp = client.describe_time_to_live(TableName=name)
      if "TimeToLiveDescription" in resp.keys():
        if (
          "TimeToLiveStatus" in resp["TimeToLiveDescription"].keys()
          and resp["TimeToLiveDescription"]["TimeToLiveStatus"] == "ENABLED"
        ):
          return True
      return False
    except Exception as ex:
      self._process_retention_errors.increment()
      log.error(
        f"unable to list {self.service} ttl status for {name} in {self.log_sfx(region)}. err={ex}"
      )

  def handler(self, client, region: str, resource_name: str) -> Resource:
    table = client.describe_table(TableName=resource_name)["Table"]
    arn = table["TableArn"]
    name = table["TableName"]
    tags = []
    if not ServerState().options.get("disable_tag_queries"):
      tags = response_tags(client.list_tags_of_resource(ResourceArn=arn))

    if ServerState().options.get("log_resource_observations"):
      log.info(f"Observed resource: {arn} in {self.log_sfx(region)}.")
    self._observed_tables.increment()

    properties = {
      "EncryptionStatus": "Enabled",  # All DynamoDB tables have SSE enabled by default
      "Fields": [a.get("AttributeName") for a in table.get("AttributeDefinitions")],
      "ItemCount": table.get("ItemCount"),
      "TableSizeBytes": table.get("TableSizeBytes"),
      "Tags": tags,
    }

    if not ServerState().options.get("disable_retention_processing"):
      retention_enabled = self.retention_enabled(client, name, region)
      if retention_enabled is not None:
        properties["Retention"] = {"RetentionEnabled": retention_enabled}

    return Resource(name, Arn(arn), table["CreationDateTime"], table["TableStatus"], properties)


class KinesisScanner(ResourceScanner):
  def __init__(self, account_id: str, authenticator: AwsAuthenticator):
    self.entities_key = "StreamNames"
    self.scan_method = "list_streams"
    self.service = "kinesis"
    super().__init__(account_id, authenticator, CompletionQueue())
    self._observed_streams = self.metrics.register(AtomicGauge("observed_streams"))

  def encryption_status(self, encryption_type: str) -> Dict[str, str]:
    if encryption_type == "NONE":
      return {"EncryptionStatus": "Disabled"}
    else:
      return {"EncryptionStatus": "Enabled", "EncryptionType": encryption_type}

  def handler(self, client, region: str, resource_name: str) -> Resource:
    stream = client.describe_stream_summary(StreamName=resource_name)["StreamDescriptionSummary"]
    arn = stream["StreamARN"]
    tags = []
    if not ServerState().options.get("disable_tag_queries"):
      tags = response_tags(client.list_tags_for_stream(StreamName=resource_name))

    if ServerState().options.get("log_resource_observations"):
      log.info(f"Observed resource: {arn} in {self.log_sfx(region)}.")
    self._observed_streams.increment()

    encryption_status = self.encryption_status(stream["EncryptionType"])
    retention_in_days = math.ceil(int(stream["RetentionPeriodHours"]) / 24)
    properties = {
      "Retention": {"RetentionInDays": retention_in_days},
      "Tags": tags,
    }
    return Resource(
      stream["StreamName"],
      Arn(arn),
      stream["StreamCreationTimestamp"],
      stream["StreamStatus"],
      {**encryption_status, **properties},
    )


class S3Scanner(ResourceScanner):
  _SCAN_REGION = "us-west-2"

  def __init__(self, account_id: int, authenticator: AwsAuthenticator):
    self._EXEMPT_SCANNER_ATTRS = ["scan_method"]
    self.entities_key = "Buckets"
    self.service = "s3"
    super().__init__(account_id, authenticator, CompletionQueue())
    self._observed_buckets = self.metrics.register(AtomicGauge("observed_buckets"))

  def encryption_status(self, client, name: str) -> Dict[str, str]:
    try:
      resp = client.get_bucket_encryption(Bucket=name)
      if "ServerSideEncryptionConfiguration" in resp.keys():
        if "Rules" in resp["ServerSideEncryptionConfiguration"].keys():
          for rule in resp["ServerSideEncryptionConfiguration"]["Rules"]:
            if "ApplyServerSideEncryptionByDefault" in rule.keys():
              if "SSEAlgorithm" in rule["ApplyServerSideEncryptionByDefault"].keys():
                encryption_type = rule["ApplyServerSideEncryptionByDefault"]["SSEAlgorithm"]
                if encryption_type == "aws:kms":
                  encryption_type = "KMS"
                return {
                  "EncryptionStatus": "Enabled",
                  "EncryptionType": encryption_type,
                }
      return {"EncryptionStatus": "Unknown"}
    except ClientError as ex:
      if ex.response["Error"]["Code"] == "ServerSideEncryptionConfigurationNotFoundError":
        return {"EncryptionStatus": "Disabled"}
      else:
        raise ex

  def retention(self, client, name: str, region: str) -> Optional[int]:
    try:
      resp = client.get_bucket_lifecycle_configuration(Bucket=name)
      if "Rules" in resp.keys():
        for rule in resp["Rules"]:
          if "Status" in rule.keys() and rule["Status"] == "Enabled":
            # Only evaluate lifecycle rules with a scope of 'Entire bucket' which
            # are represented in the API as rules w/ an empty filter prefix value.
            if (
              "Filter" in rule.keys()
              and "Prefix" in rule["Filter"].keys()
              and rule["Filter"]["Prefix"] == ""
            ):
              if "Expiration" in rule.keys() and "Days" in rule["Expiration"].keys():
                return rule["Expiration"]["Days"]
    except ClientError as ex:
      code = ex.response["Error"]["Code"]
      if code in ("NoSuchBucket", "NoSuchLifecycleConfiguration"):
        log.debug(
          f"No Lifecycle configuration set on bucket {name} in {self.log_sfx(region)}. err={ex}"
        )
      else:
        raise ex
    except Exception as ex:
      self._process_retention_errors.increment()
      log.error(
        f"unable to list {self.service} lifecycle rules for {name} in {self.log_sfx(region)}. err={ex}"
      )

  def scan(self):
    # The S3 API doesn't support regional resource listing or pagination requiring
    # the parent class scan method to be overridden.
    try:
      self.validate()
      client = self.authenticator.new_client(self.service, self._SCAN_REGION)
      resp = client.list_buckets()
      if self.entities_key in resp.keys():
        for entity in resp[self.entities_key]:
          # the S3 ListBuckets response data-structure differs from the expected behaivor
          # within the parent scanner class.
          name = entity["Name"]

          try:
            region = client.get_bucket_location(Bucket=name)["LocationConstraint"]
            # S3 buckets that return a `null` `LocationConstraint` value are located in `us-east-1`
            # as-per the docs and AWS support (Account ID: 673964658973, Support Case ID: 8246504281)
            # https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLocation.html#API_GetBucketLocation_ResponseSyntax
            if region is None:
              region = "us-east-1"
            elif region == "EU":
              region = "eu-west-1"

            resource = self.handler(client, region, entity)
            self.snk.put(resource)
            self.counter += 1
          except ClientError as ex:
            if ex.response["Error"]["Code"] != "NoSuchBucket":
              self._process_resource_errors.increment()
              log.error(
                f"unable to process {self.service} resource {name} in {self.log_sfx(self._SCAN_REGION)}. err={ex}"
              )
            continue

        self._regions_scanned.increment()
    except ClientError as ex:
      log.error(
        f"unable to list {self.service} resources in {self.log_sfx(self._SCAN_REGION)}. err={ex}"
      )
      self._list_resources_errors.increment()
    finally:
      log.info(
        "{} scan complete ({} resources). {}".format(
          self.__class__.__name__, self.counter, self.log_sfx()
        )
      )
      self.snk.set_completed()

  def handler(self, client, region: str, entity: Dict[str, Any]) -> Resource:
    name = entity["Name"]
    tags = []
    if not ServerState().options.get("disable_tag_queries"):
      try:
        tags = response_tags(client.get_bucket_tagging(Bucket=name), "TagSet")
      except ClientError as ex:
        if ex.response["Error"]["Code"] != "NoSuchTagSet":
          raise ex

    arn = S3Arn(name, self.account_id, region)
    if ServerState().options.get("log_resource_observations"):
      log.info(f"Observed resource: {arn.arn} in {self.log_sfx(region)}.")
    self._observed_buckets.increment()

    properties = self.encryption_status(client, name)
    properties["Tags"] = tags
    if not ServerState().options.get("disable_retention_processing"):
      retention = self.retention(client, name, region)
      if retention:
        properties["Retention"] = {"RetentionInDays": retention}

    return Resource(
      arn.resource,
      arn,
      entity["CreationDate"],
      "ACTIVE",  # hardcode the status as S3 doesn't expose one via the API
      properties,
    )


class SqsScanner(ResourceScanner):
  def __init__(self, account_id: int, authenticator: AwsAuthenticator):
    self.entities_key = "QueueUrls"
    self.scan_method = "list_queues"
    self.service = "sqs"
    super().__init__(account_id, authenticator, CompletionQueue())
    self._observed_queues = self.metrics.register(AtomicGauge("observed_queues"))

  def handler(self, client, region: str, resource_name: str) -> Resource:
    queue = client.get_queue_attributes(
      QueueUrl=resource_name,
      AttributeNames=[
        "CreatedTimestamp",
        "KmsMasterKeyId",
        "LastModifiedTimestamp",
        "MessageRetentionPeriod",
        "QueueArn",
      ],
    )

    encryption_status = {"EncryptionStatus": "Disabled"}
    if "KmsMasterKeyId" in queue["Attributes"] and queue["Attributes"]["KmsMasterKeyId"]:
      encryption_status["EncryptionStatus"] = "Enabled"
      encryption_status["EncryptionType"] = "KMS"

    # SQS tags are in a different format than other resource types.
    # Empty tag responses omit the `Tags` map key which also differs from other resource APIs.
    # Convert SQS tag structure before assinging them to the `Resource` class so they can be
    # processed uniformly during the registration stage.
    tags = []
    if not ServerState().options.get("disable_tag_queries"):
      queue_tags = client.list_queue_tags(QueueUrl=resource_name)
      if "Tags" in queue_tags.keys():
        for key, value in queue_tags["Tags"].items():
          tags.append(
            {
              "Key": key,
              "Value": value,
            }
          )

    arn = Arn(queue["Attributes"]["QueueArn"])
    if ServerState().options.get("log_resource_observations"):
      log.info(f"Observed resource: {arn.arn} in {self.log_sfx(region)}.")
    self._observed_queues.increment()

    # Convert retention time from seconds to days because DAL stores retention in days
    retention_in_days = math.ceil(int(queue["Attributes"]["MessageRetentionPeriod"]) / 86400)

    properties = {
      "LastModified": datetime.fromtimestamp(int(queue["Attributes"]["LastModifiedTimestamp"])),
      "Retention": {"RetentionInDays": retention_in_days},
      "Tags": tags,
      "URL": resource_name,
    }
    return Resource(
      arn.resource,
      arn,
      datetime.fromtimestamp(int(queue["Attributes"]["CreatedTimestamp"])),
      "ACTIVE",  # hardcode the status as SQS doesn't expose one via the API
      {**encryption_status, **properties},
    )


def get_all_resources(account_id: str, snk: CompletionQueue, *scanners: ResourceScanner):
  stage = f"{account_id}.scan"
  ServerState().start_stage(stage)

  futures = []
  workers = len(scanners) * 2
  try:
    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as ex:
      for scanner in scanners:
        futures.append(ex.submit(copy_completion_queue, scanner.snk, snk))
        futures.append(ex.submit(scanner.scan))

      for future in concurrent.futures.as_completed(futures, timeout=FUTURE_TIMEOUTS["scanner"]):
        _ = future.result()
  except Exception as ex:
    log.exception(f"`get_all_resources` exception: {ex}")
    raise ex
  finally:
    snk.set_completed()
    ServerState().complete_stage(stage)
