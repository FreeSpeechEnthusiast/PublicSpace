#!/bin/bash

set -e

cd /swigfaiss

mkdir -p src/main/java/com/twitter/ann/faiss
LD_LIBRARY_PATH=/usr/local/lib64 swig -v -c++ -java -package com.twitter.ann.faiss -o swigfaiss.cpp \
    -outdir src/main/java/com/twitter/ann/faiss/ \
    -Doverride= -I/faiss_artifacts/include/faiss \
    -Doverride= -I/faiss_artifacts/include \
    swigfaiss.swig

g++ -v -std=c++11 \
    -fopenmp \
    -fPIC \
    -m64 \
    -Wno-sign-compare \
    -g \
    -O3 \
    -Wextra \
    -mavx2 \
    -mpopcnt \
    -I/faiss_artifacts/include \
    -I/faiss_artifacts/include/faiss \
    -I/usr/lib/jvm/java-11-twitter/include \
    -I/usr/lib/jvm/java-11-twitter/include/linux \
    -L/usr/local/lib64 \
    -lgfortran \
    swigfaiss.cpp /faiss_artifacts/lib/libfaiss_avx2.a /usr/local/lib64/libopenblas.a \
    -shared -o swigfaiss.so
