#!/usr/bin/env bash
#
# This script is intended to be run on CI at the following address
#
#   http://go/ci/twadoop_config_dal_sync
#
set -exo pipefail

ENV=$1
ARGS=""

if [ "$ENV" == "test" ]; then
  # SSH tunnel in order to communicate to DAL
  source adp/common/ssh_tunnel_helpers.sh
  start_ssh_tunnel

  ARGS=$SSH_TUNNEL_PANTS_ARGS
  DAL_PATH="/srv#/staging/local/dal-staging/dal"
  AURORA_KEY="smf1/$USER/staging/dataset-config-dal-updater/0"
  CLIENT_ID="twadoop-config-dal-sync"
  SERVICE_IDENTIFIER="twtr:svc:${USER}:twadoop-dal-provisioner-job:devel:local"
elif [ "$ENV" == "ci" ]; then
  echo "Running on ${NODE_NAME}"
  DC=${NODE_NAME:0:4}
  echo "Using credentials for ${DC} data center"

  DAL_PATH="/srv#/prod/local/dal/dal"
  AURORA_KEY="smf1/datapipeline/prod/dataset-config-dal-updater/0"
  CLIENT_ID="twadoop-config-dal-sync-ci"
  SERVICE_IDENTIFIER="twtr:svc:jenkins:twadoop-dal-provisioner-job:prod:${DC}"
else
  echo "Usage: $0 <test|ci>"
  echo "  test: sets up a ssh tunnel and connects to dal staging"
  echo "  ci: configured to be run remotely as part of a ci process"
  exit 1
fi

BASE=adp/dal_v2/client/provisioner

# Dump all the configurations onto the filsystem for later use by the provisioner
./pants binary twadoop_config/scripts:build
./dist/build.pex -d

CREATE_DATASETS_FILE=${BASE}/targets.create_datasets.$$.txt
CREATE_DATARECORD_DATASETS_FILE=${BASE}/targets.create_datarecord_datasets.$$.txt
DEBUG_DATASETS_FILE=${BASE}/targets.debug_datasets.$$.txt

echo "collection 'create_datasets' target list"
./bazel query 'kind("scala_library", attr("generator_function", "create_datasets", //... - //sandbox/...))' > $CREATE_DATASETS_FILE

echo "collection 'create_datarecord_datasets' target list"
./bazel query 'kind("scala_library", attr("generator_function", "create_datarecord_datasets", //... - //sandbox/...))' > $CREATE_DATARECORD_DATASETS_FILE

echo "collection 'debug_datasets' target list"
./bazel query 'kind("scala_library", attr("generator_function", "debug_datasets", //... - //sandbox/...))' > $DEBUG_DATASETS_FILE

# Create a dynamic BUILD target
TARGETS_INPUT_FILE=${BASE}/targets_input.$$.txt

TARGETS_FILE=${BASE}/targets.$$.txt

# combine the output of the three bazel-query commands above into one file
#
# NOTE: we're manually ignoring targets under 'tools/build_rules/targets'
#
cat $CREATE_DATASETS_FILE $CREATE_DATARECORD_DATASETS_FILE $DEBUG_DATASETS_FILE  | sort | uniq | grep -v 'tools/build_rules/targets' > $TARGETS_INPUT_FILE

# format the above targets for use in a BUILD by wrapping in quotes and separating with commas
sed -e 's/^/"/' -e 's/$/",/' $TARGETS_INPUT_FILE > $TARGETS_FILE

# generate BUILD file from template, replacing DEPS_TARGETS with the list of actual deps
cp ${BASE}/build.template ${BASE}/BUILD
printf '%s\n' "/DEPS_TARGETS/r $TARGETS_FILE" 1 '/DEPS_TARGETS/d' w | ed ${BASE}/BUILD
rm "$TARGETS_FILE"

echo "starting the main provisioning"

# CMSClassUnloadingEnabled is used to GC classes from the classloader, which would otherwise accumulate in
# the perm gen space when we load schemas. This option requires the ConcurrentMarkSweep GC collector.
./bazel run --cwd --nocheck_visibility \
  adp/dal_v2/client/provisioner:provisioner-bin \
  -- \
  --jvm_flags="-Xmx6144m -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Daurora.instanceKey=${AURORA_KEY}" \
    -dalPath=${DAL_PATH} \
    -dalClientId=${CLIENT_ID} \
    -serviceIdentifier=${SERVICE_IDENTIFIER} \
    -config=twadoop_config/target/gen-config/dal_datasets.yml

echo "Create Sparrow BQ Datasets and match them to Log Categories"

# CMSClassUnloadingEnabled is used to GC classes from the classloader, which would otherwise accumulate in
# the perm gen space when we load schemas. This option requires the ConcurrentMarkSweep GC collector.
./bazel run --cwd --nocheck_visibility \
  adp/dal_v2/client/provisioner:sparrow-provisioner-matcher-bin \
  -- \
  --jvm_flags="-Xmx6144m -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Daurora.instanceKey=${AURORA_KEY}" \
    -dalPath=${DAL_PATH} \
    -dalClientId=${CLIENT_ID} \
    -serviceIdentifier=${SERVICE_IDENTIFIER} \
    -config=twadoop_config/target/gen-config/dal_datasets.yml

echo "Completed Sparrow Provisioning and Updating"

if [ "$ENV" == "test" ]; then
  stop_ssh_tunnel
fi
