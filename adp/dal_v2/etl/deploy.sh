#!/bin/sh
set -e

ENV=$1
APP_NAME=$2
CLUSTER=${CLUSTER:-atla}
DRY_RUN=$3

USAGE="Usage: $0 <staging|prod> <jobname> <dryrun (default: true)>"

if [ "$ENV" == "staging" ]; then
  ROLE=dal-staging
elif [ "$ENV" == "prod" ]; then
  ROLE=dal
else
  echo $USAGE
  echo "ERROR: unknown environment '$ENV'"
  exit 1
fi

if [ "$APP_NAME" = "dal-etl-application" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/application:bin
elif [ "$APP_NAME" = "dal-etl-dataset" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/dataset:bin
elif [ "$APP_NAME" = "dal-etl-taxonomy" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/taxonomy:bin
elif [ "$APP_NAME" = "dal-etl-graph-summaries" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/graph_summaries:bin
elif [ "$APP_NAME" = "dal-etl-data-usage-update" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/usage:bin
elif [ "$APP_NAME" = "dal-etl-eagleeye-users-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/eagleeye_users:bin
elif [ "$APP_NAME" = "dal-etl-upstream-alert-score-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/upstream_alert_scores:bin
elif [ "$APP_NAME" = "dal-etl-eagleeye-roles-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/eagleeye_roles:bin
elif [ "$APP_NAME" = "dal-etl-update-search-entries" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/searchentries:bin
elif [ "$APP_NAME" = "dal-etl-presto-flag-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/presto_flag_updater:bin
elif [ "$APP_NAME" = "dal-etl-presto-usage-summary" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/presto_usage_summary:bin
elif [ "$APP_NAME" = "dal-etl-update-from-source-app" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/update_from_source:bin
elif [ "$APP_NAME" = "dal-etl-related-datasets-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/related_datasets:bin
elif [ "$APP_NAME" = "dal-etl-update-active-dataset-health-reports" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/update_active_dataset_health_reports:bin
elif [ "$APP_NAME" = "dal-etl-update-physical-dataset-delay-threshold" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/pds_delay_threshold:bin
elif [ "$APP_NAME" = "dal-etl-gcs-segment-clean-up" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/gcs_segment_clean_up:bin
elif [ "$APP_NAME" = "dal-etl-dataset-twadoop-config-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/twadoop_config_updater:bin
elif [ "$APP_NAME" = "dal-etl-update-app-health-reports" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/update_app_health_reports:bin
elif [ "$APP_NAME" = "dal-etl-dal-dataset-owners-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/dal_dataset_owners:bin
elif [ "$APP_NAME" = "dal-etl-dasms-permissions-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/dasms_permissions:bin
elif [ "$APP_NAME" = "dal-etl-dmo-data-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/dmo:bin
elif [ "$APP_NAME" = "dal-etl-physical-dataset-root-url-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/physical_dataset_root_url_updater:bin
elif [ "$APP_NAME" = "dal-etl-update-physical-dataset-path" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/update_physical_dataset_path:bin
elif [ "$APP_NAME" = "dal-etl-dret-jira-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/dret_jira:bin
elif [ "$APP_NAME" = "dal-etl-hdfs-dataset-permissions-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/hdfs_dataset_permissions:bin
elif [ "$APP_NAME" = "dal-etl-update-replication-location-path" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/update_replication_location_path:bin
elif [ "$APP_NAME" = "dal-etl-resync-eagleeye-dalv2-data-dashboards" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/resync_eagleeye_dalv2_data:dashboards_bin
elif [ "$APP_NAME" = "dal-etl-strato-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/strato:bin
elif [ "$APP_NAME" = "dal-etl-scrubbing-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/scrubbing:bin
elif [ "$APP_NAME" = "dal-etl-resync-eagleeye-dalv2-dal-apps" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/resync_eagleeye_dalv2_data:apps_bin
elif [ "$APP_NAME" = "dal-etl-resync-eagleeye-dalv2-dal-apps-summaries-and-descriptions" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/resync_eagleeye_dalv2_data:apps_summaries_and_descriptions_bin
elif [ "$APP_NAME" = "dal-etl-retention-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/retention:bin
elif [ "$APP_NAME" = "dal-etl-resync-eagleeye-dalv2-dal-apps-properties" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/resync_eagleeye_dalv2_data:apps_properties_bin
elif [ "$APP_NAME" = "dal-etl-gcs-partly-cloudy-ldap" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/gcs_partly_cloudy_ldap:bin
elif [ "$APP_NAME" = "dal-etl-populate-application-group-name" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/application_group_name:bin
elif [ "$APP_NAME" = "dal-etl-eces-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/eces:bin
elif [ "$APP_NAME" = "dal-etl-retention-info-backfill" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/retention_info:bin
elif [ "$APP_NAME" = "dal-etl-collibra-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/collibra:bin
elif [ "$APP_NAME" = "dal-etl-application-properties-updater" ]; then
  BUILD_TARGET=src/scala/com/twitter/dal/etl/task/application_property_updater:bin
else
  echo $USAGE
  echo "ERROR: unknown job name '$APP_NAME'"
  exit 1
fi

ZIP_NAME="$APP_NAME.zip"

if [ -z "$DRY_RUN" ]; then
  DRY_RUN="true"
fi

if [ "$DRY_RUN" == "true" ]; then
  APP_NAME=$APP_NAME-dryrun
fi

JOB_KEY=$CLUSTER/$ROLE/$ENV/$APP_NAME
AURORA_CONFIG_FILE=adp/dal_v2/etl/config/deploy.aurora

set -x

./bazel bundle $BUILD_TARGET  --bundle-jvm-archive=zip
package upload -c $CLUSTER -r $ROLE $APP_NAME-$ENV dist/$ZIP_NAME
aurora cron schedule $JOB_KEY $AURORA_CONFIG_FILE
aurora cron start $JOB_KEY
