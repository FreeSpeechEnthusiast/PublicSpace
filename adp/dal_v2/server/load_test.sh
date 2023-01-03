#!/bin/sh

# Tell bash to exit if one of the commands fails
set -e

if [ $# != 1 ]; then
    echo "Usage: $(basename $0) [local|staging|devel]"
    echo " Runs a load test against a local DAL service or one deployed on mesos"
    exit 1
fi

CLUSTER=${CLUSTER:-atla}
TARGET="src/scala/com/twitter/dal/client/test/load:bin"

# Determine the environment
ENV=$1

if [ "$ENV" == "local" ]; then
    echo " ===> Running load test against a locally running DAL thrift server"
    ./bazel run --cwd $TARGET -- -mode=local
    exit $?
elif [ "$ENV" == "staging" ]; then
    user=dal-staging
elif [ "$ENV" == "devel" ]; then
    user=dal-staging
fi

echo " ===> Bundling dal-load-test"
./bazel bundle $TARGET --bundle-jvm-archive=zip

set -x

packer add_version --cluster="$CLUSTER" "$user" dal-load-test-$ENV dist/dal-load-test.zip

AURORA_CONFIG_FILE=adp/dal_v2/server/load_test.aurora
aurora job create "$CLUSTER/$user/$ENV/dal-load-test" $AURORA_CONFIG_FILE --bind WF_USER=$USER
