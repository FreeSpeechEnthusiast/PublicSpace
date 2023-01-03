from base64 import b64decode
from binascii import Error
import json
from json.decoder import JSONDecodeError
from threading import Event
from time import time
from typing import Any, Dict, List, Optional, Tuple, Type, Union

from twitter.common import log
from twitter.common.metrics import AtomicGauge, Observable
from util import CompletionQueue, tag_value

from com.twitter.dal.dataclassification.ttypes import DataClassificationLevel
from com.twitter.dal.has_personal_data.ttypes import HasPersonalData
from com.twitter.dal.model.ttypes import DatasetName, SegmentType, StorageType, URL
from com.twitter.dal.properties.ttypes import (
  LogicalDatasetPropertyKey,
  LogicalDatasetPropertyValue,
  OwnerAccountValueType,
)
from com.twitter.dal.schema.ttypes import GenericSchema, GenericSchemaField, Schema, UnknownSchema
from com.twitter.statebird.v2.thriftpython.ttypes import Environment

from aws_iam import IamObservedAccess
from aws_kite_role import build_kite_role
from aws_resource import Resource
from dataset_attribution import resource_owner
from dataset_metastore import Metastore
from kite_role import KiteRole
from server_config import REGISTRATION_EXCLUSION_MIN_INTERVAL, SVC_ROLE
from server_state import ServerState


