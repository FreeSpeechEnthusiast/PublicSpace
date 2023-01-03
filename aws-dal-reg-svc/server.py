import concurrent.futures
from threading import Event
from time import sleep, time
from typing import Dict, List, Optional

from twitter.common import app, log
from twitter.common.metrics import AtomicGauge, MetricSampler, RootMetrics
from twitter.common.metrics.metrics import Metrics
from twitter.common_internal.log.loglens_handler import LogLensHandler
from twitter.kite.utils.kerberos import KerberosTicketRefresher
from util import (
  batch_process_completion_queue_with_snk,
  CompletionQueue,
  envoy_proxy_connection_check,
  fatal,
  get_krb_principal,
  get_krb_user,
  process_completion_queue,
  terminate_envoy_sidecar,
)

from aws_cloud_watch import (
  account_metric,
  CloudWatchReporter,
  get_cloud_watch_reporter,
  put_account_metric,
)
from aws_iam import (
  AwsAuthenticator,
  get_all_entity_batched_resource_accesses,
  get_authenticator,
  get_iam_entity_map,
  IamAccessCacheMetrics,
  IamEntity,
)
from aws_organizations import get_account_tag, get_accounts
from aws_scanners import (
  DAXScanner,
  DynamoDbScanner,
  ElastiCacheClusterScanner,
  ElastiCacheSnapshotScanner,
  get_all_resources,
  KinesisScanner,
  S3Scanner,
  SqsScanner,
)
from botocore.exceptions import ClientError
from dataset import DatasetFilter
from dataset_metastore import Metastore
from dataset_stagestore import Stagestore
from registration import Registrar
from server_config import (
  AWS_ORGANIZATION_ACCOUNT_ID,
  AWS_ORGANIZATION_ACCOUNT_KITE_TAG,
  DYNAMO_STAGES_TABLE_ACCOUNT_ID,
  FUTURE_TIMEOUTS,
  IAM_RSRC_ACCESS_SIML_BATCH_SIZE,
  SERVER_MONITOR_INTVL,
  SUPPORTED_IAM_ENTITIES,
  SVC_DOMAIN,
  SVC_NAME,
  SVC_ROLE,
)
from server_state import ServerState


app.add_option("--aws-access-key", default=None, dest="access_key", help="AWS API access key.")
app.add_option(
  "--aws-secret-access-key", default=None, dest="secret_key", help="AWS API secret access key."
)
app.add_option("--aws-token", default=None, dest="token", help="AWS API token.")
app.add_option(
  "--disable-access-cache",
  action="store_true",
  default=False,
  dest="disable_access_cache",
  help="Disable caching of resource access entities.",
)
app.add_option(
  "--disable-access-simulation",
  action="store_true",
  default=False,
  dest="disable_access_simulation",
  help="Disable AWS IAM access simulation API calls.",
)
app.add_option(
  "--disable-kerberos",
  action="store_true",
  default=False,
  dest="disable_kerberos",
  help="Disable Kerberos ticket refresher.",
)
app.add_option(
  "--disable-kite-registration",
  action="store_true",
  default=False,
  dest="disable_kite",
  help="Disable Kite registration.",
)
app.add_option(
  "--disable-resource-filter",
  action="store_true",
  default=False,
  dest="disable_resource_filter",
  help="Disable exclusion of dataset resources that have been recently registered.",
)
app.add_option(
  "--disable-retention-processing",
  action="store_true",
  default=False,
  dest="disable_retention_processing",
  help="Disable additional API calls related to dataset retention registration.",
)
app.add_option(
  "--dry-run",
  action="store_true",
  default=False,
  dest="dry_run",
  help="Disable writes to Kite, metastore DynamoDB tables and DAL.",
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
app.add_option("--env", default="staging", help="DAL target env (staging or prod).")
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
  "--log-access-observerations",
  action="store_true",
  default=False,
  dest="log_access_observations",
  help="Enable log entries for all AWS accesses observed.",
)
app.add_option(
  "--log-metrics",
  action="store_true",
  default=False,
  dest="log_metrics",
  help="Enable log entries for CloudWatch metrics emitted.",
)
app.add_option(
  "--log-resource-observations",
  action="store_true",
  default=False,
  dest="log_resource_observations",
  help="Enable log entries for all AWS resources observed.",
)
app.add_option(
  "--process-account",
  default=None,
  dest="process_account",
  help="Manually process a single AWS account.",
)

app.add_option(
  "--s2s",
  type="str",
  help="s2s parameters job(name):role:env:cluster, apply to tls client side service authentication. More info: http://go/s2s",
)


def get_account_default_project(account_id: str) -> Optional[str]:
  auth = get_authenticator(AWS_ORGANIZATION_ACCOUNT_ID)

  # fetch Kite project from AWS organization account tags
  return get_account_tag(auth, account_id, AWS_ORGANIZATION_ACCOUNT_KITE_TAG)


