#!/bin/bash

# A wrapper script to call the Role Migration Assistant
#
# This script creates an SSH tunnel to use when communicating with the
# production service automatically. To DISABLE these SSH tunnels from being
# created set SSH_TUNNEL_DISABLED to "true" like so:
#       $> SSH_TUNNEL_DISABLED=true ./path/to/this/script.sh
#

set -e

source adp/common/ssh_tunnel_helpers.sh

start_ssh_tunnel

TARGET="adp/batch/role_migration:role_migration-bin"

# Shove the command line arguments into a bash array to work around quoting issues
arr=("$@")

echo
echo

set -x

./bazel run $TARGET -- $SSH_TUNNEL_BAZEL_RUN_ARGS "${arr[@]}"

stop_ssh_tunnel