class Dataset:
  def __init__(self, resource: Resource, accesses: List[IamObservedAccess]):
    self.accesses = accesses
    self.dal_name = resource.arn.dal_name()
    self.env = Environment.PROD
    self.has_meta = Event()
    self.meta = None
    self.resource = resource

  def __str__(self) -> str:
    return "{}(name={}, resource={}, accesses={}, meta={})".format(
      self.__class__.__name__, self.dal_name, self.resource, len(self.accesses), self.meta
    )

  def name(self) -> DatasetName:
    return DatasetName(
      environment=self.env, name=self.dal_name, role=SVC_ROLE["prod"]
    )  # dmarwick has asked for the svc to use a single role for both envs

  def annotations(self) -> Optional[Union[Dict[str, List[str]], List[str]]]:
    if self.has_meta.is_set() and "annotations" in self.meta.keys():
      return self.meta["annotations"]

    # check for `pdp_annotations` resource tag if meta value not set
    if "Tags" in self.resource.properties.keys():
      value = tag_value(self.resource.properties["Tags"], "pdp_annotations")
      if value:
        try:
          return json.loads(b64decode(value))
        except (Error, JSONDecodeError, UnicodeDecodeError) as ex:
          log.error(
            "unable to parse `pdp_annotations` tag value for resource: {}, value: {}. ex={}".format(
              self.resource, value, ex
            )
          )

  def records_classes(self) -> Optional[List[str]]:
    if self.has_meta.is_set() and "records_classes" in self.meta.keys():
      return self.meta["records_classes"]

    if "Tags" in self.resource.properties.keys():
      value = None
      for t in ("pdp_records_classes", "pdp_records_class"):
        value = tag_value(self.resource.properties["Tags"], t)
        if value:
          break

      if value:
        try:
          return json.loads(b64decode(value))
        except (Error, JSONDecodeError, UnicodeDecodeError) as ex:
          log.error(
            "unable to parse `pdp_records_classes` tag value for resource: {}, value: {}. ex={}".format(
              self.resource, value, ex
            )
          )

  @property
  def retention(self) -> Optional[Dict[LogicalDatasetPropertyKey, LogicalDatasetPropertyValue]]:
    if "Retention" in self.resource.properties.keys():
      retention = self.resource.properties["Retention"]

      if not retention:
        return

      svc = self.resource.arn.service
      if svc == "dynamodb":
        if (
          self.has_meta.is_set()
          and "retention" in self.meta.keys()
          and retention.get("RetentionEnabled")
        ):
          return {
            LogicalDatasetPropertyKey.AmazonDynamoDbRetentionTtlInDays: LogicalDatasetPropertyValue(
              amazonDynamoDbRetentionTtlInDays=int(self.meta["retention"])
            )
          }
      elif svc == "dax":
        return {
          LogicalDatasetPropertyKey.DaxRetentionInDays: LogicalDatasetPropertyValue(
            daxRetentionInDays=retention["RetentionInDays"]
          ),
          LogicalDatasetPropertyKey.DaxQueryCacheTtlInMs: LogicalDatasetPropertyValue(
            daxQueryCacheTtlInMs=retention["QueryCacheTtlInMs"]
          ),
          LogicalDatasetPropertyKey.DaxItemCacheTtlInMs: LogicalDatasetPropertyValue(
            daxItemCacheTtlInMs=retention["ItemCacheTtlInMs"]
          ),
        }
      elif svc == "kinesis":
        return {
          LogicalDatasetPropertyKey.AmazonKinesisRetentionTtlInDays: LogicalDatasetPropertyValue(
            amazonKinesisRetentionTtlInDays=retention["RetentionInDays"]
          )
        }
      elif svc == "s3":
        return {
          LogicalDatasetPropertyKey.AmazonS3RetentionTtlInDays: LogicalDatasetPropertyValue(
            amazonS3RetentionTtlInDays=retention["RetentionInDays"]
          )
        }
      elif svc == "sqs":
        return {
          LogicalDatasetPropertyKey.AmazonSQSRetentionTtlInDays: LogicalDatasetPropertyValue(
            amazonSQSRetentionTtlInDays=retention["RetentionInDays"]
          )
        }
      elif svc == "elasticache":
        return {
          LogicalDatasetPropertyKey.AmazonElastiCacheRetentionTtlInDays: LogicalDatasetPropertyValue(
            amazonElastiCacheRetentionTtlInDays=retention["RetentionInDays"]
          )
        }

  def contains_pii(self) -> Optional[HasPersonalData]:
    if self.has_meta.is_set() and "contains_pii" in self.meta.keys():
      if self.meta["contains_pii"] == True:
        return HasPersonalData.YES_PERSONAL_DATA
      elif self.meta["contains_pii"] == False:
        return HasPersonalData.NO_PERSONAL_DATA

    # check for `pdp_pii` resource tag if meta value not set
    if "Tags" in self.resource.properties.keys():
      value = tag_value(self.resource.properties["Tags"], "pdp_pii")
      if value:
        value = value.lower()
        if value in ("1", "true"):
          return HasPersonalData.YES_PERSONAL_DATA
        elif value in ("0", "false"):
          return HasPersonalData.NO_PERSONAL_DATA

    if self.annotations():
      return HasPersonalData.YES_PERSONAL_DATA

  def data_classification_level(self) -> Optional[DataClassificationLevel]:
    if self.has_meta.is_set() and self.meta.get("data_classification_level") is not None:
      level = self.meta["data_classification_level"].lower()
      if level == "cornerstone":
        return DataClassificationLevel.CORNERSTONE_DATA
      elif level == "high_sensitivity":
        return DataClassificationLevel.HIGH_SENSITIVITY_DATA
      elif level == "medium_sensitivity":
        return DataClassificationLevel.MEDIUM_SENSITIVITY_DATA
      elif level == "low_sensitivity":
        return DataClassificationLevel.LOW_SENSITIVITY_DATA

  def properties(self) -> Dict[str, Union[SegmentType, StorageType, URL]]:
    svc = self.resource.arn.service
    if svc == "dax":
      storage_type = StorageType("DAX")
    elif svc == "dynamodb":
      storage_type = StorageType("DynamoDB")
    elif svc == "kinesis":
      storage_type = StorageType("Kinesis")
    elif svc == "s3":
      storage_type = StorageType("S3")
    elif svc == "sqs":
      storage_type = StorageType("SQS")
    elif svc == "elasticache":
      storage_type = StorageType("ElastiCache")
    return {
      "segment_type": SegmentType.SNAPSHOT,
      "storage_type": storage_type,
      "URL": URL(f"aws://{self.resource.arn.arn}"),
    }

  # deprecated
  def owner(self) -> Tuple[str, OwnerAccountValueType]:
    if self.has_meta.is_set() and "team" in self.meta.keys():
      owner = self.meta["team"].upper()
    else:
      owner = resource_owner(
        self.resource.arn.account_id, self.resource.arn.resource, self.resource.properties
      )

    return owner, OwnerAccountValueType.TeamCode

  def project(self) -> Optional[str]:
    if self.has_meta.is_set() and "project" in self.meta.keys():
      return self.meta["project"]

    # check for `pdp_project` resource tag if meta value not set
    if "Tags" in self.resource.properties.keys():
      value = tag_value(self.resource.properties["Tags"], "pdp_project")
      if value:
        return value

    if self.resource.arn.account_id in ServerState().meta["account_default_projects"].keys():
      return ServerState().meta["account_default_projects"][self.resource.arn.account_id]

  def kite_role(self) -> Optional[Type[KiteRole]]:
    return build_kite_role(self.resource.arn, self.project())

  def schema(self) -> Schema:
    fields = {}
    if self.has_meta.is_set() and "schema" in self.meta.keys():
      fields = self.meta["schema"]
    # check for `pdp_schema` resource tag if meta value not set
    elif "Tags" in self.resource.properties.keys():
      value = tag_value(self.resource.properties["Tags"], "pdp_schema")
      if value:
        try:
          fields = json.loads(b64decode(value))
        except (Error, JSONDecodeError, UnicodeDecodeError) as ex:
          log.error(
            "unable to parse `pdp_schema` tag value for resource: {}, value: {}. ex={}".format(
              self.resource, value, ex
            )
          )

    if not fields:
      return Schema(unknown=UnknownSchema())

    dataset_annotations = self.annotations()

    schema = {field: frozenset() for field in fields}
    if isinstance(dataset_annotations, dict):
      schema.update(dataset_annotations)

    dal_fields = [
      GenericSchemaField(fieldName=field, personalDataTypes=frozenset(pdts))
      for field, pdts in schema.items()
    ]
    has_personal_data = self.contains_pii()

    return Schema(genericSchema=GenericSchema(fields=dal_fields, hasPersonalData=has_personal_data))

  def set_meta(self, meta: Dict[str, Any]):
    if meta is None:
      return

    # as-per dmarwick and swei DAL datasets should be registered using a `Prod` env until
    # EagleEye is updated to display and support `Dev` datasets.

    # if 'environment' in meta.keys():
    #   env = meta['environment'].lower()
    #   if env == 'dev':
    #     self.env = Environment.DEV
    #   elif env == 'staging':
    #     self.env = Environment.STAGING

    self.meta = meta
    self.has_meta.set()


class DatasetFilter(Observable):
  def __init__(self, metastore: Metastore, snk: CompletionQueue):
    self.metastore = metastore
    self.snk = snk
    self._emitted = self.metrics.register(AtomicGauge("emitted"))
    self._excluded = self.metrics.register(AtomicGauge("excluded"))

  def process(self, resource: Resource):
    record = self.metastore.get_regional_or_global(resource.arn)
    if record and "registered" in record.keys():
      registered = int(record["registered"])
      if not ServerState().options.get("disable_resource_filter"):
        now = time()
        refresh_at = record.get("refresh_at")
        if REGISTRATION_EXCLUSION_MIN_INTERVAL > (now - registered) or (
          refresh_at and int(refresh_at) > now
        ):
          log.info(f"excluding recently registered dataset: {resource.arn.arn} - {registered}")
          self._excluded.increment()
          return

    self._emitted.increment()
    self.snk.put(resource)
