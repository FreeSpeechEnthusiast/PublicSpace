#!/bin/bash

# This script should be used along with adp/dal_v2/server/run_staging_backed_local.sh
# to have both DAL and EagleEyeHttpServer running locally with database tunnels to
# Staging DALv2 database.

./bazel run eagleeye/eagleeye-http-server/src/test/scala/com/twitter/eagleeye/http_server/hardcoded_http_server:local_bin -- \
-admin.port=':9993' \
-serviceIdentifier.service='eagleeye-http-server' \
-serviceIdentifier.role=$USER \
-serviceIdentifier.cluster='local_cluster' \
-serviceIdentifier.environment='devel'
