#!/bin/sh
set -o errexit

# Script that builds a screech-owl-alerter bundle, adds a new package to packer and redeploys on aurora
# To kill a cron, run:
#   aurora cron deschedule $CLUSTER/$ROLE/$ENV/screech-owl-alerter
#   aurora killall $CLUSTER/$ROLE/$ENV/screech-owl-alerter

ENV=$1
CLUSTER=atla
NAME=screech-owl-alerter

if [ "$ENV" == "devel" ]; then
  ROLE=$USER
elif [ "$ENV" == "staging" ]; then
  ROLE=screechowl-staging
elif [ "$ENV" == "prod" ]; then
  ROLE=screechowl
  DASHBOARD=$NAME-$ROLE-$ENV
else
  echo "Usage: $0 <devel|staging|prod>"
  exit 1
fi

./bazel bundle --bundle-jvm-archive=tgz adp:screech-owl-alerter

packer add_version --cluster=$CLUSTER $ROLE $NAME dist/$NAME.tar.gz

aurora cron schedule $CLUSTER/$ROLE/$ENV/$NAME adp/screechowl/alerter/config/deploy.aurora

aurora cron start --open-browser $CLUSTER/$ROLE/$ENV/$NAME
