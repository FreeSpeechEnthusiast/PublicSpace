#!/bin/bash

set -ex

REPO_ROOT=$(git rev-parse --show-toplevel)
# shellcheck source=/dev/null
source "$REPO_ROOT/ann/faiss_setup/common.sh"

function build_image_and_pull_results() {
    cd "$REPO_ROOT/ann/faiss_setup/jvm/centos7"
    docker build .
    IMAGE_ID=$(docker build -q .)
    CONTAINER_ID=$(docker create "$IMAGE_ID")
    mkdir -p "$TMP_ROOT/result"
    docker cp -L "$CONTAINER_ID":/usr/local/lib64/libstdc++.so.6 "$TMP_ROOT/result"
    docker cp -L "$CONTAINER_ID":/usr/local/lib64/libgcc_s.so.1 "$TMP_ROOT/result"
    docker cp -L "$CONTAINER_ID":/usr/local/lib64/libgomp.so.1 "$TMP_ROOT/result"
    docker cp -L "$CONTAINER_ID":/usr/local/lib64/libquadmath.so.0 "$TMP_ROOT/result"
    docker cp -L "$CONTAINER_ID":/usr/local/lib64/libgfortran.so.5 "$TMP_ROOT/result"
    docker cp -L "$CONTAINER_ID":/swigfaiss/swigfaiss.so "$TMP_ROOT/result"
    docker rm -v "$CONTAINER_ID"
}

build_image_and_pull_results
