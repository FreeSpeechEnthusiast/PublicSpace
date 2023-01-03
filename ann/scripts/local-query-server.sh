#!/usr/bin/env bash
set -eux

JOB_NAME="hnsw-query-server"
REPO_ROOT="$(git rev-parse --show-toplevel)" || die 'failed to find git toplevel'

# Dev certs https://docbird.twitter.biz/service_authentication/howto/credentials.html#developer-certs
developer-cert-util --local --job ${JOB_NAME}

bazel run \
  //ann/src/main/scala/com/twitter/ann/service/query_server/hnsw:hnsw-query-server -- \
    --jvm_flags=" \
      -Dlog.access.output=/dev/stdout \
      -Dlog.service.output=/dev/stdout \
      -Dlog.level=WARN" \
    -thrift.port=:8886 \
    -admin.port=:9996 \
    -decider.base "${REPO_ROOT}/ann/src/main/resources/config/hnsw_query_server_decider.yml" \
    -opportunistic.tls.level desired \
    -service.identifier "${USER}:${JOB_NAME}:devel:local" \
    -environment devel \
    -index_directory "${REPO_ROOT}/ann/src/test/resources/service/query_server/hnsw/hnsw_l2" \
    -metric L2 \
    -dimension 3 \
    -id_type long \
    -com.twitter.server.wilyns.disable true \
    -refreshable false
