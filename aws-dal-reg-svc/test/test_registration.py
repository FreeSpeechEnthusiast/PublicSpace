from base64 import b64encode
from datetime import datetime

from aws_arn import Arn, S3Arn
from aws_iam import IamObservedAccess
from aws_resource import Resource
from com.twitter.dal.properties.ttypes import LogicalDatasetPropertyKey
from com.twitter.statebird.v2.thriftpython.ttypes import BatchApp, Environment
from dataset import Dataset
from dateutil.tz import tzutc
from registration import annotations, app, cluster_name, has_annotations, records_classes
from server_state import ServerState


ServerState().options["env"] = "staging"


def test_has_annotations():
  arn = S3Arn("vine-archive-eks-lb-logs", "496113600437", "us-west-2")
  resource = Resource(
    arn.resource, arn, datetime(2021, 6, 7, 17, 55, 32, tzinfo=tzutc()), "ACTIVE", {}
  )
  dataset = Dataset(
    resource,
    [
      IamObservedAccess(
        "arn:aws:iam::496113600437:role/aws-service-role/elasticloadbalancing.amazonaws.com/AWSServiceRoleForElasticLoadBalancing",
        "arn:aws:s3:::vine-archive-eks-lb-logs",
        set(["write"]),
      ),
    ],
  )
  dataset.set_meta(
    {
      "ID": "s3/vine-archive-eks-lb-logs",
      "annotations": [
        "IpAddress",
        "RawUrlPath",
        "RawUrlQueryParameter",
        "UserAgent",
      ],
    }
  )

  # PersonalDataDatasetAnnotations
  assert has_annotations(annotations(dataset)) == True

  # clear annotations to check for no annotations
  # and for use of the test after this
  dataset.set_meta({})
  assert has_annotations(annotations(dataset)) == False

  # PersonalDataFieldAnnotation
  dataset.set_meta(
    {
      "ID": "s3/archive.vine.co",
      "annotations": {
        "posts.created": ["PublicTimestamp"],
        "posts.likes": ["CountOfPublicLikes"],
        "posts.permalinkUrl": ["LongUrl"],
        "posts.userId": ["UserId"],
        "posts.userIdStr": ["UserId"],
        "posts.username": ["DisplayName", "Username"],
        "posts.vanityUrls": ["DisplayName", "Username"],
        "profiles.created": ["PublicTimestamp"],
        "profiles.shareUrl": ["LongUrl"],
        "profiles.userId": ["UserId"],
        "profiles.userIdStr": ["UserId"],
        "profiles.username": ["DisplayName", "Username"],
        "profiles.vanityUrls": ["DisplayName", "Username"],
      },
    }
  )
  assert has_annotations(annotations(dataset)) == True


def test_annotations():
  arn = S3Arn("vine-archive-eks-lb-logs", "496113600437", "us-west-2")
  resource = Resource(
    arn.resource, arn, datetime(2021, 6, 7, 17, 55, 32, tzinfo=tzutc()), "ACTIVE", {}
  )
  dataset = Dataset(
    resource,
    [
      IamObservedAccess(
        "arn:aws:iam::496113600437:role/aws-service-role/elasticloadbalancing.amazonaws.com/AWSServiceRoleForElasticLoadBalancing",
        "arn:aws:s3:::vine-archive-eks-lb-logs",
        set(["write"]),
      ),
    ],
  )
  dataset.set_meta(
    {
      "ID": "s3/vine-archive-eks-lb-logs",
      "annotations": [
        "IpAddress",
        "RawUrlPath",
        "RawUrlQueryParameter",
        "UserAgent",
      ],
    }
  )

  assert len(annotations(dataset).datasetAnnotations.personalDataTypes) == len(
    dataset.meta["annotations"]
  )


def test_annotations_from_tags():
  arn = S3Arn("vine-archive-eks-lb-logs", "496113600437", "us-west-2")
  resource = Resource(
    arn.resource,
    arn,
    datetime(2021, 6, 7, 17, 55, 32, tzinfo=tzutc()),
    "ACTIVE",
    {
      "Tags": [
        {
          "Key": "pdp_annotations",
          "Value": b64encode(
            b'["IpAddress", "RawUrlPath", "RawUrlQueryParameter", "UserAgent"]'
          ).decode("utf-8"),
        }
      ]
    },
  )
  dataset = Dataset(
    resource,
    [
      IamObservedAccess(
        "arn:aws:iam::496113600437:role/aws-service-role/elasticloadbalancing.amazonaws.com/AWSServiceRoleForElasticLoadBalancing",
        "arn:aws:s3:::vine-archive-eks-lb-logs",
        set(["write"]),
      ),
    ],
  )

  assert len(annotations(dataset).datasetAnnotations.personalDataTypes) == 4


