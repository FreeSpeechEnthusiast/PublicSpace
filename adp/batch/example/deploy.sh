#!/bin/bash

ENV=${1:-devel}
CLUSTER=atla

if [ "$ENV" == "local" ]; then
  echo "Don't use this example in local environment"
  exit 1
elif [ "$ENV" == "devel" ]; then
  ROLE=$USER
elif [ "$ENV" == "staging" ] || [ "$ENV" == "prod" ]; then
  echo "Don't use this example in staging or production enviroments"
  exit 1
else
  echo "Usage: $0 <local|devel|staging|prod>"
  exit 1
fi

# if we change prod to deploy 'live' version and not 'latest' we need to handle that below
TARGET=src/scala/com/twitter/twadoop/batch/example:batch-scala-example-app

set -x

./bazel bundle $TARGET --bundle-jvm-archive=zip && \
packer add_version --cluster=$CLUSTER $ROLE batch-scala-example-app dist/batch-scala-example-app.zip && \
aurora deploy create $CLUSTER/$ROLE/$ENV/batch-scala-example adp/batch/example/deploy.aurora && \
aurora deploy release $CLUSTER/$ROLE/$ENV/batch-scala-example
