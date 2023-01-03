#!/bin/bash

set -e

wget -O - https://artifactory.twitter.biz/artifactory/generic-3rdparty-local/faiss/source/v1.7.2.tar.gz | tar -xzf -
cd /faiss-1.7.2

export LD_LIBRARY_PATH=/usr/local/lib64
cmake -B _build \
    -DBUILD_TESTING=OFF \
    -DFAISS_OPT_LEVEL=avx2 \
    -DFAISS_ENABLE_GPU=OFF \
    -DFAISS_ENABLE_PYTHON=OFF \
    -DBLAS_LIBRARIES=/usr/local/lib64/libopenblas.a \
    -DLAPACK_LIBRARIES=/usr/local/lib64/libopenblas.a \
    -DCMAKE_INSTALL_LIBDIR=lib \
    -DCMAKE_BUILD_TYPE=RelWithDebInfo .

cmake --build _build --config RelWithDebInfo -j10
cmake --install _build --prefix /faiss_artifacts
export LD_LIBRARY_PATH=
