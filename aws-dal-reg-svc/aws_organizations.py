from typing import Dict, List, Optional, Union

from util import response_tags, tag_value

from aws_iam import AwsAuthenticator
from botocore.exceptions import ClientError
from server_config import GLOBAL_API_REGION
from server_state import ServerState


def get_accounts(auth: AwsAuthenticator, exclude_ous: bool = True) -> List[str]:
  process_account = ServerState().options.get("process_account")
  if process_account:
    return [process_account]
  else:
    return list_org_accounts(auth)


def get_account_tag(auth: AwsAuthenticator, account_id: str, tag: str) -> Optional[str]:
  try:
    return tag_value(response_tags(get_account_tags(auth, account_id)), tag)
  except ClientError as ex:
    if ex.response["Error"]["Code"] not in ("InvalidInputException", "TargetNotFoundException"):
      raise ex


def get_account_tags(
  auth: AwsAuthenticator, account_id: str
) -> Dict[str, Union[List[Dict[str, str]], str]]:
  client = auth.new_client("organizations", region=GLOBAL_API_REGION)
  return client.list_tags_for_resource(ResourceId=account_id)


def list_org_accounts(auth: AwsAuthenticator) -> List[str]:
  accounts = []
  client = auth.new_client("organizations", region=GLOBAL_API_REGION)
  for resp in client.get_paginator("list_accounts").paginate():
    if "Accounts" in resp.keys():
      for account in resp["Accounts"]:
        if account["Status"] == "ACTIVE":
          accounts.append(account["Id"])

  return accounts


def list_ou_accounts(auth: AwsAuthenticator, ou: str) -> List[str]:
  accounts = []
  client = auth.new_client("organizations", region=GLOBAL_API_REGION)
  for resp in client.get_paginator("list_children").paginate(ParentId=ou, ChildType="ACCOUNT"):
    if "Children" in resp.keys():
      for child in resp["Children"]:
        if child["Type"] == "ACCOUNT":
          accounts.append(child["Id"])

  return accounts


def create_policy(
  auth: AwsAuthenticator, name: str, description: str, content: str, policy_type: str
) -> Optional[str]:
  try:
    client = auth.new_client("organizations", region=GLOBAL_API_REGION)
    resp = client.create_policy(
      Name=name, Description=description, Content=content, Type=policy_type
    )
    return resp.get("Policy")
  except ClientError as ex:
    raise ex


def list_policies_for_target(
  auth: AwsAuthenticator,
  target_id: str,
  policy_filter: str = "SERVICE_CONTROL_POLICY",
  max_results: int = 20,
) -> Optional[str]:
  try:
    policies = []
    client = auth.new_client("organizations", region=GLOBAL_API_REGION)
    for resp in client.get_paginator("list_policies_for_target").paginate(
      TargetId=target_id, Filter=policy_filter, MaxResults=max_results
    ):
      policies.extend(resp["Policies"])
    return policies
  except ClientError as ex:
    raise ex


def attach_policy(auth: AwsAuthenticator, policy_id: str, target_id: str):
  try:
    client = auth.new_client("organizations", region=GLOBAL_API_REGION)
    client.attach_policy(PolicyId=policy_id, TargetId=target_id)
  except ClientError as ex:
    raise ex


def describe_policy(auth: AwsAuthenticator, policy_id: str) -> Dict[str, str]:
  try:
    client = auth.new_client("organizations", region=GLOBAL_API_REGION)
    resp = client.describe_policy(PolicyId=policy_id)
    return resp["Policy"]
  except ClientError as ex:
    raise ex


def update_policy(auth: AwsAuthenticator, policy_id: str, content: str) -> Dict[str, str]:
  try:
    client = auth.new_client("organizations", region=GLOBAL_API_REGION)
    resp = client.update_policy(PolicyId=policy_id, Content=content)
    return resp["Policy"]
  except ClientError as ex:
    raise ex


def delete_policy(auth: AwsAuthenticator, policy_id: str) -> Dict[str, str]:
  try:
    client = auth.new_client("organizations", region=GLOBAL_API_REGION)
    resp = client.delete_policy(PolicyId=policy_id)
    return resp
  except ClientError as ex:
    raise ex


def detach_policy(auth: AwsAuthenticator, policy_id: str, target_id: str) -> Dict[str, str]:
  try:
    client = auth.new_client("organizations", region=GLOBAL_API_REGION)
    resp = client.detach_policy(PolicyId=policy_id, TargetId=target_id)
    return resp
  except ClientError as ex:
    raise ex
