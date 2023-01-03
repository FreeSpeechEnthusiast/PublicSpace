from enum import auto, Enum
import re
from time import time
from typing import Any, Dict, Optional, Tuple, Union

from twitter.common import app, log
from twitter.common_internal.log.loglens_handler import LogLensHandler
from twitter.kite.utils.kerberos import KerberosTicketRefresher
from util import (
  account_stage_key,
  envoy_proxy_connection_check,
  get_krb_principal,
  get_krb_user,
  terminate_envoy_sidecar,
)

from com.twitter.dal.model.ttypes import ActiveState, DatasetName
from com.twitter.dal.properties.ttypes import LogicalDatasetPropertyKey
from com.twitter.statebird.v2.thriftpython.ttypes import Environment

from aws_arn import Arn, S3Arn
from aws_iam import get_authenticator
from aws_organizations import get_accounts
from dataset_metastore import Metastore
from dataset_stagestore import Stagestore
from kite_role import KiteRole
from registration import app_context, client, kite_client, location
from server_config import (
  AWS_ORGANIZATION_ACCOUNT_ID,
  DYNAMO_STAGES_TABLE_ACCOUNT_ID,
  SVC_DOMAIN,
  SVC_ROLE,
)
from server_state import ServerState


DATASET_OBSV_INTVL = 43200  # seconds
STAGE_OBSV_INTVL = 21600  # seconds


app.add_option("--aws-access-key", default=None, dest="access_key", help="AWS API access key.")
app.add_option(
  "--aws-secret-access-key", default=None, dest="secret_key", help="AWS API secret access key."
)
app.add_option("--aws-token", default=None, dest="token", help="AWS API token.")
app.add_option(
  "--dataset-filter",
  default=None,
  dest="dataset_filter",
  help="Regular expression to apply to DAL dataset names to select for deletion.",
)
app.add_option(
  "--deleted-accounts",
  action="store_true",
  default=False,
  dest="deleted_accounts",
  help="Delete datasets associated with AWS accounts no longer within the Twitter AWS org.",
)
app.add_option(
  "--disable-kerberos",
  action="store_true",
  default=False,
  dest="disable_kerberos",
  help="Disable Kerberos ticket refresher.",
)
app.add_option(
  "--disable-metastore-deletion",
  action="store_true",
  default=False,
  dest="disable_metastore_deletion",
  help="Disable metastore DynamoDB record deletions.",
)
app.add_option(
  "--dry-run",
  action="store_true",
  default=False,
  dest="dry_run",
  help="Disable deletes to Kite, metastore DynamoDB tables and DAL.",
)

app.add_option(
  "--envoy-proxy-url",
  default=None,
  dest="envoy_proxy_url",
  help="Envoy Proxy Url. Enable use of envoy proxy for requests to AWS APIs.",
)

app.add_option(
  "--enable-splunk",
  action="store_true",
  default=False,
  dest="enable_splunk",
  help="Enable loglens/splunk logging.",
)
app.add_option("--env", default="prod", help="DAL target env (staging or prod).")
app.add_option(
  "--load-env-creds",
  action="store_true",
  default=False,
  dest="load_env_creds",
  help="Load AWS credentials from env variables.",
)
app.add_option(
  "--load-tss-creds",
  action="store_true",
  default=False,
  dest="load_tss_creds",
  help="Load credentials from TSS.",
)
app.add_option(
  "--local-dynamodb-create-table",
  action="store_true",
  default=False,
  dest="local_dynamodb_create",
  help="Create a local dynamodb table to use for dataset metastore.",
)
app.add_option(
  "--local-dynamodb-endpoint",
  default=None,
  dest="local_dynamodb_endpoint",
  help="Define a local dynamodb endpoint to use for dataset metastore.",
)
app.add_option(
  "--process-account",
  default=None,
  dest="process_account",
  help="Manually process a single AWS account.",
)
app.add_option(
  "--reconcile",
  action="store_true",
  default=False,
  dest="reconcile",
  help="Purge registration records for datasets that no longer exist.",
)

app.add_option(
  "--s2s",
  type="str",
  help="s2s parameters job(name):role:env:cluster, apply to tls client side service authentication. More info: http://go/s2s",
)


class ProcessReason(Enum):
  DELETED_ACCOUNT = auto()
  FILTER = auto()
  RECONCILE = auto()


def parse_dal_dataset_name(name: str) -> Optional[Tuple[str, str]]:
  pattern = re.compile("^aws-([\d]+)-(([a-z]{2})-([-a-z]+)-([0-9]{1})).+")
  result = pattern.match(name)
  if result and len(result.groups()) >= 2:
    return result.group(1), result.group(2)


