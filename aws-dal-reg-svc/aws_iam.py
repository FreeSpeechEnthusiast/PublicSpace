import concurrent.futures
from os import environ
from time import sleep, time
from typing import Dict, List, Optional, Set

from twitter.common import log
from twitter.common.metrics import AtomicGauge
from twitter.common.metrics.metrics import Metrics
from util import get_tss_json, jitter_ttl, rate_limit

from aws_arn import Arn
from aws_resource import Resource, resources_by_service
import boto3
from boto3.session import Session
from botocore.config import Config
from botocore.credentials import RefreshableCredentials
from botocore.exceptions import ClientError, ConnectionClosedError
from botocore.parsers import ResponseParserError
from botocore.session import get_session
from datastore import Datastore
from errors import AwsEnvCredRequired, AwsInvalidIamEntityType, AwsRegionRequired
from server_config import (
  AWS_CLIENT_CONNECT_TIMEOUT,
  AWS_CLIENT_READ_TIMEOUT,
  AWS_CLIENT_RETRY_CONF,
  DYNAMO_ACCESS_CACHE_ACCOUNT_TTL,
  DYNAMO_ACCESS_CACHE_TABLE_NAME,
  DYNAMO_DEFAULT_ACCESS_CACHE_TTL,
  FUTURE_TIMEOUTS,
  GLOBAL_API_REGION,
  IAM_ACCESS_SIML_RATE_LIMIT,
  IAM_API_DELAYED_RETRIES,
  IAM_API_RETRY_DELAY_INTERVAL,
  IAM_MAX_API_FUTURES,
  PDP_ROLE_ARN_PATTERN,
  RESOURCE_ACCESS_POLICY_ACTIONS,
  SUPPORTED_IAM_ENTITIES,
  SVC_NAME,
)
from server_state import ServerState


class AwsAuthenticator:
  def __init__(
    self,
    account_id: str,
    access_key: str,
    secret_access_key: str,
    token: str = None,
    alt_role_arn_pattern: str = None,
  ):
    self.account_id = account_id
    self.access_key = access_key
    self.secret_access_key = secret_access_key
    self.token = token
    self.alt_role_arn_pattern = alt_role_arn_pattern
    self.session_creds = RefreshableCredentials.create_from_metadata(
      metadata=self.assume_role(), refresh_using=self.assume_role, method="sts-assume-role"
    )

  def assume_role(self) -> Dict[str, str]:
    sts = boto3.client(
      "sts",
      aws_access_key_id=self.access_key,
      aws_secret_access_key=self.secret_access_key,
      aws_session_token=self.token,
      config=self.client_config(GLOBAL_API_REGION),
    )

    role_arn_pattern = (
      self.alt_role_arn_pattern if self.alt_role_arn_pattern else PDP_ROLE_ARN_PATTERN
    )
    resp = sts.assume_role(
      RoleArn=role_arn_pattern.format(self.account_id), RoleSessionName=f"{SVC_NAME}-{time()}"
    )["Credentials"]
    credentials = {
      "access_key": resp["AccessKeyId"],
      "secret_key": resp["SecretAccessKey"],
      "token": resp["SessionToken"],
      "expiry_time": resp["Expiration"].isoformat(),
    }

    return credentials

  def client_config(self, region: str = None):
    args = {
      "connect_timeout": AWS_CLIENT_CONNECT_TIMEOUT,
      "read_timeout": AWS_CLIENT_READ_TIMEOUT,
      "region_name": region,
      "retries": AWS_CLIENT_RETRY_CONF,
    }

    if ServerState().options.get("envoy_proxy_url"):
      args["proxies"] = {"https": ServerState().options.get("envoy_proxy_url")}

    return Config(**args)

  def clone(self):
    return type(self)(self.account_id, self.access_key, self.secret_access_key, self.token)

  def _new(self, obj_type: str, service: str = None, region: str = None):
    core_session = get_session()
    core_session._credentials = self.session_creds
    if obj_type == "session" and region:
      core_session.set_config_variable("region", region)
    elif obj_type in ("client", "resource") and not region:
      raise AwsRegionRequired(obj_type)

    args = {"config": self.client_config(region)}
    boto_session = Session(botocore_session=core_session)
    if obj_type == "client":
      return boto_session.client(service, **args)
    elif obj_type == "resource":
      local_dynamodb_endpoint = ServerState().options.get("local_dynamodb_endpoint")
      if service == "dynamodb" and local_dynamodb_endpoint:
        args["endpoint_url"] = local_dynamodb_endpoint
      return boto_session.resource(service, **args)
    elif obj_type == "session":
      return boto_session

  def new_client(self, service: str, region: str):
    return self._new("client", service, region)

  def new_resource(self, service: str, region: str):
    return self._new("resource", service, region)

  def new_session(self, region: str = None) -> Session:
    return self._new("session", None, region)