def test_field_annotations():
  arn = S3Arn("archive.vine.co", "496113600437", "us-east-1")
  resource = Resource(
    arn.resource, arn, datetime(2017, 1, 18, 18, 48, 23, tzinfo=tzutc()), "ACTIVE", {}
  )
  dataset = Dataset(
    resource,
    [
      IamObservedAccess(
        "arn:aws:iam::496113600437:group/twitter-security",
        "arn:aws:s3:::archive.vine.co",
        set(["read", "write"]),
      ),
      IamObservedAccess(
        "arn:aws:iam::496113600437:group/twitter-security-scan",
        "arn:aws:s3:::archive.vine.co",
        set(["read"]),
      ),
    ],
  )
  dataset.set_meta(
    {
      "ID": "s3/archive.vine.co",
      "annotations": {
        "posts.created": ["PublicTimestamp"],
        "posts.likes": ["CountOfPublicLikes"],
        "posts.permalinkUrl": ["LongUrl"],
        "posts.userId": ["UserId"],
        "posts.userIdStr": ["UserId"],
        "posts.username": ["DisplayName", "Username"],
        "posts.vanityUrls": ["DisplayName", "Username"],
        "profiles.created": ["PublicTimestamp"],
        "profiles.shareUrl": ["LongUrl"],
        "profiles.userId": ["UserId"],
        "profiles.userIdStr": ["UserId"],
        "profiles.username": ["DisplayName", "Username"],
        "profiles.vanityUrls": ["DisplayName", "Username"],
      },
    }
  )

  assert len(annotations(dataset).datasetFieldAnnotations) == len(
    dataset.meta["annotations"].keys()
  )


def test_no_annotations():
  arn = S3Arn("archive.vine.co", "496113600437", "us-east-1")
  resource = Resource(
    arn.resource, arn, datetime(2017, 1, 18, 18, 48, 23, tzinfo=tzutc()), "ACTIVE", {}
  )
  dataset = Dataset(
    resource,
    [
      IamObservedAccess(
        "arn:aws:iam::496113600437:group/twitter-security",
        "arn:aws:s3:::archive.vine.co",
        set(["read", "write"]),
      ),
      IamObservedAccess(
        "arn:aws:iam::496113600437:group/twitter-security-scan",
        "arn:aws:s3:::archive.vine.co",
        set(["read"]),
      ),
    ],
  )

  assert annotations(dataset) is None


def test_no_records_classes():
  arn = S3Arn("test.test.com", "11111111", "us-east-1")
  resource = Resource(arn.resource, arn, datetime.now(), "ACTIVE", {})
  dataset = Dataset(resource=resource, accesses=[])

  assert records_classes(dataset) is None


def test_record_classes():
  arn = S3Arn("test.test.com", "11111111", "us-east-1")
  resource = Resource(arn.resource, arn, datetime.now(), "ACTIVE", {})
  dataset = Dataset(resource=resource, accesses=[])
  dataset.set_meta({"records_classes": ["3", "4", "5"]})
  assert records_classes(dataset)[
    LogicalDatasetPropertyKey.RecordsClassInfo
  ].recordsClassInfo.recordsClasses.rrsCodes == frozenset(["3", "4", "5"])


def test_records_classes_from_tags():
  arn = S3Arn("test.test.com", "11111111", "us-east-1")
  resource = Resource(
    arn.resource,
    arn,
    datetime.now(),
    "ACTIVE",
    {
      "Tags": [{"Key": "pdp_records_classes", "Value": b64encode(b'["1","2","3"]').decode("utf-8")}]
    },
  )
  dataset = Dataset(resource=resource, accesses=[])
  assert records_classes(dataset)[
    LogicalDatasetPropertyKey.RecordsClassInfo
  ].recordsClassInfo.recordsClasses.rrsCodes == frozenset(["1", "2", "3"])


def test_app():
  assert app() == BatchApp(
    domain="aws-dal-reg-svc",
    environment=Environment.STAGING,
    name="AWS-DAL-Registration-Service",
    role="aws-dal-registration-svc",
  )


def test_cluster_name():
  arn = Arn("arn:aws:dynamodb:us-east-2:123456789012:table/myDynamoDBTable")
  assert cluster_name(arn) == "aws:us-east-2"

  s3_arn = S3Arn("archive.vine.co", "496113600437", "us-east-1")
  assert cluster_name(s3_arn) == "aws:us-east-1"
