from datetime import datetime
from typing import Dict, List, Union

from aws_arn import Arn
from errors import UnknownResourceStatus
from server_config import RESOURCE_STATUSES


class Resource:
  def __init__(
    self,
    name: str,
    arn: Arn,
    creation_date: datetime,
    status: str,
    properties: Dict[str, Union[str, int, List[str], List[Dict[str, str]]]],
  ):
    if not status or status not in RESOURCE_STATUSES.get(arn.service):
      raise UnknownResourceStatus(arn.service, status)
    self.arn = arn
    self.creation_date = creation_date
    self.status = status
    self.properties = properties

  def normalized_status(self) -> str:
    if self.arn.service == "dynamodb":
      if self.status in ("ARCHIVING", "ARCHIVED", "DELETING"):
        return "INACTIVE"
    elif self.arn.service == "kinesis":
      if self.status == "DELETING":
        return "INACTIVE"
    return "ACTIVE"

  def __str__(self) -> str:
    return "{}(arn={}, created={}, status={}, properties={})".format(
      self.__class__.__name__, self.arn.arn, self.creation_date, self.status, self.properties
    )


def resources_by_service(src_resources: List[Resource]) -> Dict[str, List[Resource]]:
  resources = {}
  for resource in src_resources:
    svc = resource.arn.service
    if svc not in resources.keys():
      resources[svc] = []
    resources[svc].append(resource)

  return resources
