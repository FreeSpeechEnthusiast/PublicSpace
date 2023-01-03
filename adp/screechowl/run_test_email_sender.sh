#!/bin/bash

TARGET="src/scala/com/twitter/screechowl/util/email:email_sender_bin"
RUN_ARGS="--jvmopt=-Xmx4g"

./bazel run $RUN_ARGS $TARGET
