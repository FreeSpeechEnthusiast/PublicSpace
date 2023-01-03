#!/bin/sh
set -o errexit
set -x

# Script that builds a screech-owl-task-silence-alerts bundle, adds a new package to packer
# and redeploys on aurora
# To kill a cron, run:
#   aurora cron deschedule $CLUSTER/$ROLE/$ENV/screech-owl-task-silence-alerts
#   aurora job killall $CLUSTER/$ROLE/$ENV/screech-owl-task-silence-alerts

ENV=$1
CLUSTER=atla
NAME=screech-owl-task-silence-alerts
DRY_RUN=$2

BUILD_TARGET=src/scala/com/twitter/screechowl/task/silence_alerts:bin

if [ "$ENV" == "staging" ]; then
  ROLE=screechowl-staging
elif [ "$ENV" == "prod" ]; then
  ROLE=screechowl
else
  echo "Usage: $0 <staging|prod> <dryrun (true|false)>"
  exit 1
fi

TAR_NAME="$NAME.tar.gz"

if [ -z "$DRY_RUN" ]; then
  DRY_RUN="true"
fi

if [ "$DRY_RUN" == "true" ]; then
  NAME=$NAME-dryrun
fi

./bazel  bundle $BUILD_TARGET --bundle-jvm-archive=tgz

packer add_version --cluster=$CLUSTER $ROLE $NAME dist/$TAR_NAME

aurora cron schedule "$CLUSTER/$ROLE/$ENV/$NAME" adp/screechowl/task/silence_alerts/config/deploy.aurora

aurora cron start --open-browser "$CLUSTER/$ROLE/$ENV/$NAME"
