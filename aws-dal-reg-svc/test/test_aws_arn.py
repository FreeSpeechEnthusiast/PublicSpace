from aws_arn import Arn, S3Arn


def test_arn():
  arn = Arn(
    "arn:aws:sts::673964658973:assumed-role/AWSReservedSSO_default-administrator-access_3fa297e9a80924ff/dmarlow@twitter.com"
  )
  assert arn.partition == "aws"
  assert arn.service == "sts"
  assert arn.region == ""
  assert arn.account_id == "673964658973"
  assert (
    arn.resource
    == "AWSReservedSSO_default-administrator-access_3fa297e9a80924ff/dmarlow@twitter.com"
  )
  assert arn.resource_type == "assumed-role"


def test_dynamodb_arn():
  arn = "arn:aws:dynamodb:ap-southeast-1:075211493084:table/passtiche_manifest"
  assert Arn(arn).resource_type == "table"


def test_dynamodb_dal_name():
  arn = "arn:aws:dynamodb:us-east-1:338624818603:table/users"
  assert Arn(arn).dal_name() == "aws-338624818603-us-east-1-dynamodb-table-users"


def test_dynamodb_kite_name():
  arn = "arn:aws:dynamodb:us-east-2:338624818603:table/terraform_state_lock"
  assert Arn(arn).kite_name() == "338624818603.us-east-2-terraform_state_lock"


def test_kinesis_arn():
  arn = "arn:aws:kinesis:us-west-2:673964658973:stream/kinesis-stream-dev-fleets-active-fleeters-3-Stream-EMC5VA1H7RV0"
  assert Arn(arn).resource == "kinesis-stream-dev-fleets-active-fleeters-3-Stream-EMC5VA1H7RV0"


def test_s3_arn():
  arn = "arn:aws:s3:::stackset-enable-aws-config-organizat-configbucket-8qdk11hmb3oo"
  assert Arn(arn).resource == "stackset-enable-aws-config-organizat-configbucket-8qdk11hmb3oo"

  s3_arn = S3Arn(
    "stackset-enable-aws-config-organizat-configbucket-8qdk11hmb3oo", "673964658973", "us-west-1"
  )
  assert s3_arn.arn == arn
  assert (
    s3_arn.dal_name()
    == "aws-673964658973-us-west-1-s3-stackset-enable-aws-config-organizat-configbucket-8qdk11hmb3oo"
  )
  assert (
    s3_arn.kite_name()
    == "673964658973.us-west-1-stackset-enable-aws-config-organizat-configbucket-8qdk11hmb3oo"
  )


def test_sqs_arn():
  arn = Arn("arn:aws:sqs:us-west-2:673964658973:canary-high-priority-fanout")
  assert arn.account_id == "673964658973"
  assert arn.resource_type is None
  assert arn.resource == "canary-high-priority-fanout"
  assert arn.dal_name() == "aws-673964658973-us-west-2-sqs-canary-high-priority-fanout"
  assert arn.kite_name() == "673964658973.us-west-2-canary-high-priority-fanout"


def test_dax_arn():
  arn = Arn("arn:aws:dax:us-west-2:123456789012:cache/DAXCluster01")
  assert arn.account_id == "123456789012"
  assert arn.resource_type == "cache"
  assert arn.resource == "DAXCluster01"
  assert arn.dal_name() == "aws-123456789012-us-west-2-dax-cache-DAXCluster01"
  assert arn.kite_name() == "123456789012.us-west-2-DAXCluster01"
