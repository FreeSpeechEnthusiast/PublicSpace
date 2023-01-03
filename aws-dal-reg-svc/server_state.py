import os
from time import time
from typing import Any, Dict, List

from twitter.common import log

from server_config import STAGE_PROGRESS_TIMEOUTS


def stage_progress_timeout(stage: str = None) -> int:
  default = STAGE_PROGRESS_TIMEOUTS["default"]
  if stage:
    _, stage_name = stage.split(".", 1)
    return STAGE_PROGRESS_TIMEOUTS.get(stage_name, default)
  else:
    return default


class ServerState:
  _INSTANCE = None

  def __new__(cls, meta: Dict[str, Any] = None, options: Dict[str, Any] = None):
    if not cls._INSTANCE:
      cls._INSTANCE = object.__new__(cls)
      cls._INSTANCE.meta = {}
      cls._INSTANCE.observed_stats = {}
      cls._INSTANCE.options = {}
      cls._INSTANCE.stages = set()
      cls._INSTANCE.stage_store = None
      cls._INSTANCE.stage_queue = None
    if meta:
      cls._INSTANCE.meta = {
        **cls._INSTANCE.meta,
        **meta,
      }
    if options:
      cls._INSTANCE.options = {
        **cls._INSTANCE.options,
        **options,
      }
    return cls._INSTANCE

  @classmethod
  def __str__(cls):
    return "{}=(active_stages={} - {})".format(
      cls._INSTANCE.__class__.__name__, cls._INSTANCE.stages, cls._INSTANCE.meta
    )

  @classmethod
  def set_stage_store(cls, s):
    cls._INSTANCE.stage_store = s

  @classmethod
  def set_stage_queue(cls, q: Any):
    cls._INSTANCE.stage_queue = q

  @classmethod
  def start_stage(cls, stage: str):
    cls._INSTANCE.stages.add(stage)
    cls._INSTANCE.meta[f"stage_{stage}_start"] = time()
    log.info(f"stage started: {stage}")

  @classmethod
  def complete_stage(cls, stage: str):
    if stage in cls._INSTANCE.stages:
      cls._INSTANCE.stages.remove(stage)
    if cls._INSTANCE.stage_store:
      cls._INSTANCE.stage_store.put(stage)
    if cls._INSTANCE.stage_queue:
      cls._INSTANCE.stage_queue.put_nowait(stage + "_stage_completed")
    cls._INSTANCE.meta[f"stage_{stage}_completed"] = time()
    log.info(f"stage completed: {stage}")

  @classmethod
  def monitor(cls, accounts: List[int], stats: Dict[str, int]):
    def _exit_server(stage: str):
      log.fatal(f"exiting due to stage progress timeout - stage={stage}")
      os._exit(os.EX_TEMPFAIL)

    def _process(stage: str, metrics: List[str], obsv_stats: Dict[str, int], stats: Dict[str, int]):
      timeout_statuses = []
      for metric in metrics:
        if metric in obsv_stats.keys():
          obsv_time, val = obsv_stats[metric]
          if val == stats.get(metric):
            if (time() - obsv_time) > stage_progress_timeout(stage):
              log.warn(
                "stage progress metric timeout observed - {} - {} - {} = {}".format(
                  stage, metric, obsv_time, val
                )
              )
              timeout_statuses.append(True)
              continue
        timeout_statuses.append(False)

      if all(timeout_statuses):
        _exit_server(stage)
      if any(timeout_statuses):
        log.info("stage progress timeout suppressed due to partial progress - {}".format(stage))

    # monitor overall progress
    if len(cls._INSTANCE.stages) == 0:
      if "zero_stages_observed" in cls._INSTANCE.meta.keys():
        if (time() - cls._INSTANCE.meta["zero_stages_observed"]) > stage_progress_timeout():
          _exit_server(None)
      else:
        cls._INSTANCE.meta["zero_stages_observed"] = time()
    else:
      # remove zero_stages_observed value if present
      cls._INSTANCE.meta.pop("zero_stages_observed", None)

    # monitor stage progress
    for account in accounts:
      for stage in cls._INSTANCE.stages:
        if stage == f"{account}.scan":
          metrics = [
            f"{account}.dax_scanner.observed_clusters",
            f"{account}.dynamodb_scanner.observed_tables",
            f"{account}.kinesis_scanner.observed_streams",
            f"{account}.s3_scanner.observed_buckets",
            f"{account}.sqs_scanner.observed_queues",
          ]
        elif stage == f"{account}.filter":
          metrics = [f"{account}.resource_filter.emitted"]
        elif stage == f"{account}.access_simulation":
          metrics = [f"{account}.access_simulation_api_calls"]
        elif stage == f"{account}.registration":
          metrics = [
            f"{account}.registrar.datasets_registered",
            f"{account}.registrar.datasets_registered_late",
            f"{account}.registrar.datasets_registered_kite",
          ]
        else:
          continue

        _process(stage, metrics, cls._INSTANCE.observed_stats, stats)
        for metric in metrics:
          val = stats.get(metric)
          if val or val == 0:
            if metric in cls._INSTANCE.observed_stats.keys():
              _, obsv_val = cls._INSTANCE.observed_stats[metric]
              if obsv_val == val:
                continue  # only update obsv time if val changes
            cls._INSTANCE.observed_stats[metric] = [time(), val]
