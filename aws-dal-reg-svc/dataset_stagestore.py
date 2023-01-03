from time import time
from typing import Dict, Optional, Union

from aws_iam import AwsAuthenticator
from datastore import Datastore
from server_config import DYNAMO_STAGES_TABLE_NAME, DYNAMO_STAGES_TABLE_TTL


class Stagestore(Datastore):
  def __init__(self, auth: AwsAuthenticator):
    super().__init__(auth, DYNAMO_STAGES_TABLE_NAME)

  def delete(self, key: str):
    raise NotImplementedError

  def exists(self, key: str):
    raise NotImplementedError

  def get(self, key: str) -> Optional[Dict[str, Union[str, int]]]:
    return self.table.get_item(Key=self.key(key)).get("Item")

  def key(self, key: str) -> Dict[str, str]:
    account_id, stage = key.split(".")
    return {"account_id": account_id, "stage": stage}

  def put(self, key: str):
    return self.table.put_item(Item=self.record(key))

  def record(self, key: str) -> Dict[str, Union[str, int]]:
    return {
      **self.key(key),
      **{"expire": int(time() + DYNAMO_STAGES_TABLE_TTL), "ts": int(time())},
    }
