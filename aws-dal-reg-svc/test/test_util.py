from util import jitter_ttl, response_tags, tag_value


def test_jitter_ttl():
  ttl = 86400
  assert jitter_ttl(ttl, 0.5) >= 43200
  assert jitter_ttl(ttl, 0.25) >= 64800


def test_response_tags():
  response = {
    "Tags": [
      {"Key": "kite_project", "Value": "cat-aws-test"},
      {"Key": "Created_by", "Value": "gcp-admin"},
    ],
    "ResponseMetadata": {
      "RequestId": "76d41a97-7e48-4171-8626-7f44dc0163ff",
      "HTTPStatusCode": 200,
      "HTTPHeaders": {
        "x-amzn-requestid": "76d41a97-7e48-4171-8626-7f44dc0163ff",
        "content-type": "application/x-amz-json-1.1",
        "content-length": "97",
        "date": "Fri, 11 Feb 2022 17:49:29 GMT",
      },
      "RetryAttempts": 0,
    },
  }

  expected = [
    {"Key": "kite_project", "Value": "cat-aws-test"},
    {"Key": "Created_by", "Value": "gcp-admin"},
  ]
  assert expected == response_tags(response)


def test_tag_value():
  expected = "abc"
  actual = tag_value([{"Key": "test", "Value": "abc"}], "test")
  assert expected == actual

  expected = "123"
  actual = tag_value([{"Key": "test", "Value": "abc"}, {"Key": "Zyx", "Value": "123"}], "zYx")
  assert expected == actual

  expected = None
  actual = tag_value([{"Key": "test", "Value": "abc"}], "doremi")
  assert expected == actual

  expected = None
  actual = tag_value([], "")
  assert expected == actual