def delete_kite_role(properties: Dict[str, Union[None, str]], dry_run: bool = False):
  role = KiteRole(
    properties["resource_name"],
    properties["resource_type"],
    properties["infra_service_name"],
    properties.get("project"),
  )

  if dry_run:
    log.info(f"Supressing Kite role deletion due to --dry-run flag: {properties['resource_name']}")
  else:
    log.info(f"Deleting Kite role: {properties['resource_name']}")
    kite_client().deprecate_role(role)


def delete_metastore_record(account_id: str, arn: str):
  metastore = Metastore(get_authenticator(account_id))
  metastore.delete(metastore.key(Arn(arn)))


def get_metastore_record(account_id: str, arn: str, region: str) -> Dict[str, Any]:
  dataset_arn = Arn(arn)
  if dataset_arn.service == "s3":
    bucket_name = arn[len("arn:aws:s3:::") :]
    dataset_arn = S3Arn(
      bucket_name,
      account_id,
      region,
    )
  metastore = Metastore(get_authenticator(account_id))
  return metastore.get(metastore.key(dataset_arn, True))


def has_metastore_observed(account_id: str, arn: str, region: str) -> Optional[bool]:
  meta_record = get_metastore_record(account_id, arn, region)
  if meta_record and "observed" in meta_record:
    observed_ts = meta_record["observed"]
    observed_elapsed_time = int(time()) - observed_ts
    if DATASET_OBSV_INTVL > observed_elapsed_time:
      return True
    else:
      return False


def has_processed_stage(record: Dict[str, Any]) -> bool:
  if record and "ts" in record:
    if STAGE_OBSV_INTVL > (int(time()) - record["ts"]):
      return True
  return False


def fatal(msg: str):
  log.fatal(msg)
  raise Exception(msg)


