import re
from typing import Optional, Type

from twitter.common import log

from aws_arn import Arn
from kite_role import KiteRole
from server_config import KITE_PROJECT_NAME_PATTERN
from server_state import ServerState


def build_kite_role(arn: Arn, project: Optional[str]) -> Optional[Type[KiteRole]]:
  """
  Construct Kite role with information given and the kite roles supported by current service.
  Args:
    arn: the ARN of the resource.
    project: the Kite project name owning the resource.
  """
  if not project:
    log.info(f"Kite project not found for {arn}: {arn.kite_name()}.")
    return
  if not re.match(KITE_PROJECT_NAME_PATTERN, project, re.IGNORECASE):
    log.error(
      f"Supressing configured Kite project value due to unsupported character set for {arn}, value: {project}"
    )
    return
  if ServerState().options.get("disable_kite"):  # CAT-2699
    return

  if arn.service == "dax" and arn.resource_type == "cache":
    return DAXKiteRole(arn.kite_name(), project)
  elif arn.service == "dynamodb" and arn.resource_type == "table":
    return DynamoDbKiteRole(arn.kite_name(), project)
  elif arn.service == "kinesis" and arn.resource_type == "stream":
    return KinesisStreamKiteRole(arn.kite_name(), project)
  elif arn.service == "s3":  # S3 ARNs do not contain a resource type
    return S3BucketKiteRole(arn.kite_name(), project)
  elif arn.service == "sqs":  # SQS ARNs do not contain a resource type
    return SqsQueueKiteRole(arn.kite_name(), project)
  elif arn.service == "elasticache":
    return ElastiCacheClusterKiteRole(arn.kite_name(), project)


class DAXKiteRole(KiteRole):
  """
  Wrapper class that provides resource_type and infrastructure_service for DAX clusters.
  """

  def __init__(self, name: str, kite_project: str):
    super().__init__(name, "aws_dax_cache", "aws_dax", kite_project)


class DynamoDbKiteRole(KiteRole):
  """
  Wrapper class that provides resource_type and infrastructure_service for DynamoDB tables.
  """

  def __init__(self, name: str, kite_project: str):
    super().__init__(name, "aws_dynamodb_table", "aws_dynamodb", kite_project)


class KinesisStreamKiteRole(KiteRole):
  """
  Wrapper class that provides resource_type and infrastructure_service for Kenisis streams.
  """

  def __init__(self, name: str, kite_project: str):
    super().__init__(name, "aws_kinesis_stream", "aws_kinesis", kite_project)


class S3BucketKiteRole(KiteRole):
  """
  Wrapper class that provides resource_type and infrastructure_service for S3 buckets.
  """

  def __init__(self, name: str, kite_project: str):
    super().__init__(name, "aws_s3_bucket", "aws_s3", kite_project)


class SqsQueueKiteRole(KiteRole):
  """
  Wrapper class that provides resource_type and infrastructure_service for SQS queues.
  """

  def __init__(self, name: str, kite_project: str):
    super().__init__(name, "aws_sqs_queue", "aws_sqs", kite_project)


class ElastiCacheClusterKiteRole(KiteRole):
  """
  Wrapper class that provides resource_type and infrastructure_service for ElastiCache clusters.
  """

  def __init__(self, name: str, kite_project: str):
    super().__init__(name, "aws_elasticache", "aws_elasticache", kite_project)
