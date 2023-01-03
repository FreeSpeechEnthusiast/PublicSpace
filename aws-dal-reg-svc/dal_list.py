import json

from twitter.common import app, log

from com.twitter.dal.model.ttypes import ActiveState
from com.twitter.dal.properties.ttypes import LogicalDatasetPropertyKey
from com.twitter.statebird.v2.thriftpython.ttypes import Environment

from registration import client
from server_config import SVC_ROLE
from server_state import ServerState


app.add_option("--env", default="prod", help="DAL target env (staging or prod).")

app.add_option(
  "--s2s",
  type="str",
  help="s2s parameters job(name):role:env:cluster, apply to tls client side service authentication. More info: http://go/s2s",
)


def main(args, options):
  ServerState({}, options.__dict__)
  if options.env == "staging":
    env = Environment.STAGING
  else:
    env = Environment.PROD

  role = SVC_ROLE[options.env]
  resp = client().findPhysicalDatasets(
    activeStates=[ActiveState.ACTIVE],
    environment=env,
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
    role=role,
    storageTypes=None,
  )

  log.info(f"AWS DAL physical datasets: {len(resp.physicalDatasets)}")
  for dataset in resp.physicalDatasets:
    log.info(dataset.url.url[len("aws://") :])

  if resp.logicalDatasets:
    log.info(f"AWS DAL logical datasets: {len(resp.logicalDatasets)}")
    for dataset in resp.logicalDatasets:
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

      team = dataset.ownerTeamCode
      if not team:
        team = "None"

      log.info(f"{' - '.join([dataset.name.name, team, json.dumps(kite_properties)])}")


app.set_name("aws-pdp-dal-list")
app.main()
