#!/usr/bin/env bash

SOURCE_ROOT="$(git rev-parse --show-toplevel)"
cd "${SOURCE_ROOT}" || exit

iago-internal/scripts/start.sh auth/policykeeper/server/src/test/scala/com/twitter/auth/policykeeper/loadtest:loadtest-bin \
  -config="auth/policykeeper/server/src/test/resources/loadtest.yaml" \
  -service.identifier="$USER:iago:devel:local"
