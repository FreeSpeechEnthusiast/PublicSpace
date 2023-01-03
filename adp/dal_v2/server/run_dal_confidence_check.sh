#!/bin/bash

source adp/common/ssh_tunnel_helpers.sh

start_ssh_tunnel

TARGET="src/scala/com/twitter/dal/client/test/confidence_check:confidence_check_bin"

# SERVICE_LOCATION=/cluster/local/dal/prod/dal_read_only

SERVICE_LOCATION=/cluster/atla/dal-staging/staging/dal

set -x

bazel run $TARGET -- $SSH_TUNNEL_BAZEL_RUN_ARGS -serviceLocation $SERVICE_LOCATION -com.twitter.dal.client.builder.DALServiceIdentifier=twtr:svc:jboyd:dal:devel:local

stop_ssh_tunnel
