#!/bin/bash

source adp/common/ssh_tunnel_helpers.sh

start_ssh_tunnel

arr=("$@")

./bazel run \
   src/scala/com/twitter/dal/client/cli:bin -- $SSH_TUNNEL_BAZEL_RUN_ARGS "${arr[@]}"

stop_ssh_tunnel
