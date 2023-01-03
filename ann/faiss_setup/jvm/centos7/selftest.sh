#!/bin/bash

set -e

cd /swigfaiss

cp /swigfaiss/swigfaiss.so /swigfaiss/libswigfaiss.so

g++ -v -std=c++11 \
    -g \
    -O3 \
    -Wextra \
    -mavx2 \
    -mpopcnt \
    -L/swigfaiss \
    -lswigfaiss \
    -L/usr/local/lib64 \
    -lgfortran \
    selftest.cpp /faiss_artifacts/lib/libfaiss_avx2.a /usr/local/lib64/libopenblas.a \
    -o selftest

LD_LIBRARY_PATH=/swigfaiss:/usr/local/lib64 ./selftest