class IamEntityPolicy:
  def __init__(self, entity_arn: Arn, policy: Dict[str, str]):
    self.entity_arn = entity_arn
    self.policy_name = policy["PolicyName"]
    self.policy_type = policy["PolicyType"]
    self.policy_arn = policy.get("PolicyArn")

  def document(self, client) -> str:
    if self.policy_type == "MANAGED":
      return get_managed_policy_document(client, self.policy_arn)
    else:
      return get_inline_policy_document(
        client, self.entity_arn.resource_type, self.entity_arn.resource, self.policy_name
      )

  def __str__(self) -> str:
    return "{}(arn={}, policy={}, type={})".format(
      self.__class__.__name__, self.entity_arn.arn, self.policy_name, self.policy_type
    )


class IamEntity:
  def __init__(self, arn: Arn):
    self.arn = arn

  def policies(self, client, svcs: List[str]) -> List[IamEntityPolicy]:
    policies = []
    for policy in get_entity_access_policies(client, svcs, self.arn.arn):
      policies.append(IamEntityPolicy(self.arn, policy))

    return policies

  def __str__(self) -> str:
    return "{}(arn={})".format(self.__class__.__name__, self.entity_arn.arn)


class IamObservedAccess:
  def __init__(self, entity_arn: str, resource_arn: str, levels: Set[str]):
    self.entity_arn = entity_arn
    self.resource_arn = resource_arn
    self.levels = levels

  def __repr__(self) -> str:
    return self.__str__()

  def __str__(self) -> str:
    return "{}(entity_arn={}, resource_arn={}, levels={})".format(
      self.__class__.__name__, self.entity_arn, self.resource_arn, self.levels
    )


class IamAccessCacheMetrics:
  _INSTANCE = None

  def __new__(cls):
    if not cls._INSTANCE:
      cls._INSTANCE = object.__new__(cls)
      cls._INSTANCE.metrics = {}
      for m in ("accesses_fetched", "add", "get", "hit", "miss"):
        cls._INSTANCE.metrics[m] = AtomicGauge(f"access_cache_{m}")

    return cls._INSTANCE

  def register_metrics(cls, metrics: Metrics):
    for _, gauge in cls._INSTANCE.metrics.items():
      metrics.register(gauge)


class IamAccessCache(Datastore):
  def __init__(self, auth: AwsAuthenticator):
    self.account_id = auth.account_id
    super().__init__(auth, DYNAMO_ACCESS_CACHE_TABLE_NAME)

  def add(self, resource: Resource, accesses: List[IamObservedAccess]):
    key = resource.arn.arn
    if not self.exists(key):
      ttl = DYNAMO_ACCESS_CACHE_ACCOUNT_TTL.get(self.account_id, DYNAMO_DEFAULT_ACCESS_CACHE_TTL)
      val = {
        "accesses": [a.__dict__ for a in accesses],
        "expire": int(time() + jitter_ttl(ttl)),
      }
      self.put(key, val)
      IamAccessCacheMetrics().metrics["add"].increment()

  def add_batch(self, accesses: Dict[Resource, List[IamObservedAccess]]):
    for resource, resource_accesses in accesses.items():
      self.add(resource, resource_accesses)

  def fetch(self, resource: Resource) -> Optional[List[IamObservedAccess]]:
    record = self.get(resource.arn.arn)
    IamAccessCacheMetrics().metrics["get"].increment()
    if record and "accesses" in record.keys():
      if int(record["expire"]) > time():
        IamAccessCacheMetrics().metrics["hit"].increment()
        accesses = []
        for access in record["accesses"]:
          accesses.append(
            IamObservedAccess(access["entity_arn"], access["resource_arn"], set(access["levels"]))
          )

        IamAccessCacheMetrics().metrics["accesses_fetched"].add(len(accesses))
        return accesses
    else:
      IamAccessCacheMetrics().metrics["miss"].increment()


def actions_access_levels(svc: str, actions: List[str]) -> Set[str]:
  access_levels = set()
  for access_level, ref_actions in RESOURCE_ACCESS_POLICY_ACTIONS[svc].items():
    for action in actions:
      if action in ref_actions:
        access_levels.add(access_level)

  return access_levels


