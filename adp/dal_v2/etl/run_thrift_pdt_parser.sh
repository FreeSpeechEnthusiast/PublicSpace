#!/bin/bash

set -ex


# Run a command that generates some gizmoduck thrift, which is referred to
# by other (non-generated) thrift sources.
./bazel run --cwd scrooge-internal/generator:bin \
    -- \
    -i src/thrift/ \
    --language gizmoduck_thrift --finagle --gen-adapt \
    --dest src/thrift/ \
    src/thrift/com/twitter/gizmoduck/user.thrift

echo

THRIFT_PARSER_RUN_COMMAND=$(mktemp)

echo "THRIFT_PARSER_RUN_COMMAND file is $THRIFT_PARSER_RUN_COMMAND"

DIR=${1:-$PWD}

./bazel run --script_path="$THRIFT_PARSER_RUN_COMMAND" src/scala/com/twitter/dal/client/thrift_pdt_parser:bin

LOG_FILE="dal_thrift_pdt_parser.log"

time \
    $THRIFT_PARSER_RUN_COMMAND \
    --jvm_flags=-Xmx22g \
    -directory="$DIR" \
    -source-root="$PWD" \
    2>&1 | tee $LOG_FILE

echo "Logs in $LOG_FILE"
