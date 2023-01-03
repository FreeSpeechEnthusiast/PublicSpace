#!/bin/bash

export NEST_HOST=hadoopnest2.smf1.twitter.com
source adp/common/ssh_tunnel_helpers.sh

start_ssh_tunnel

PROXEE_COMMAND=$(mktemp)

echo "PROXEE_COMMAND file is $PROXEE_COMMAND"

./bazel run --cwd \
        csl/proxee/app:app -- \
	$SSH_TUNNEL_BAZEL_RUN_ARGS \
	-proxee.config adp/common/proxee-testing-examples/proxee_config.yaml

stop_ssh_tunnel
