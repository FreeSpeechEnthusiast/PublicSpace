#!/bin/bash
#
# Wrapper script around StatebirdCLI.
#
# Arguments passed to this script get passed to the main() method of the client.
#
# Examples:
#
# adp/statebird/v2/statebird_client.sh -p -v get_run 71891694
#
# adp/statebird/v2/statebird_client.sh -p -v get_history \
#   "client-analytics/prod/oink/oink user_audits_analysis:daily"
#
# adp/statebird/v2/statebird_client.sh -p mark_invalid \
#   --note "This were invalid attempts."  230279229 229595013

source adp/common/ssh_tunnel_helpers.sh
source adp/common/local_s2s_auth_helpers.sh

start_ssh_tunnel

create_local_s2s_auth_cert "statebird-client"

arr=("$@")

./bazel run \
        src/scala/com/twitter/statebird/client/v2:client -- \
        $SSH_TUNNEL_BAZEL_RUN_ARGS "${arr[@]}"

stop_ssh_tunnel
