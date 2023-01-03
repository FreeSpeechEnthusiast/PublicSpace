FROM --platform=linux/amd64 docker-local.artifactory.twitter.biz/kaos/twitter-java:latest

RUN yum update
RUN yum install -y wget openblas-devel lapack-devel gcc make gcc-c++ automake pcre-devel byacc

# GCC
COPY install-gcc.sh /
RUN /install-gcc.sh

# Cmake
COPY install-cmake.sh /
RUN /install-cmake.sh

ENV CC=/usr/local/bin/gcc
ENV CXX=/usr/local/bin/g++

# swig
COPY install-swig.sh /
RUN /install-swig.sh

# OpenBLAS
COPY install-openblas.sh /
RUN /install-openblas.sh

# faiss
COPY build-faiss.sh /
RUN /build-faiss.sh

# Swig bridge
COPY swigfaiss.swig /swigfaiss/
COPY build-swigfaiss.sh /
RUN /build-swigfaiss.sh

# Disabled, because docker has buggy avx2 https://github.com/docker/for-mac/issues/4576
# # Dynamic libraries loading selftest
# COPY selftest.cpp /swigfaiss/
# COPY selftest.sh /
# RUN /selftest.sh