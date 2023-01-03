#!/bin/bash

# Helper to start a REPL for use with the EagleEye Thrift Server.
#
# This script creates an SSH tunnel to use when communicating with the
# production service automatically. To DISABLE these SSH tunnels from being
# created set SSH_TUNNEL_DISABLED to "true" like so:
#       $> SSH_TUNNEL_DISABLED=true ./path/to/this/script.sh
#

source adp/common/ssh_tunnel_helpers.sh
source adp/common/local_s2s_auth_helpers.sh

create_local_s2s_auth_cert "eagleeye-thrift-server-repl"

DIR_NAME=$(dirname "$0")

start_ssh_tunnel

TARGET=adp/eagleeye-thrift-server/repl:repl-deps

./bazel repl --jvm_flags=-Xmx2g $SSH_TUNNEL_BAZEL_RUN_ARGS $TARGET -- -p $PWD/$DIR_NAME/init.txt

stop_ssh_tunnel
