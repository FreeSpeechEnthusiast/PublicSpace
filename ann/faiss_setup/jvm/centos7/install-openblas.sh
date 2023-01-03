#!/bin/bash

set -e

cd /
wget -O - https://artifactory.twitter.biz/artifactory/generic-3rdparty-local/openblas/source/v0.3.21.tar.gz | tar -xzf -
cd OpenBLAS-0.3.21

export LD_LIBRARY_PATH=/usr/local/lib64

cmake -B _build \
    -DTARGET=HASWELL \
    -DUSE_OPENMP=1 -DNUM_THREADS=128 \
    -DCMAKE_BUILD_TYPE=Release .

make -C _build -j 10
cmake --install _build

export LD_LIBRARY_PATH=
