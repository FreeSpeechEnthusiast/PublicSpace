from datetime import datetime

from aws_organizations import (
  delete_policy,
  detach_policy,
  get_account_tag,
  get_account_tags,
  get_accounts,
  list_org_accounts,
  list_ou_accounts,
  list_policies_for_target,
)
from server_state import ServerState


def test_list_org_accounts(mock_org_auth):
  response = {
    "Accounts": [
      {
        "Id": "12345678910",
        "Arn": "string",
        "Email": "string",
        "Name": "string",
        "Status": "ACTIVE",
        "JoinedMethod": "CREATED",
        "JoinedTimestamp": datetime(2015, 1, 1),
      },
      {
        "Id": "34567891234",
        "Arn": "testing-bad",
        "Email": "testing-bad@testing.com",
        "Name": "suspended account",
        "Status": "SUSPENDED",
        "JoinedMethod": "CREATED",
        "JoinedTimestamp": datetime(2015, 1, 1),
      },
    ],
  }
  mock_org_auth.add_response("list_accounts", response, {})
  mock_org_auth.activate()

  assert list_org_accounts(mock_org_auth) == ["12345678910"]


def test_get_account_tags(mock_org_auth):
  response = {
    "Tags": [
      {"Key": "test_tag", "Value": "test_string"},
    ],
  }
  mock_org_auth.add_response("list_tags_for_resource", response, {"ResourceId": "12345678910"})
  mock_org_auth.activate()

  assert get_account_tags(mock_org_auth, "12345678910") == {
    "Tags": [{"Key": "test_tag", "Value": "test_string"}]
  }


def test_get_account_tag(mock_org_auth):
  response = {
    "Tags": [
      {"Key": "test_tag", "Value": "test_string"},
    ],
  }
  mock_org_auth.add_response("list_tags_for_resource", response, {"ResourceId": "12345678910"})
  mock_org_auth.activate()

  assert get_account_tag(mock_org_auth, "12345678910", "test_tag") == "test_string"


def test_list_ou_accounts(mock_org_auth):
  response = {
    "Children": [
      {"Id": "12345678910", "Type": "ACCOUNT"},
      {"Id": "34567891234", "Type": "ORGANIZATIONAL_UNIT"},
    ],
  }
  mock_org_auth.add_response(
    "list_children", response, {"ParentId": "ou-test-parent", "ChildType": "ACCOUNT"}
  )
  mock_org_auth.activate()

  assert list_ou_accounts(mock_org_auth, "ou-test-parent") == ["12345678910"]


def test_get_accounts(mock_org_auth):

  # Suspended or quarantined
  response = {
    "Accounts": [
      {
        "Id": "12345678910",
        "Arn": "string",
        "Email": "string",
        "Name": "string",
        "Status": "ACTIVE",
        "JoinedMethod": "CREATED",
        "JoinedTimestamp": datetime(2015, 1, 1),
      },
      {
        "Id": "34567891234",
        "Arn": "testing-bad",
        "Email": "testing-bad@testing.com",
        "Name": "suspended account",
        "Status": "SUSPENDED",
        "JoinedMethod": "CREATED",
        "JoinedTimestamp": datetime(2015, 1, 1),
      },
      {
        "Id": "34567891237",
        "Arn": "testing-bad",
        "Email": "testing-quarantined@testing.com",
        "Name": "quarantined account",
        "Status": "ACTIVE",
        "JoinedMethod": "CREATED",
        "JoinedTimestamp": datetime(2015, 1, 1),
      },
    ],
  }

  mock_org_auth.add_response("list_accounts", response, {})
  mock_org_auth.activate()

  assert get_accounts(mock_org_auth) == ["12345678910", "34567891237"]

  ServerState().options["process_account"] = "12345678910"
  assert get_accounts(mock_org_auth) == ["12345678910"]
  del ServerState().options["process_account"]


def test_detach_policy(mock_org_auth):
  response = {
    "ResponseMetadata": {
      "RequestId": "1d746cf3-f83a-418d-8b22-726e3a66e4db",
      "HTTPStatusCode": 200,
      "HTTPHeaders": {
        "x-amzn-requestid": "1d746cf3-f83a-418d-8b22-726e3a66e4db",
        "content-type": "application/x-amz-json-1.1",
        "content-length": "0",
        "date": "Tue, 06 Sep 2022 21:39:01 GMT",
      },
      "RetryAttempts": 0,
    }
  }

  mock_org_auth.add_response(
    "detach_policy", response, {"PolicyId": "p-pl9dqm0p", "TargetId": "586372745279"}
  )
  mock_org_auth.activate()

  assert (
    detach_policy(mock_org_auth, "p-pl9dqm0p", "586372745279")["ResponseMetadata"]["HTTPStatusCode"]
    == 200
  )


def test_delete_policy(mock_org_auth):
  response = {
    "ResponseMetadata": {
      "RequestId": "de4392a1-771d-46bb-bdd8-dc1cbaa96659",
      "HTTPStatusCode": 200,
      "HTTPHeaders": {
        "x-amzn-requestid": "de4392a1-771d-46bb-bdd8-dc1cbaa96659",
        "content-type": "application/x-amz-json-1.1",
        "content-length": "0",
        "date": "Tue, 06 Sep 2022 21:39:01 GMT",
      },
      "RetryAttempts": 0,
    }
  }

  mock_org_auth.add_response("delete_policy", response, {"PolicyId": "p-pl9dqm0p"})
  mock_org_auth.activate()

  assert delete_policy(mock_org_auth, "p-pl9dqm0p")["ResponseMetadata"]["HTTPStatusCode"] == 200


def test_list_policies_for_target(mock_org_auth):
  response = {
    "Policies": [
      {
        "Id": "test_policy",
        "Arn": "arn:aws:organizations::171959851929:policy/o-h3re6gv7m2/service_control_policy/p-test01",
        "Name": "test_policy",
        "Description": "test_policy",
        "Type": "SERVICE_CONTROL_POLICY",
        "AwsManaged": False,
      },
    ],
  }

  mock_org_auth.add_response(
    "list_policies_for_target",
    response,
    {"TargetId": "171959851929", "Filter": "SERVICE_CONTROL_POLICY", "MaxResults": 20},
  )
  mock_org_auth.activate()

  assert list_policies_for_target(mock_org_auth, "171959851929")[0]["Id"] == "test_policy"
