#!/bin/bash
set -e

export NEST=hadoopnest3.atla.twitter.com

IAGO_COMMAND=$(mktemp)

TARGET=src/scala/com/twitter/dal/iago:loadtest-bin

./bazel run --script_path="$IAGO_COMMAND" "$TARGET"

tweetypie/scripts/tunneled $IAGO_COMMAND \
        --jvm_flags=-Xmx8g \
        -config="$PWD/adp/dal_v2/server/iago_load_test/dal-loadtest-staging.yaml" \
        -service.identifier="$USER:iago:devel:local"