def entity_batch_resource_accesses(
  client,
  actions: List[str],
  entity_arn: str,
  resource_arns: List[str],
  simulation_gauge: AtomicGauge,
  simulation_error_gauge: AtomicGauge,
  simulation_eval_gauge: AtomicGauge,
) -> Dict[str, List[str]]:
  allowed_actions = {}
  attempts = 0
  marker = None
  resp = None
  args = {
    "ActionNames": actions,
    "PolicySourceArn": entity_arn,
    "MaxItems": 200,
    "ResourceArns": resource_arns,
  }

  while marker or resp is None:
    if marker:
      args["Marker"] = marker

    try:
      start_time = time()
      if ServerState().options.get("disable_access_simulation"):
        resp = {"EvaluationResults": []}
      else:
        try:
          resp = client.simulate_principal_policy(**args)
        except ClientError as ex:
          if ex.response["Error"]["Code"] == "NoSuchEntity":
            log.info(f"access simulation IAM entity no longer exists. entity={entity_arn}")
            return {}
          else:
            raise ex
        rate_limit(IAM_ACCESS_SIML_RATE_LIMIT, start_time)
      simulation_gauge.increment()
    except (ClientError, ConnectionClosedError, ResponseParserError) as ex:
      if isinstance(ex, ClientError):
        if ex.response["Error"]["Code"] != "Throttling":
          raise ex

      attempts += 1
      simulation_error_gauge.increment()
      err_msg = "access simulation request failed: attempt={} args={} ex={}"
      if isinstance(ex, ResponseParserError):
        # log exception class name only as ResponseParserError exceptions contains the response
        # body which generates an extreme amount of logging
        log.error(err_msg.format(attempts, args, ex.__class__.__name__))
      else:
        log.error(err_msg.format(attempts, args, ex))

      if IAM_API_DELAYED_RETRIES >= attempts:
        sleep(IAM_API_RETRY_DELAY_INTERVAL)
        resp = None
        continue
      else:
        raise ex

    if resp.get("IsTruncated"):
      marker = resp["Marker"]
    else:
      marker = None

    for result in resp["EvaluationResults"]:
      simulation_eval_gauge.increment()
      if result["EvalDecision"] == "allowed":
        if result["EvalResourceName"] not in allowed_actions.keys():
          allowed_actions[result["EvalResourceName"]] = []
        allowed_actions[result["EvalResourceName"]].append(result["EvalActionName"])

  return allowed_actions


def get_authenticator(account_id: str, alt_role_arn_pattern: str = None) -> AwsAuthenticator:
  token = None
  if ServerState().options.get("load_env_creds"):
    for key in ("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY"):
      if key not in environ:
        raise AwsEnvCredRequired(key)
    access_key = environ["AWS_ACCESS_KEY_ID"]
    secret_key = environ["AWS_SECRET_ACCESS_KEY"]
    token = environ.get("AWS_SESSION_TOKEN")
  elif ServerState().options.get("load_tss_creds"):
    creds = get_tss_json("aws_credentials.json")
    access_key = creds["aws_access_key"]
    secret_key = creds["aws_secret_access_key"]
  else:
    access_key = ServerState().options["access_key"]
    secret_key = ServerState().options["secret_key"]
    token = ServerState().options["token"]
  return AwsAuthenticator(account_id, access_key, secret_key, token, alt_role_arn_pattern)


def get_entity_batched_resource_accesses(
  client,
  entity_arn: Arn,
  src_resources: List[Resource],
  simulation_gauge: AtomicGauge,
  simulation_error_gauge: AtomicGauge,
  simulation_eval_gauge: AtomicGauge,
) -> Dict[Resource, List[IamObservedAccess]]:
  resource_access = {}
  resource_arn_to_resource = {}
  for resource in src_resources:
    resource_access[resource] = []
    resource_arn_to_resource[resource.arn.arn] = resource

  for resource_svc, resources in resources_by_service(src_resources).items():
    eval_actions = []
    for level, actions in RESOURCE_ACCESS_POLICY_ACTIONS[resource_svc].items():
      eval_actions = eval_actions + actions

    for resource_arn, allowed_actions in entity_batch_resource_accesses(
      client,
      eval_actions,
      entity_arn.arn,
      [r.arn.arn for r in resources],
      simulation_gauge,
      simulation_error_gauge,
      simulation_eval_gauge,
    ).items():
      access_levels = actions_access_levels(resource_svc, allowed_actions)
      access = IamObservedAccess(entity_arn.arn, resource_arn, access_levels)

      if ServerState().options.get("log_access_observations"):
        log.info(f"{access}")
      resource_access[resource_arn_to_resource[resource_arn]].append(access)

  return resource_access


