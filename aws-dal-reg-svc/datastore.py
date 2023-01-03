from typing import Any, Dict, Optional

from boto3 import dynamodb
from server_config import DYNAMO_STAGES_TABLE_NAME, DYNAMO_SVC_TABLE_REGION
from server_state import ServerState


# this method should only be used for testing w/ a local dynamodb server.
# real tables should be created via terraform (or c21n for PSCP).
def create_table(name: str, rsrc: "dynamodb.ServiceResource"):
  if name == DYNAMO_STAGES_TABLE_NAME:
    rsrc.create_table(
      TableName=name,
      AttributeDefinitions=[
        {
          "AttributeName": "stage",
          "AttributeType": "S",
        },
        {
          "AttributeName": "account_id",
          "AttributeType": "S",
        },
      ],
      BillingMode="PAY_PER_REQUEST",
      KeySchema=[
        {
          "AttributeName": "stage",
          "KeyType": "HASH",
        },
        {
          "AttributeName": "account_id",
          "KeyType": "RANGE",
        },
      ],
    )
  else:
    rsrc.create_table(
      TableName=name,
      AttributeDefinitions=[
        {
          "AttributeName": "ID",
          "AttributeType": "S",
        },
      ],
      BillingMode="PAY_PER_REQUEST",
      KeySchema=[
        {
          "AttributeName": "ID",
          "KeyType": "HASH",
        },
      ],
    )


class Datastore:
  def __init__(self, auth, table_name: str):
    rsrc = auth.new_resource("dynamodb", DYNAMO_SVC_TABLE_REGION)
    if (
      ServerState().options.get("local_dynamodb_create")
      and ServerState().options.get("local_dynamodb_endpoint")
      and table_name not in [t.table_name for t in rsrc.tables.all()]
    ):
      create_table(table_name, rsrc)
    self.table = rsrc.Table(table_name)

  def delete(self, key: str) -> Dict[str, Any]:
    return self.table.delete_item(Key={"ID": key})

  def exists(self, key: str) -> bool:
    return "Item" in self.table.get_item(Key={"ID": key})

  def get(self, key: str) -> Optional[Dict[str, Any]]:
    return self.table.get_item(Key={"ID": key}).get("Item")

  def put(self, key: str, val: Dict[str, Any]):
    return self.table.put_item(Item={**{"ID": key}, **val})
