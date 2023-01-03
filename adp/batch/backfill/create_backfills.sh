#!/bin/bash

# A wrapper script to call the Statebird Backfill-Creation tool.
#
# This script creates an SSH tunnel to use when communicating with the
# production service automatically. To DISABLE these SSH tunnels from being
# created set SSH_TUNNEL_DISABLED to "true" like so:
#       $> SSH_TUNNEL_DISABLED=true ./path/to/this/script.sh
#


die() { echo "fatal: $*" >&2; exit 1; }

source adp/common/ssh_tunnel_helpers.sh
source adp/common/local_s2s_auth_helpers.sh

start_ssh_tunnel

# unconditionally run cleanup on script exit
trap stop_ssh_tunnel EXIT

# create developer cert for s2s auth
create_local_s2s_auth_cert "statebird-backfill-tool"

TARGET="src/scala/com/twitter/twadoop/batch/backfill_cli:backfill-cli-bin"

# A place to write log files to, the below is cross-platform hackery to create a TMP dir
OUTPUT_TMP_DIR=$(mktemp -d 2>/dev/null || mktemp -d -t 'backfill_tmpdir') || die "could not create tmpdir"

# A file to write stdout/stderr of the command run to.
STDOUT_LOG="$OUTPUT_TMP_DIR/create_backfills_output.log"
# A file with information about the items created in it.
CREATED_BACKFILLS_LOG="$OUTPUT_TMP_DIR/created_backfill_information.log"

# Shove the command line arguments into a bash array to work around quoting issues
arr=("$@")

create_backfills() {
  # shellcheck disable=SC2090,SC2086
  ./bazel run "$TARGET" -- $SSH_TUNNEL_BAZEL_RUN_ARGS -output.log $CREATED_BACKFILLS_LOG "${arr[@]}" >> "$STDOUT_LOG" 2>&1
}


if ! create_backfills; then
  cat >&2 <<EOS

ERROR: last 10 lines of command output follow:

----- START LOG -----
$(tail -n10 "$STDOUT_LOG")
----- END LOG -----

ERROR: non-zero exit. Please check $STDOUT_LOG for details
EOS

  exit 1
fi

# display the created items from the command
cat "$CREATED_BACKFILLS_LOG"

# display a message about where to find logs
echo
echo "Does the above list of created items look right?  If not :"
echo "- information about the batch runs created is in: $CREATED_BACKFILLS_LOG"
echo "- stdout/stderr from bazel and this command is in: $STDOUT_LOG"
