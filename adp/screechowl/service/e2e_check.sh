#!/bin/bash
#
# Runs the ScreechOwlE2ECheck class, which does some simple verifications that the ScreechOwl
# service is running and behaving correctly.
#
source adp/common/ssh_tunnel_helpers.sh

start_ssh_tunnel
ssh_tunnel_running && echo "IS RUNNING"

TARGET="src/scala/com/twitter/screechowl/service/e2e_test:bin"

#
#
# To run this, you'll need to create an S2S auth cert and move it to where its expected:
#
#    developer-cert-util --job e2e-job-screechowl-confidence-check --local --client-only -f
#    cp -r /Users/$USER/.s2s/local/devel/e2e-job-screechowl-confidence-check /var/lib/tss/keys/s2s/local_cluster/devel/
#
# Then add the args below to the run command:
#    -serviceIdentifier.environment=devel -serviceIdentifier.role=$USER -serviceIdentifier.cluster=local_cluster

./bazel run $TARGET -- $SSH_TUNNEL_BAZEL_RUN_ARGS -serviceLocation=/s/screechowl/screech-owl-service

stop_ssh_tunnel
