from datetime import datetime
from time import time
from typing import Dict, List, Optional

from com.twitter.dal import DAL
from twitter.common import log
from twitter.common.metrics import AtomicGauge, Observable
from twitter.ml.common.thrift_client_connector import ThriftClientConnector
from twitter.s2s.core import ServiceIdentifier
from util import current_ms_time, get_krb_principal, jitter_ttl, rate_limit

from com.twitter.dal.has_personal_data.ttypes import HasPersonalData
from com.twitter.dal.model.ttypes import (
  AccessGroupPermissions,
  ApplicationContext,
  LogicalDatasetPersonalDataAnnotations,
  PersonalDataDatasetAnnotations,
  PersonalDataFieldAnnotation,
  PhysicalDatasetRegistrationSpec,
  PhysicalLocation,
  PhysicalLocationType,
)
from com.twitter.dal.properties.ttypes import (
  LogicalDatasetPropertyKey,
  LogicalDatasetPropertyValue,
  RecordsClasses,
  RecordsClassInfo,
)
from com.twitter.dal.ttypes import RegisterDatasetResponse
from com.twitter.statebird.v2.thriftpython.ttypes import BatchApp, Environment

from aws_arn import Arn
from aws_iam import IamObservedAccess
from aws_resource import Resource
from dataset import Dataset
from dataset_metastore import Metastore
from errors import RegistrationError
from registration_base_modules.kite_client import KiteClient
from server_config import (
  DAL_CLIENT_NAME,
  DAL_RATE_LIMIT,
  DAL_SERVICE_NAME,
  REGISTRATION_EXCLUSION_MAX_INTERVAL,
  REGISTRATION_EXCLUSION_MIN_INTERVAL,
  SVC_DOMAIN,
  SVC_NAME,
  SVC_ROLE,
)
from server_state import ServerState


DATASET_LOCATION_TYPE = PhysicalLocationType(name="aws-region")


def has_annotations(annotations: Optional[LogicalDatasetPersonalDataAnnotations] = None) -> bool:
  if annotations:
    if len(annotations.datasetAnnotations.personalDataTypes) > 0:
      return True
    elif len(annotations.datasetFieldAnnotations) > 0:
      return True

  return False


def annotations(dataset: Dataset) -> Optional[LogicalDatasetPersonalDataAnnotations]:
  dataset_annotations = dataset.annotations()
  if dataset_annotations:
    if isinstance(dataset_annotations, dict):
      field_annotations = set()
      for field, pdts in dataset_annotations.items():
        if isinstance(pdts, list) and len(pdts) > 0:
          field_annotations.add(
            PersonalDataFieldAnnotation(fieldName=field, personalDataTypes=frozenset(pdts))
          )
      return LogicalDatasetPersonalDataAnnotations(
        datasetAnnotations=PersonalDataDatasetAnnotations(personalDataTypes=frozenset()),
        datasetFieldAnnotations=field_annotations,
        keyValueDatasetAnnotations=None,
      )
    elif isinstance(dataset_annotations, list):
      return LogicalDatasetPersonalDataAnnotations(
        datasetAnnotations=PersonalDataDatasetAnnotations(
          personalDataTypes=frozenset(dataset_annotations)
        ),
        datasetFieldAnnotations=frozenset(),
        keyValueDatasetAnnotations=None,
      )


def records_classes(
  dataset: Dataset,
) -> Optional[Dict[LogicalDatasetPropertyKey, LogicalDatasetPropertyValue]]:
  dataset_records_classes = dataset.records_classes()
  if dataset_records_classes:
    return {
      LogicalDatasetPropertyKey.RecordsClassInfo: LogicalDatasetPropertyValue(
        recordsClassInfo=RecordsClassInfo(
          recordsClasses=RecordsClasses(rrsCodes=frozenset(dataset_records_classes)),
          updatedAtMillis=current_ms_time(),
          producedByApp=app(),
        )
      )
    }
  return None


def app() -> BatchApp:
  if ServerState().options["env"] == "staging":
    env = Environment.STAGING
  else:
    env = Environment.PROD
  return BatchApp(
    domain=SVC_DOMAIN, environment=env, name=SVC_NAME, role=SVC_ROLE["prod"]
  )  # dmarwick has asked for the service to use a single role for both envs


def app_context(location: PhysicalLocation) -> ApplicationContext:
  return ApplicationContext(appComponent=app(), appType="aws-to-dal", deploymentLocation=location)


def service_identifier() -> ServiceIdentifier:
  if ServerState().options.get("s2s"):
    s2s_params = ServerState().options.get("s2s").split(":")
    if len(s2s_params) != 4:
      log.info(f"Failed to initialize S2S ServiceIdentifier: {ServerState().options.get('s2s')}")
      return None
    log.info(f"Initializing S2S ServiceIdentifier: {ServerState().options.get('s2s')}")
    return ServiceIdentifier(
      service=s2s_params[0],
      role=s2s_params[1],
      environment=s2s_params[2],
      zone=s2s_params[3],
    )
  else:
    return None


