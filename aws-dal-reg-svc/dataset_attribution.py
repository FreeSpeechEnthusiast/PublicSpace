import re
from typing import Dict, List, Optional, Union

from twitter.common import log
from util import tag_value

from server_config import DEFAULT_OWNER_TEAM, RESOURCE_ATTRIBUTION_FILTERS


def resource_filter(account_id: str, team: str, f: str):
  try:
    return re.compile(f, re.IGNORECASE)
  except re.error:
    err_msg = "unable to compile resource filter - account: {} team: {} value: {}"
    log.error(err_msg.format(account_id, team, f))


def filters_match(account_id: str, team: str, filters: List[str], name: str) -> bool:
  for f in filters:
    pattern = resource_filter(account_id, team, f)
    if pattern and pattern.match(name):
      return True

  return False


def _resource_attribution_filters_parser(
  account_filters: Dict, account_id: str, name: str
) -> Optional[str]:
  for attribute, filters in account_filters.items():
    if attribute == "default":
      continue
    if "include" in filters.keys():
      if filters_match(account_id, attribute, filters["include"], name):
        if "exclude" in filters.keys():
          if filters_match(account_id, attribute, filters["exclude"], name):
            continue
        return attribute

  return account_filters.get("default")


def resource_owner(
  account_id: str,
  name: str,
  properties: Dict[str, Union[str, int, List[str], List[Dict[str, str]]]],
) -> str:
  # use `pdp_team` resource tag if present
  if "Tags" in properties.keys():
    value = tag_value(properties["Tags"], "pdp_team")
    if value:
      return value.upper()

  account_filters = RESOURCE_ATTRIBUTION_FILTERS.get(account_id)
  if not account_filters or "default" not in account_filters.keys():
    return DEFAULT_OWNER_TEAM

  return _resource_attribution_filters_parser(account_filters, account_id, name)
