from aws_arn import Arn, S3Arn
from aws_kite_role import (
  build_kite_role,
  DAXKiteRole,
  DynamoDbKiteRole,
  KinesisStreamKiteRole,
  S3BucketKiteRole,
  SqsQueueKiteRole,
)
from server_state import ServerState


def test_build_kite_role():
  tests = [
    {
      "arn": Arn("arn:aws:dynamodb:us-east-2:123456789012:table/myDynamoDBTable"),
      "project": "testing",
      "expected": DynamoDbKiteRole,
    },
    {
      "arn": Arn("arn:aws:dax:us-west-2:123456789012:cache/DAXCluster01"),
      "project": "testing",
      "expected": DAXKiteRole,
    },
    {
      "arn": Arn("arn:aws:kinesis:us-west-2:111122223333:stream/my-stream"),
      "project": "testing",
      "expected": KinesisStreamKiteRole,
    },
    {
      "arn": Arn("arn:aws:sqs:us-east-2:444455556666:queue1"),
      "project": "testing",
      "expected": SqsQueueKiteRole,
    },
    {
      "arn": S3Arn("test.test.com", "11111111", "us-east-1"),
      "project": "testing",
      "expected": S3BucketKiteRole,
    },
  ]
  for test in tests:
    assert isinstance(build_kite_role(test["arn"], test["project"]), test["expected"])

  # bad resource
  bad_resource_arn = Arn("arn:aws:kinesis:us-west-2:111122223333:bad_arn/my-stream")
  assert build_kite_role(bad_resource_arn, "testing") is None

  # unsupported resource type
  bad_type = Arn("arn:aws:magic:us-west-2:111122223333:stream/my-stream")
  assert build_kite_role(bad_type, "testing") is None

  # no project should return none
  arn = Arn("arn:aws:dynamodb:us-east-2:123456789012:table/myDynamoDBTable")
  assert build_kite_role(arn, None) is None

  # empty project should return none
  arn = Arn("arn:aws:dynamodb:us-east-2:123456789012:table/myDynamoDBTable")
  assert build_kite_role(arn, "") is None

  # When the server state is disabled, return None
  ServerState().options["disable_kite"] = True
  arn = Arn("arn:aws:dynamodb:us-east-2:123456789012:table/myDynamoDBTable")
  assert build_kite_role(arn, "testing") is None
  ServerState().options["disable_kite"] = False
