from collections import defaultdict
import csv
from datetime import datetime
import sys

from twitter.common import app, log
from util import account_stage_key

from aws_iam import get_authenticator
from aws_organizations import get_account_tag, get_accounts
from botocore.exceptions import ClientError
from dataset_stagestore import Stagestore
from server_config import (
  AWS_ORGANIZATION_ACCOUNT_ID,
  AWS_ORGANIZATION_ACCOUNT_KITE_TAG,
  DYNAMO_STAGES_TABLE_ACCOUNT_ID,
)
from server_state import ServerState


app.add_option("--aws-access-key", default=None, dest="access_key", help="AWS API access key.")
app.add_option(
  "--aws-secret-access-key", default=None, dest="secret_key", help="AWS API secret access key."
)
app.add_option("--aws-token", default=None, dest="token", help="AWS API token.")
app.add_option("--env", default="prod", help="DAL target env (staging or prod).")
app.add_option(
  "--load-env-creds",
  action="store_true",
  default=False,
  dest="load_env_creds",
  help="Load AWS credentials from env variables.",
)


def main(args, options):
  ServerState({}, options.__dict__)
  aws_org_auth = get_authenticator(AWS_ORGANIZATION_ACCOUNT_ID)
  accounts = get_accounts(aws_org_auth)
  stage_store = Stagestore(get_authenticator(DYNAMO_STAGES_TABLE_ACCOUNT_ID))
  status = defaultdict(
    lambda: {
      "ACCESS_STATUS": "UNKNOWN",
      "DEFAULT_KITE_PROJECT": "UNKNOWN",
      "LAST_OBSERVED": "UNKNOWN",
      "LAST_SCANNED": "UNKNOWN",
      "LAST_PROCESSED": "UNKNOWN",
    }
  )

  for account_id in accounts:
    try:
      status[account_id]["ACCESS_STATUS"] = "ACTIVE"
    except ClientError as exception:
      log.error(
        f"Unable to authenticate to PDP role in account: {account_id}. exception={exception}"
      )
      status[account_id]["ACCESS_STATUS"] = "ERROR"

    if status[account_id]["ACCESS_STATUS"] == "ACTIVE":
      try:
        default_project = get_account_tag(
          aws_org_auth, account_id, AWS_ORGANIZATION_ACCOUNT_KITE_TAG
        )
        status[account_id]["DEFAULT_KITE_PROJECT"] = default_project
      except ClientError as exception:
        log.error(f"Unable to get the account project tag: {account_id}. exception={exception}")
        if exception.response["Error"]["Code"] in ("AccessDenied", "AccessDeniedException"):
          status[account_id]["DEFAULT_KITE_PROJECT"] = "ACCESS_ERROR"
        elif exception.response["Error"]["Code"] == "ParameterNotFound":
          status[account_id]["DEFAULT_KITE_PROJECT"] = "UNDEFINED"

      # query stage store
      try:
        for stage in (
          ("observe", "LAST_OBSERVED"),
          ("scan", "LAST_SCANNED"),
          ("registration", "LAST_PROCESSED"),
        ):
          stage_name, report_key = stage
          stage_resp = stage_store.get(account_stage_key(account_id, stage_name))
          if stage_resp and "ts" in stage_resp:
            ts = stage_resp["ts"]
            status[account_id][report_key] = (
              datetime.utcfromtimestamp(ts).strftime("%Y-%m-%d %H:%M:%S") + " UTC"
            )
      except ClientError as exception:
        log.error(
          f"Unable to query for stage store for account: {account_id}. exception={exception}"
        )

  tsv_writer = csv.writer(sys.stdout, delimiter="\t", lineterminator="\n")
  tsv_writer.writerow(
    [
      "ACCOUNT_ID",
      "ACCESS_STATUS",
      "DEFAULT_KITE_PROJECT",
      "LAST_OBSERVED",
      "LAST_SCANNED",
      "LAST_PROCESSED",
    ]
  )
  for account_id, properties in status.items():
    tsv_writer.writerow(
      [
        account_id,
        status[account_id]["ACCESS_STATUS"],
        status[account_id]["DEFAULT_KITE_PROJECT"],
        status[account_id]["LAST_OBSERVED"],
        status[account_id]["LAST_SCANNED"],
        status[account_id]["LAST_PROCESSED"],
      ]
    )


app.set_name("aws-pdp-account-status")
app.main()
