from base64 import b64encode
from datetime import datetime
import json

from aws_arn import S3Arn
from aws_resource import Resource
from com.twitter.dal.has_personal_data.ttypes import HasPersonalData
from com.twitter.dal.model.ttypes import StorageType, URL
from dataset import Dataset, DatasetName


experimental_resource = Resource(
  name="experimental-s3-bucket-test",
  arn=S3Arn("experimental-s3-bucket-test", "11111111", "us-west-2"),
  creation_date=datetime.now(),
  status="ACTIVE",
  properties={
    "Tags": [
      {
        "Key": "pdp_records_classes",
        "Value": b64encode(json.dumps(["a", "b", "c"]).encode("utf-8")),
      },
      {"Key": "pdp_annotations", "Value": b64encode(json.dumps(["a", "b", "c"]).encode("utf-8"))},
      {"Key": "pdp_pii", "Value": "1"},
      {"Key": "pdp_project", "Value": "test"},
    ],
  },
)


def test_dataset_name():
  experimental = Dataset(resource=experimental_resource, accesses=[])
  assert experimental.name() == DatasetName(
    role="aws-dal-registration-svc",
    environment=2,
    name="aws-11111111-us-west-2-s3-experimental-s3-bucket-test",
  )


def test_dataset_records_classes():
  experimental = Dataset(resource=experimental_resource, accesses=[])
  assert experimental.records_classes() == ["a", "b", "c"]


def test_dataset_annotations():
  experimental = Dataset(resource=experimental_resource, accesses=[])
  assert experimental.records_classes() == ["a", "b", "c"]


def test_dataset_properties():
  experimental = Dataset(resource=experimental_resource, accesses=[])
  assert experimental.properties() == {
    "URL": URL(url="aws://arn:aws:s3:::experimental-s3-bucket-test"),
    "segment_type": 2,
    "storage_type": StorageType("S3"),
  }


def test_dataset_has_personal_data():
  experimental = Dataset(resource=experimental_resource, accesses=[])
  assert experimental.contains_pii() == HasPersonalData.YES_PERSONAL_DATA


def test_dataset_project():
  experimental = Dataset(resource=experimental_resource, accesses=[])
  assert experimental.project() == "test"
