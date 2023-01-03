#!/bin/bash

# Helper to start a REPL for use with the ScreechOwl service.
#
# This script creates an SSH tunnel to use when communicating with the
# production service automatically. To DISABLE these SSH tunnels from being
# created set SSH_TUNNEL_DISABLED to "true" like so:
#       $> SSH_TUNNEL_DISABLED=true ./path/to/this/script.sh
#

source adp/common/ssh_tunnel_helpers.sh
source adp/common/local_s2s_auth_helpers.sh

create_local_s2s_auth_cert "screechowl-repl"

DIR_NAME=`dirname $0`

start_ssh_tunnel

TARGET="src/scala/com/twitter/screechowl/service:service"

./bazel repl $SSH_TUNNEL_BAZEL_RUN_ARGS $TARGET --  -p $PWD/adp/screechowl/service/repl/init.txt

stop_ssh_tunnel
