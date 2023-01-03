from time import time
from typing import Any, Dict, Optional

from aws_arn import Arn
from aws_iam import AwsAuthenticator
from datastore import Datastore
from server_config import DYNAMO_METASTORE_TABLE_NAME, INIT_TS_FIELD_PFX
from server_state import ServerState


def should_set_init_field(field: str, record: Dict[str, Any]) -> bool:
  if field in ("created_at", "refresh_at"):
    return False

  init_field = INIT_TS_FIELD_PFX + field
  ts_fields = [init_field, field]
  if field == "observed":
    for f in (INIT_TS_FIELD_PFX + "registered", "registered"):
      ts_fields.append(f)
  for k in record.keys():
    if k in ts_fields:
      return False

  return True


class Metastore(Datastore):
  def __init__(self, auth: AwsAuthenticator):
    super().__init__(auth, DYNAMO_METASTORE_TABLE_NAME)
    self.supplemental_fields = [
      "annotations",
      "contains_pii",
      "project",
      "records_classes",
      "schema",
    ]

  # It is a common use-case to have N resources defined in different regions
  # with the same name to support regional deployments. The registration service
  # will reuse the same dataset metadata for each regional resource by default.
  # In the event that a resource shares a common name as other regional resources
  # but has a different schema/pdp metadata an individual record can be added
  # to the dynamodb table with an ID of `region/service/resource_name`.
  # `get_regional_or_global` checks for a regional record first and then fetches
  # a global record if none exist to support this use-case.
  def get_regional_or_global(self, arn: Arn) -> Optional[Dict[str, Any]]:
    k = self.key(arn)
    if arn.region:
      regional_record = self.get("/".join([arn.region, k]))
      if regional_record and any(f in regional_record for f in self.supplemental_fields):
        return regional_record

    return self.get(k)

  def key(self, arn: Arn, regional: bool = False) -> str:
    if regional:
      return "/".join([arn.region, arn.service, arn.resource])
    else:
      return "/".join([arn.service, arn.resource])

  def set_timestamp(self, key: str, field: str, value: Optional[int] = None):
    if ServerState().options.get("dry_run"):
      return

    init_field = INIT_TS_FIELD_PFX + field
    if not value:
      value = int(time())
    record = self.get(key)
    if record:
      update_expression = f"SET {field} = :value"
      if should_set_init_field(field, record):
        update_expression = update_expression + f", {init_field} = :value"
      self.table.update_item(
        Key={"ID": key},
        ExpressionAttributeValues={":value": value},
        UpdateExpression=update_expression,
      )
    else:
      record = {"ID": key, field: value}
      if should_set_init_field(field, record):
        record[init_field] = value
      self.table.put_item(Item=record)

  def set_created_at(self, key: str, ts: int):
    self.set_timestamp(key, "created_at", ts)

  def set_observed(self, key: str):
    self.set_timestamp(key, "observed")

  def set_refresh_at(self, key: str, ts: int):
    self.set_timestamp(key, "refresh_at", ts)

  def set_registered(self, key: str):
    self.set_timestamp(key, "registered")