def get_all_entity_batched_resource_accesses(
  resources: List[Resource],
  auth: AwsAuthenticator,
  iam_entities: Dict[str, List[IamEntity]],
  gauges: Dict[str, AtomicGauge],
) -> Dict[Resource, List[IamObservedAccess]]:
  cache = IamAccessCache(auth.clone())
  cached_accesses = {}
  processed_accesses = {}
  for resource in resources:
    if not ServerState().options.get("disable_access_cache"):
      res = cache.fetch(resource)
      if res and len(res) > 0:
        cached_accesses[resource] = res
        continue
    processed_accesses[resource] = []

  futures = []
  workers = IAM_MAX_API_FUTURES
  with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as ex:
    for _, entities in iam_entities.items():
      for entity in entities:
        futures.append(
          ex.submit(
            get_entity_batched_resource_accesses,
            auth.clone().new_client("iam", region=GLOBAL_API_REGION),
            entity.arn,
            processed_accesses.keys(),
            gauges["access_simulation"],
            gauges["access_simulation_errors"],
            gauges["access_simulation_evaluations"],
          )
        )

    for future in concurrent.futures.as_completed(futures, timeout=FUTURE_TIMEOUTS["processor"]):

      try:
        for resource, resource_accesses in future.result().items():
          gauges["resource_access_processed"].increment()
          gauges["observed_resource_accesses"].add(len(resource_accesses))
          processed_accesses[resource] = processed_accesses[resource] + resource_accesses
      except ResponseParserError:
        log.error("access simulation failed for resource batch due API response parsing errors.")
        # All `get_entity_batched_resource_accesses` futures executed by this method
        # must complete successfully in order to have a complete list of IAM entities
        # that have access to the input resources. In the event that a future exhausts
        # retries for IAM API calls and fails due to response parsing errors return an
        # empty map to effectively skip processing for the input resource batch.
        return {}

  # populate access cache
  if not ServerState().options.get("disable_access_cache"):
    cache.add_batch(processed_accesses)

  return {**cached_accesses, **processed_accesses}


def get_inline_policy_document(client, entity_type: str, entity_name: str, policy_name: str) -> str:
  opts = {"PolicyName": policy_name}

  if entity_type == "group":
    opts["GroupName"] = entity_name
    resp = client.get_group_policy(**opts)
  elif entity_type == "role":
    opts["RoleName"] = entity_name
    resp = client.get_role_policy(**opts)
  elif entity_type == "user":
    opts["UserName"] = entity_name
    resp = client.get_user_policy(**opts)
  else:
    raise AwsInvalidIamEntityType(entity_type)

  return resp["PolicyDocument"]


def get_managed_policy_document(client, policy_arn: str) -> str:
  policy_ver = client.get_policy(PolicyArn=policy_arn)["Policy"]["DefaultVersionId"]

  return client.get_policy_version(PolicyArn=policy_arn, VersionId=policy_ver)["PolicyVersion"][
    "Document"
  ]


def get_entity_access_policies(client, svcs: List[str], arn: str) -> List[Dict[str, str]]:
  policies = []
  marker = None
  resp = None
  opts = {
    "Arn": arn,
    "ServiceNamespaces": svcs,
  }

  while marker or resp is None:
    if marker:
      opts["Marker"] = marker

    resp = client.list_policies_granting_service_access(**opts)
    if resp.get("IsTruncated"):
      marker = resp["Marker"]
    else:
      marker = None

    for namespace in resp["PoliciesGrantingServiceAccess"]:
      policies = policies + namespace["Policies"]

  return policies


def get_all_access_entities(client, entity_type: str, gauge: AtomicGauge) -> List[IamEntity]:
  entities = []
  for resp in client.get_paginator(f"list_{entity_type}s").paginate():
    for entity in resp[entity_type.capitalize() + "s"]:
      arn = entity["Arn"]
      if ServerState().options.get("log_resource_observations"):
        log.info(f"Observed IAM entity: {arn}.")

      entities.append(IamEntity(Arn(arn)))

  gauge.add(len(entities))
  return entities


def get_iam_entity_map(
  auth: AwsAuthenticator, gauges: Dict[str, AtomicGauge]
) -> Dict[str, List[IamEntity]]:
  entities = {}
  future_to_keys = {}

  workers = min(IAM_MAX_API_FUTURES, len(SUPPORTED_IAM_ENTITIES))
  with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as ex:
    log.info(f"Fetching IAM entities. account: {auth.account_id} types: {SUPPORTED_IAM_ENTITIES}")
    for entity_type in SUPPORTED_IAM_ENTITIES:
      client = auth.new_client("iam", region=GLOBAL_API_REGION)
      future_to_keys[
        ex.submit(get_all_access_entities, client, entity_type, gauges[entity_type])
      ] = entity_type

    for future in concurrent.futures.as_completed(
      future_to_keys.keys(), timeout=FUTURE_TIMEOUTS["scanner"]
    ):
      entities[future_to_keys[future]] = future.result()

  return entities