def client():
  return ThriftClientConnector(
    client_iface=DAL,
    service_name=DAL_SERVICE_NAME[ServerState().options["env"]],
    client_name=DAL_CLIENT_NAME,
    service_identifier=service_identifier(),
  ).connect()


def kite_client() -> KiteClient:
  return KiteClient(env=ServerState().options["env"], principal=get_krb_principal())


def cluster_name(arn: Arn) -> str:
  return ":".join([arn.partition, arn.region])


def dataset_properties(
  dataset: Dataset,
) -> Optional[Dict[LogicalDatasetPropertyKey, LogicalDatasetPropertyValue]]:
  properties = {}

  if dataset.retention:
    properties.update(dataset.retention)

  classification_level = dataset.data_classification_level()
  if classification_level:
    properties.update(
      {
        LogicalDatasetPropertyKey.DataClassificationLevel: LogicalDatasetPropertyValue(
          dataClassificationLevel=classification_level
        )
      }
    )
  rec_classes = records_classes(dataset)
  if rec_classes:
    properties.update(rec_classes)

  if len(properties) == 0:
    return None

  return properties


def location() -> PhysicalLocation:
  return PhysicalLocation(name="global", locationType=PhysicalLocationType(name="global"))


def register_dataset(dataset: Dataset) -> RegisterDatasetResponse:
  """
  If Kite project info is not available, register with the legacy API when using owner team.
  """
  if dataset.project():
    return register_dataset_v2(dataset)
  else:
    return register_dataset_v1(dataset)


def register_dataset_v1(dataset: Dataset) -> RegisterDatasetResponse:
  owner, owner_type = dataset.owner()
  return client().registerOrUpdateDataset(
    clusterName=cluster_name(dataset.resource.arn),
    context=app_context(location()),
    datasetName=dataset.name(),
    datasetOwnerAccount=owner,
    datasetOwnerAccountType=owner_type,
    datasetUrl=dataset.properties()["URL"],
    hasPersonalData=dataset.contains_pii(),
    logicalDatasetPersonalDataAnnotations=annotations(dataset),
    logicalDatasetProperties=dataset_properties(dataset),
    physicalLocationType=DATASET_LOCATION_TYPE,
    schema=dataset.schema(),
    segmentType=dataset.properties()["segment_type"],
    storageType=dataset.properties()["storage_type"],
  )


def register_dataset_v2(dataset: Dataset) -> RegisterDatasetResponse:
  """
  Register dataset with V2 registration API using accountable Kite role.
  https://docbird.twitter.biz/dal/dal_dataset_registration_for_storage_owners.html
  """
  accountableEntity = dataset.kite_role().accountable_entity()
  dataset_annotations = annotations(dataset)

  physicalDatasetRegistrationSpecs = [
    PhysicalDatasetRegistrationSpec(
      physicalLocation=PhysicalLocation(
        locationType=DATASET_LOCATION_TYPE,
        name=cluster_name(dataset.resource.arn),
      ),
      url=dataset.properties()["URL"],
    )
  ]

  # The V2 registration method rejects registrations where PII status
  # is set to `YES_PERSONAL_DATA` and no PDT annotations are configured.

  pii_status = dataset.contains_pii()
  schema = dataset.schema()
  # If genericSchema is set, set pii_status to None as DAL rejects sending
  # pii_status via genericSchema and via the hasPersonalData field to avoid ambiguity.
  if (
    pii_status == HasPersonalData.YES_PERSONAL_DATA and not has_annotations(dataset_annotations)
  ) or schema.genericSchema:
    pii_status = None

  response_v2 = client().registerDatasetV2(
    accountableEntity=accountableEntity,
    context=app_context(location()),
    datasetName=dataset.name(),
    hasPersonalData=pii_status,
    logicalDatasetPersonalDataAnnotations=dataset_annotations,
    logicalDatasetPropertiesToDelete=None,
    properties=dataset_properties(dataset),
    physicalDatasetRegistrationSpecs=physicalDatasetRegistrationSpecs,
    schema=schema,
    segmentType=dataset.properties()["segment_type"],
    storageType=dataset.properties()["storage_type"],
  )

  # Convert RegisterDatasetV2Response to RegisterDatasetResponse
  # unpacking since we're sure there's always only one element in the set
  (physicalDataset,) = response_v2.physicalDatasets
  return RegisterDatasetResponse(
    logicalDataset=response_v2.logicalDataset, physicalDataset=physicalDataset
  )