def get_iam_entities(
  auth: AwsAuthenticator, account_metrics: Metrics
) -> Dict[str, List[IamEntity]]:
  entity_gagues = {}
  for entity_type in SUPPORTED_IAM_ENTITIES:
    entity_gagues[entity_type] = account_metrics.register(AtomicGauge(f"{entity_type}s_observed"))

  return get_iam_entity_map(auth, entity_gagues)


def monitor(
  accounts: List[int], shutdown_event: Event, reporter: CloudWatchReporter, stage_store: Stagestore
):
  stage_queue = CompletionQueue()
  ServerState().set_stage_store(stage_store)
  ServerState().set_stage_queue(stage_queue)
  while not shutdown_event.is_set():
    sleep(SERVER_MONITOR_INTVL)
    try:
      log.info(f"{ServerState()}")
      process_completion_queue(stage_queue, put_account_metric, {"reporter": reporter}, True)
      stats = MetricSampler(RootMetrics()).sample()
      reporter.process(stats)
      ServerState().monitor(accounts, stats)
    except Exception as ex:
      log.error(f"Server monitor error: {ex}")


def setup_logger(enable_splunk: bool, env: str):
  if enable_splunk:
    app_id = SVC_ROLE[env]
    log.info(f"Enabling splunk logging: app_id={app_id} job_key={SVC_DOMAIN}")
    log.logging.getLogger().addHandler(LogLensHandler(app_id, {"job_key": SVC_DOMAIN}))


def setup_kerberos():
  """
  Refreshes Kerberos credential caches at a set interval using kinit.
  """
  keytab = "/var/lib/tss/keys/fluffy/keytabs/client/{}.keytab".format(get_krb_user())
  refresher = KerberosTicketRefresher(keytab=keytab, principal=get_krb_principal())
  refresher.start()


def start_scanner_future(
  auth: AwsAuthenticator,
  executor: concurrent.futures.ThreadPoolExecutor,
  metrics: Metrics,
  snk: CompletionQueue,
) -> concurrent.futures.Future:
  dax_scanner = DAXScanner(auth.account_id, auth)
  metrics.register_observable("dax_scanner", dax_scanner)
  dynamodb_scanner = DynamoDbScanner(auth.account_id, auth)
  metrics.register_observable("dynamodb_scanner", dynamodb_scanner)
  kinesis_scanner = KinesisScanner(auth.account_id, auth)
  metrics.register_observable("kinesis_scanner", kinesis_scanner)
  s3_scanner = S3Scanner(auth.account_id, auth)
  metrics.register_observable("s3_scanner", s3_scanner)
  sqs_scanner = SqsScanner(auth.account_id, auth)
  metrics.register_observable("sqs_scanner", sqs_scanner)
  elasticachecluster_scanner = ElastiCacheClusterScanner(auth.account_id, auth)
  metrics.register_observable("elasticachecluster_scanner", elasticachecluster_scanner)
  elasticachesnapshot_scanner = ElastiCacheSnapshotScanner(auth.account_id, auth)
  metrics.register_observable("elasticachesnapshot_scanner", elasticachesnapshot_scanner)
  return executor.submit(
    get_all_resources,
    auth.account_id,
    snk,
    dax_scanner,
    dynamodb_scanner,
    kinesis_scanner,
    s3_scanner,
    sqs_scanner,
    elasticachecluster_scanner,
    elasticachesnapshot_scanner,
  )


def start_filter_future(
  auth: AwsAuthenticator,
  executor: concurrent.futures.ThreadPoolExecutor,
  metrics: Metrics,
  src: CompletionQueue,
  snk: CompletionQueue,
) -> concurrent.futures.Future:
  resource_filter = DatasetFilter(Metastore(auth), snk)
  metrics.register_observable("resource_filter", resource_filter)
  return executor.submit(
    process_completion_queue,
    src,
    resource_filter.process,
    completion_callback=snk.set_completed,
    stage=f"{auth.account_id}.filter",
  )


def start_access_simulation_future(
  auth: AwsAuthenticator,
  executor: concurrent.futures.ThreadPoolExecutor,
  iam_entities: Dict[str, List[IamEntity]],
  metrics: Metrics,
  src: CompletionQueue,
  snk: CompletionQueue,
) -> concurrent.futures.Future:
  args = {
    "auth": auth,
    "iam_entities": iam_entities,
    "gauges": {
      "access_simulation": metrics.register(AtomicGauge("access_simulation_api_calls")),
      "access_simulation_errors": metrics.register(AtomicGauge("access_simulation_api_errors")),
      "access_simulation_evaluations": metrics.register(
        AtomicGauge("access_simulation_api_evaluations")
      ),
      "observed_resource_accesses": metrics.register(AtomicGauge("observed_resource_accesses")),
      # this metric can be used to calculate progress during
      # the access simulation stage as each resource is simulated
      # against each access entity. the simulation stage completes
      # when this metric reaches the product of the sum of access entities
      # and the sum of emitted dataset resources.
      "resource_access_processed": metrics.register(AtomicGauge("resource_access_processed")),
    },
  }
  return executor.submit(
    batch_process_completion_queue_with_snk,
    IAM_RSRC_ACCESS_SIML_BATCH_SIZE,
    src,
    snk,
    get_all_entity_batched_resource_accesses,
    args,
    completion_callback=snk.set_completed,
    stage=f"{auth.account_id}.access_simulation",
  )


