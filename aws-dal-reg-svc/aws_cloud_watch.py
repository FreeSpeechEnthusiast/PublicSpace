from datetime import datetime
from typing import Dict, List, Tuple

from twitter.common import log
from twitter.common.metrics import AtomicGauge, Observable, RootMetrics

from aws_iam import AwsAuthenticator, get_authenticator
from botocore.exceptions import ClientError
from server_config import AWS_CLOUD_WATCH_ACCOUNT_ID, AWS_CLOUD_WATCH_REGION
from server_state import ServerState


class CloudWatchReporter(Observable):
  def __init__(self, authenticator: AwsAuthenticator, namespace: str):
    self.client = authenticator.new_client("cloudwatch", AWS_CLOUD_WATCH_REGION)
    self.namespace = namespace
    self._metrics_submitted = self.metrics.register(AtomicGauge("metrics_submitted"))
    self._put_errors = self.metrics.register(AtomicGauge("put_errors"))

  def process(self, stats: Dict[str, int]):
    for metric, value in stats.items():
      account_id, name = parse_metric(metric)
      self.put(account_id, name, value)

  def put(self, account_id: str, metric: str, value: int):
    try:
      self.client.put_metric_data(
        Namespace=self.namespace,
        MetricData=[
          {
            "Dimensions": [{"Name": "account_id", "Value": account_id}],
            "MetricName": metric,
            "Value": value,
            "Unit": "Count",
          }
        ],
      )
      self._metrics_submitted.increment()
    except ClientError as ex:
      log.error(f"unable to submit cloudwatch metric: {metric}. ex={ex}")
      self._put_errors.increment()


class CloudWatchStatsQuery:
  def __init__(self, metric: str, namespace: str, period: int, statistic: str):
    self.metric = metric
    self.namespace = namespace
    self.period = period
    self.statistic = statistic

  def query(self, client, dimensions: List[Dict[str, str]], time_range: Tuple[datetime, datetime]):
    start_time, end_time = time_range
    return client.get_metric_statistics(
      Namespace=self.namespace,
      MetricName=self.metric,
      Dimensions=dimensions,
      StartTime=start_time,
      EndTime=end_time,
      Period=self.period,
      Statistics=[self.statistic],
    )


def account_metric(account_id: str, metric: str) -> str:
  return ".".join([account_id, metric])


def get_cloud_watch_reporter(namespace: str) -> CloudWatchReporter:
  reporter = CloudWatchReporter(get_authenticator(AWS_CLOUD_WATCH_ACCOUNT_ID), namespace)
  metrics = RootMetrics().scope(AWS_CLOUD_WATCH_ACCOUNT_ID)
  metrics.register_observable("cloud_watch", reporter)
  return reporter


def parse_metric(value: str) -> Tuple[str, str]:
  elements = value.split(".")
  account_id = elements[0]
  return account_id, ".".join(elements[1:])


def put_account_metric(metric: str, reporter: CloudWatchReporter, value: int = 1):
  account_id, name = parse_metric(metric)
  reporter.put(account_id, name, value)
  if ServerState().options.get("log_metrics"):
    log.info(f"metric emitted - {metric}: {value}")