def register_dataset_access(dataset: Dataset, dataset_id: int):
  if ServerState().options.get("disable_access_simulation"):
    return

  permissions = []
  for access in dataset.accesses:
    access_group = getattr(access, "entity_arn")
    if not access_group:
      access_group = getattr(access, "ldap_group")

    permissions.append(
      AccessGroupPermissions(
        accessGroup=access_group,
        readAccess="read" in access.levels,
        writeAccess="write" in access.levels,
      )
    )

  client().setPhysicalDatasetAccessGroups(
    context=app_context(location()), physicalDatasetId=dataset_id, permissions=permissions
  )


class Registrar(Observable):
  def __init__(self, metastore: Metastore):
    self._dal_errors = self.metrics.register(AtomicGauge("dal_errors"))
    self._datasets_registered = self.metrics.register(AtomicGauge("datasets_registered"))
    self._datasets_registered_late = self.metrics.register(AtomicGauge("datasets_registered_late"))
    self._datasets_registered_kite = self.metrics.register(AtomicGauge("datasets_registered_kite"))
    self._dataset_access_registered = self.metrics.register(
      AtomicGauge("datasets_access_registered")
    )
    self.metastore = metastore

  def register_access(self, dataset: Dataset, dataset_id: int):
    if len(dataset.accesses) == 0:
      return

    log.info(f"registering dataset access: {dataset.resource.arn} - {dataset.accesses}")
    if not ServerState().options.get("dry_run"):
      start_time = time()
      try:
        register_dataset_access(dataset, dataset_id)
      except Exception as dal_ex:
        log.exception(f"DAL dataset access registration error: {dal_ex}")
        self._dal_errors.increment()
        raise RegistrationError(dal_ex)

      self._dataset_access_registered.increment()
      rate_limit(DAL_RATE_LIMIT, start_time)

  def register_dataset(self, dataset: Dataset) -> RegisterDatasetResponse:
    dataset.set_meta(self.metastore.get_regional_or_global(dataset.resource.arn))
    metastore_key = self.metastore.key(dataset.resource.arn)
    if dataset.resource.creation_date != datetime.min:
      self.metastore.set_created_at(metastore_key, int(dataset.resource.creation_date.timestamp()))
    # Getting the Register record from DynamoDB
    record = self.metastore.get(metastore_key)
    log.info(
      f"registered details: {record}, project: {dataset.project()}, pii: {dataset.contains_pii()}"
    )

    if record and "Item" in record:
      created_at, registered_at = None, None
      # Fetching the created_at value
      if "created_at" in record["Item"]:
        created_at = record["Item"]["created_at"]

      # Fetching the registered value
      if "registered" in record["Item"]:
        registered_at = record["Item"]["registered"]

      # Checking the latency between created_at and registered_at. If Latency is more than 60 mins, increment the metric
      if created_at and registered_at and registered_at < (created_at + 3600000):
        self._datasets_registered_late.increment()

    log.info(
      f"registering dataset: {dataset}, project: {dataset.project()}, pii: {dataset.contains_pii()}"
    )
    if not ServerState().options.get("dry_run"):
      start_time = time()
      try:

        resp = register_dataset(dataset)
      except Exception as dal_ex:
        log.exception(f"DAL registration error: {dal_ex}")
        self._dal_errors.increment()
        raise RegistrationError(dal_ex)

      self._datasets_registered.increment()
      self.metastore.set_refresh_at(
        metastore_key,
        int(start_time)
        + jitter_ttl(
          REGISTRATION_EXCLUSION_MAX_INTERVAL,
          REGISTRATION_EXCLUSION_MIN_INTERVAL / REGISTRATION_EXCLUSION_MAX_INTERVAL,
        ),
      )
      self.metastore.set_registered(metastore_key)
      rate_limit(DAL_RATE_LIMIT, start_time)
      return resp

  def register_kite_role(self, dataset: Dataset):
    kite_role = dataset.kite_role()
    if kite_role:
      log.info(f"registering kite role: {kite_role}")
      if not ServerState().options.get("dry_run"):
        kite_client().set_role(kite_role)
        self._datasets_registered_kite.increment()
    else:
      log.info(f"Kite role not built, skipping creating kite role for {dataset}.")

  def register_datasets(self, datasets: Dict[Resource, List[IamObservedAccess]]):
    for resource, accesses in datasets.items():
      try:
        dataset = Dataset(resource, accesses)
      except Exception as ex:
        log.error(f"Unable to instantiate Dataset class for resource: {resource}. ex={ex}")
        continue

      try:
        resp = self.register_dataset(dataset)
      except RegistrationError:
        log.error(f"dataset registration failed for dataset: {dataset}")
        continue

      try:
        if resp:
          self.register_access(dataset, resp.physicalDataset.id)
      except RegistrationError:
        log.error(f"access registration failed for dataset: {dataset}")

      try:
        self.register_kite_role(dataset)
      except Exception as e:
        log.debug(f"kite role registration failed for dataset: {dataset}\n{e}")
