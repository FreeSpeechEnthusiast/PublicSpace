#!/bin/bash

DALV2_COMMAND=$(mktemp)

echo "DALV2_COMMAND file is $DALV2_COMMAND"

./bazel run adp/dal_v2/server:dev_bin \
--script_path="$DALV2_COMMAND" \
-- \
-service.configClass=com.twitter.dal.server.config.DevLocalStagingConfig \
-service.port=":9994" \
-admin.port=":9992" \
-serviceIdentifier.cluster="local_cluster" \
-serviceIdentifier.role="$USER" \
-serviceIdentifier.service="dal" \
-serviceIdentifier.environment="devel"

echo "Running DAL Server..."

$DALV2_COMMAND
