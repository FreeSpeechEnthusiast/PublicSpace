from aws_cloud_watch import parse_metric


def test_parse_metric():
  account_id, name = parse_metric('171959851929.s3_scanner.observed_buckets')
  assert account_id == '171959851929'
  assert name == 's3_scanner.observed_buckets'

  account_id, name = parse_metric('857487374138.users_observed')
  assert account_id == '857487374138'
  assert name == 'users_observed'
