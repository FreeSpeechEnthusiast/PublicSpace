from datetime import datetime, timedelta, timezone
import getpass
import json
import os
import queue
from random import randrange, uniform
from subprocess import run
from threading import Event
from time import sleep, time
from typing import Any, Callable, Dict, List, Optional

from twitter.common import log

from server_config import TSS_PATH
from server_state import ServerState
from urllib3 import ProxyManager


class CompletionQueue(queue.Queue):
  def __init__(self, maxsize=0):
    self.completed = Event()
    super().__init__(maxsize)

  def set_completed(self):
    self.completed.set()


def copy_completion_queue(src: CompletionQueue, snk: CompletionQueue, stage: str = None):
  process_completion_queue(src, snk.put, stage=stage)


def process_completion_queue(
  src: CompletionQueue,
  fn: Callable,
  args: Dict[str, Any] = {},
  break_on_empty: bool = False,
  completion_callback: Callable = None,
  stage: str = None,
):
  try:
    while not ((break_on_empty or src.completed.is_set()) and src.empty()):
      try:
        val = src.get(timeout=0.1)
        if stage and stage not in ServerState().stages:
          ServerState().start_stage(stage)

        fn(val, **args)
      except queue.Empty:
        pass
  except Exception as ex:
    log.exception(f"`process_completion_queue` exception: {ex}")
    raise ex
  finally:
    if completion_callback:
      completion_callback()
    if stage:
      ServerState().complete_stage(stage)


def account_stage_key(account_id: str, stage: str) -> str:
  return ".".join([account_id, stage])


def batch_process_completion_queue_with_snk(
  batch_size: int,
  src: CompletionQueue,
  snk: CompletionQueue,
  fn: Callable[[List[Any]], Any],
  args: Dict[str, Any] = {},
  completion_callback: Callable = None,
  stage: str = None,
):
  batch = []
  try:
    while not (src.completed.is_set() and src.empty()):
      if batch_size > len(batch):
        try:
          val = src.get(timeout=0.1)
          if stage and stage not in ServerState().stages:
            ServerState().start_stage(stage)

          batch.append(val)
        except queue.Empty:
          pass
      else:
        snk.put(fn(batch, **args))
        batch = []

    if len(batch) > 0:
      snk.put(fn(batch, **args))
  except Exception as ex:
    log.exception(f"`batch_process_completion_queue_with_snk` exception: {ex}")
    raise ex
  finally:
    if completion_callback:
      completion_callback()
    if stage:
      ServerState().complete_stage(stage)


def current_ms_time() -> int:
  return int(round(time() * 1000))


def fatal(msg: str):
  log.fatal(msg)
  raise Exception(msg)


def get_krb_principal() -> str:
  """
  Helper function to get local user as Kerberos principal.
  """
  return "{}@TWITTER.BIZ".format(get_krb_user())


def get_krb_user() -> str:
  """
  Get local user.
  """
  return getpass.getuser()


def get_tss_json(basename: str) -> Dict[str, Any]:
  with open(os.path.join(TSS_PATH, basename)) as f:
    return json.loads(f.read())


def jitter_ttl(ttl: int, ratio: float = 0.25) -> int:
  min_val = ttl - (ttl * ratio)
  return randrange(min_val, ttl)


def rate_limit(limit_intvl: float, start_time: float):
  elapsed_time = time() - start_time
  if limit_intvl > elapsed_time:
    sleep(limit_intvl - elapsed_time)


def relative_time_range_hours_ago(n):
  now = datetime.now(timezone.utc)
  return (now - timedelta(hours=n), now)


def response_tags(response: Dict[str, Any], key: str = "Tags") -> List[Dict[str, str]]:
  if key in response.keys():
    return response[key]
  else:
    return []


def tag_value(tags: List[Dict[str, str]], key: str) -> Optional[str]:
  for tag in tags:
    if tag.get("Key").lower() == key.lower() and tag.get("Value"):
      return tag.get("Value")


def envoy_proxy_connection_check(proxy_url):
  if proxy_url:
    log.info(f"Waiting for the envoy proxy {proxy_url} to be available.")

    http = ProxyManager(proxy_url)
    sleep_time = 10.0
    backoff_in_seconds = 1.0
    for i in range(10):
      sleep(sleep_time)
      sleep_time = backoff_in_seconds * 2 ** (i + 1) + uniform(0, 1)
      try:
        r = http.request("GET", "http://organizations.us-east-1.amazonaws.com", retries=1)
        log.info(f"Envoy Proxy is available. {r.status}")
        break
      except Exception as e:
        log.info(f"Failed to connect to the proxy {e}")
      log.info(f"Retrying after sleeping {sleep_time}sec....")


def terminate_envoy_sidecar(proxy_url):
  if proxy_url:
    log.info("signaling termination to the proxy sidecar")
    run(
      "kill $(ps -eo user:50,pid,cmd | grep 'aws-dal-registration-svc.*[.]/envoy' | awk '{ print $2 }')",
      shell=True,
    )
