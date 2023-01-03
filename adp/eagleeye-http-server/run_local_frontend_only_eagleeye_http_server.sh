#!/bin/bash

# This script deploys EagleEyeHttpServer front end without deploying or listening for DAL in the localhost.
# It connects to DAL staging identified by wily name /cluster/local/dal-staging/dal
source adp/common/ssh_tunnel_helpers.sh

start_ssh_tunnel
# shellcheck disable=SC2086
./bazel run \
eagleeye/eagleeye-http-server/src/test/scala/com/twitter/eagleeye/http_server/hardcoded_http_server:local_front_end_bin -- \
 $SSH_TUNNEL_BAZEL_RUN_ARGS \
-admin.port=':9997' \
-serviceIdentifier.service='eagleeye-http-server' \
-serviceIdentifier.role=$USER \
-serviceIdentifier.cluster='local_cluster' \
-serviceIdentifier.environment='devel'

stop_ssh_tunnel
