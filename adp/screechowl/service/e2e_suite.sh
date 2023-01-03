#!/bin/bash
#
# Runs the ScreechOwlSuite class, part of the ADP E2E tests implementation.
# The ScreechOwlSuite class builds and deploys to aurora the kicks off the class.
#
source adp/common/ssh_tunnel_helpers.sh

start_ssh_tunnel
ssh_tunnel_running && echo "IS RUNNING"

TARGET="src/scala/com/twitter/data_platform/e2e_testing/suites:screechowl-suite-bin"

set -x

./bazel run $TARGET -- $SSH_TUNNEL_BAZEL_RUN_ARGS -screechOwlServerSet=screechowl/prod/screech-owl-service

stop_ssh_tunnel