def main(args, options):
  ServerState({}, options.__dict__)

  if options.envoy_proxy_url:
    envoy_proxy_connection_check(options.envoy_proxy_url)

  stage_store = Stagestore(get_authenticator(DYNAMO_STAGES_TABLE_ACCOUNT_ID))

  # Validate inputs
  if not any([options.dataset_filter, options.deleted_accounts, options.reconcile]):
    fatal("Input --dataset-filter, --deleted-accounts or --reconcile is required")

  # Logging
  if options.enable_splunk:
    app_id = SVC_ROLE[options.env]
    log.info(f"Enabling splunk logging: app_id={app_id} job_key={SVC_DOMAIN}")
    log.logging.getLogger().addHandler(LogLensHandler(app_id, {"job_key": SVC_DOMAIN}))

  # Kerberos
  if not options.disable_kerberos:
    keytab = "/var/lib/tss/keys/fluffy/keytabs/client/{}.keytab".format(get_krb_user())
    refresher = KerberosTicketRefresher(keytab=keytab, principal=get_krb_principal())
    refresher.start()

  # Dataset filter
  pattern = None
  if options.dataset_filter:
    try:
      pattern = re.compile(options.dataset_filter)
    except re.error as ex:
      fatal(
        f"Unable to compile --dataset-filter regular expression input: {options.dataset_filter}. err: {ex}"
      )

  # Account IDs
  active_accounts = None
  if options.deleted_accounts or options.reconcile:
    active_accounts = get_accounts(get_authenticator(AWS_ORGANIZATION_ACCOUNT_ID), False)

  # Query datasets
  if options.env == "staging":
    dal_env = Environment.STAGING
  else:
    dal_env = Environment.PROD
  dal_role = SVC_ROLE[options.env]
  log.info(f"Querying {options.env} {dal_role} DAL datasets...")
  dal_resp = client().findPhysicalDatasets(
    activeStates=[ActiveState.ACTIVE],
    environment=dal_env,
    filterKeysAndAcceptableValues=None,
    hasPersonalData=None,
    integrationTypes=None,
    keysToHydrate=None,
    logicalDatasetIds=None,
    logicalDatasetKeysToHydrate=[
      LogicalDatasetPropertyKey.AccountableKiteInfrastructureServiceName,
      LogicalDatasetPropertyKey.AccountableKiteResourceName,
      LogicalDatasetPropertyKey.AccountableKiteResourceType,
      LogicalDatasetPropertyKey.AccountableKiteRoleCurrentProject,
    ],
    name=None,
    ownerTeamCodes=None,
    physicalLocations=None,
    requiredKeys=None,
    role=dal_role,
    storageTypes=None,
  )
  if not dal_resp.logicalDatasets or len(dal_resp.logicalDatasets) == 0:
    fatal("Unable to list logical datasets from DAL.")
  if (
    not dal_resp.logicalDatasets
    or len(dal_resp.logicalDatasets) == 0
    or len(dal_resp.physicalDatasets) == 0
  ):
    fatal("Unexpected empty DAL response.")
  log.info(
    f"DAL datasets - physical: {len(dal_resp.physicalDatasets)} logical: {len(dal_resp.logicalDatasets)}"
  )

  # Index dataset ARNs by DAL logical ID
  dataset_arns = {}
  for physical_dataset in dal_resp.physicalDatasets:
    dataset_arns[physical_dataset.logicalDatasetId] = physical_dataset.url.url[len("aws://") :]

  # Process datasets
  for dataset in dal_resp.logicalDatasets:
    arn = dataset_arns.get(dataset.id)
    name = dataset.name.name
    log_sfx = f"Dataset - id: {dataset.id} name: {name}"

    parse_result = parse_dal_dataset_name(name)
    if parse_result:
      account_id, region = parse_result
    else:
      log.error("Unable to parse AWS account ID and region from DAL dataset name. " + log_sfx)
      continue
    if options.process_account and account_id != options.process_account:
      continue

    should_process = False
    if options.deleted_accounts:
      if account_id not in active_accounts:
        should_process = ProcessReason.DELETED_ACCOUNT
    if not should_process and options.dataset_filter and pattern.match(name):
      should_process = ProcessReason.FILTER
    if not should_process and options.reconcile:
      if not has_processed_stage(stage_store.get(account_stage_key(account_id, "observe"))):
        log.warn("Unable to reconcile dataset in unprocessed account. " + log_sfx)
        continue
      if not arn:
        log.warn(
          "Unable to reconcile dataset due to missing DAL physical dataset URL/ARN value. "
          + log_sfx
        )
        continue
      try:
        metastore_observed = has_metastore_observed(account_id, arn, region)
        if metastore_observed == True:
          log.info("Excluding recently metastore observed dataset from deletion. " + log_sfx)
          continue
        elif metastore_observed is None:
          log.warn("Expected metastore dataset record missing. " + log_sfx)
        should_process = ProcessReason.RECONCILE
      except Exception as ex:
        log.error(f"Unable to query metastore value for dataset. ex={ex} " + log_sfx)
        continue

    if should_process:
      log.info(
        f"Dataset selected for deletion - id: {dataset.id} name: {name} reason: {should_process.name}"
      )

      kite_properties = {}
      for k, v in dataset.properties.items():
        if k == LogicalDatasetPropertyKey.AccountableKiteInfrastructureServiceName:
          kite_properties["infra_service_name"] = v.accountableKiteInfrastructureServiceName
        elif k == LogicalDatasetPropertyKey.AccountableKiteResourceName:
          kite_properties["resource_name"] = v.accountableKiteResourceName
        elif k == LogicalDatasetPropertyKey.AccountableKiteResourceType:
          kite_properties["resource_type"] = v.accountableKiteResourceType
        elif k == LogicalDatasetPropertyKey.AccountableKiteRoleCurrentProject:
          kite_properties["project"] = v.accountableKiteRoleCurrentProject

      # Delete Kite role
      if len(kite_properties) >= 3:
        delete_kite_role(kite_properties, options.dry_run)

      # Delete DAL dataset
      if options.dry_run:
        log.info("Supressing DAL dataset deletion due to --dry-run flag. " + log_sfx)
      else:
        log.info("Deleting DAL dataset. " + log_sfx)
        client().setLogicalDatasetDeleted(
          context=app_context(location()),
          datasetName=DatasetName(
            environment=dal_env,
            name=name,
            role=dal_role,
          ),
        )

      # Delete metastore record
      if not options.deleted_accounts:
        if not options.disable_metastore_deletion:
          if arn:
            if options.dry_run:
              log.info(
                "Supressing Metastore record deletion API call due to --dry-run flag. " + log_sfx
              )
            else:
              log.info("Deleting metastore record. " + log_sfx)
              delete_metastore_record(account_id, arn)
          else:
            log.error(
              "Unable to find corresponding physical dataset, metastore values may be orphaned. "
              + log_sfx
            )

  log.info("Processing complete, shutting down.")
  terminate_envoy_sidecar(options.envoy_proxy_url)


app.set_name("aws-pdp-dal-delete-datasets")
app.main()