def start_registration_future(
  auth: AwsAuthenticator,
  executor: concurrent.futures.ThreadPoolExecutor,
  metrics: Metrics,
  src: CompletionQueue,
) -> concurrent.futures.Future:
  registrar = Registrar(Metastore(auth))
  metrics.register_observable("registrar", registrar)
  return executor.submit(
    process_completion_queue,
    src,
    registrar.register_datasets,
    stage=f"{auth.account_id}.registration",
  )


def main(args, options):
  ServerState({"account_default_projects": {}, "server_start": time()}, options.__dict__)

  if options.envoy_proxy_url:
    envoy_proxy_connection_check(options.envoy_proxy_url)

  setup_logger(options.enable_splunk, options.env)
  if not options.disable_kerberos:
    setup_kerberos()

  log.info(f"Initializing service with env: {options.env}.")
  if options.dry_run:
    log.info("Disabling all writes due to --dry-run option.")
  if not (options.load_env_creds or options.load_tss_creds):
    if not all([options.access_key, options.secret_key]):
      fatal(
        "options `--aws-access-key` and `--aws-secret-access-key` are required unless loading creds from TSS."
      )
  if options.local_dynamodb_create:
    if not options.local_dynamodb_endpoint:
      fatal(
        "option `--local-dynamodb-create` can only be used if option `--local-dynamodb-endpoint` is defined."
      )
  if options.local_dynamodb_endpoint:
    log.info("Enabling use of local dynamodb instance.")
  if options.process_account:
    log.info(f"Processing individual AWS account: {options.process_account}")

  accounts = get_accounts(get_authenticator(AWS_ORGANIZATION_ACCOUNT_ID))
  shutdown_event = Event()
  stage_store = Stagestore(get_authenticator(DYNAMO_STAGES_TABLE_ACCOUNT_ID))
  with concurrent.futures.ThreadPoolExecutor(max_workers=1) as monitor_ex:
    # the worker account multiplier should match the number of
    # futures / pipeline stages defined below.
    workers = len(accounts) * 4
    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as ex:
      futures = []
      for account_id in accounts:
        account_metrics = RootMetrics().scope(account_id)
        cw_reporter = get_cloud_watch_reporter(SVC_ROLE[options.env])

        try:
          auth = get_authenticator(account_id)
          default_project = get_account_default_project(account_id)

          if default_project:
            ServerState().meta["account_default_projects"][account_id] = default_project
          else:
            log.warn(f"Unable to detect default Kite project for account: {account_id}")
            put_account_metric(account_metric(account_id, "default_project_missing"), cw_reporter)
        except ClientError as exception:
          if exception.response["Error"]["Code"] in ("AccessDenied", "AccessDeniedException"):
            log.error(f"Unable to authenticate to AWS account: {account_id}. ex={exception}")
            continue
          else:
            raise exception

        # fetch IAM entities
        if not options.disable_access_cache:
          IamAccessCacheMetrics().register_metrics(account_metrics)
        iam_entities = get_iam_entities(auth.clone(), account_metrics)
        for svc, entities in iam_entities.items():
          log.info(f"Total {account_id}/{svc} access entities: {len(entities)}")

        # stage: scan
        resource_snk = CompletionQueue()
        futures.append(start_scanner_future(auth.clone(), ex, account_metrics, resource_snk))

        # stage filter
        filtered_resource_snk = CompletionQueue()
        futures.append(
          start_filter_future(
            auth.clone(), ex, account_metrics, resource_snk, filtered_resource_snk
          )
        )

        # stage: access_simulation
        resource_access_snk = CompletionQueue()
        futures.append(
          start_access_simulation_future(
            auth.clone(),
            ex,
            iam_entities,
            account_metrics,
            filtered_resource_snk,
            resource_access_snk,
          )
        )

        # stage: registration
        futures.append(
          start_registration_future(auth.clone(), ex, account_metrics, resource_access_snk)
        )

      monitor_future = monitor_ex.submit(
        monitor, accounts, shutdown_event, cw_reporter, stage_store
      )
      for future in concurrent.futures.as_completed(futures, timeout=FUTURE_TIMEOUTS["main"]):
        _ = future.result()

    log.info("processing complete, shutting down.")
    terminate_envoy_sidecar(options.envoy_proxy_url)

    shutdown_event.set()
    monitor_future.result()


app.set_name(SVC_NAME)
app.main()
