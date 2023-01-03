#!/bin/bash

set -e

REPO_ROOT=$(git rev-parse --show-toplevel)
# shellcheck source=/dev/null
source "$REPO_ROOT/ann/faiss_setup/common.sh"

brew update

if [ "$(arch)" == "arm64" ]; then
    # Build both slices, because rosetta2 on arm doesn't support needed x86_64 extensions
    TARGET_ARCH="x86_64;arm64"
else
    # We can't build arm64 on x86_64 mac
    TARGET_ARCH="x86_64"
fi

echo "Target architectures are $TARGET_ARCH"

function build_faiss() {
    brew outdated cmake || brew install cmake

    mkdir -p "$TMP_ROOT"
    cd "$TMP_ROOT"

    echo "Building libomp"
    if [ ! -d "$(pwd)/llvm-project-llvmorg-12.0.1/openmp" ]; then
        wget -O - https://artifactory.twitter.biz/artifactory/generic-3rdparty-local/llvm-project/source/llvm-project-llvmorg-12.0.1.tar.gz |
            tar -xzf - -C "$TMP_ROOT"
    fi

    pushd "llvm-project-llvmorg-12.0.1/openmp" &&
        cmake . \
            -B build \
            -DCMAKE_BUILD_TYPE=Release \
            -DCMAKE_OSX_ARCHITECTURES="${TARGET_ARCH}" \
            -DLIBOMP_ENABLE_SHARED=OFF &&
        cmake --build build -j &&
        echo "Install libomp on the system as root" && sudo cmake --install build &&
        popd

    if [ ! -d "$(pwd)/faiss-1.7.2" ]; then
        wget -O - https://artifactory.twitter.biz/artifactory/generic-3rdparty-local/faiss/source/v1.7.2.tar.gz |
            tar -xzf - -C "$TMP_ROOT"
    fi

    cd "faiss-1.7.2"
    cmake \
        -DCMAKE_OSX_ARCHITECTURES="${TARGET_ARCH}" \
        -DFAISS_OPT_LEVEL=avx2 \
        -DCMAKE_BUILD_TYPE=Release \
        -DFAISS_ENABLE_PYTHON=OFF \
        -DFAISS_ENABLE_GPU=OFF . &&
        make -j faiss faiss_avx2
}

function instantiate_swig_template() {
    brew outdated swig || brew install swig

    mkdir -p "$REPO_ROOT/ann/src/main/java/com/twitter/ann/faiss/swig"
    swig -v -c++ -java \
        -Doverride= "-I$TMP_ROOT/faiss-1.7.2/faiss" \
        -Doverride= "-I$TMP_ROOT/faiss-1.7.2" \
        -package com.twitter.ann.faiss \
        -o "$TMP_ROOT/swigfaiss.cpp" \
        -outdir "$REPO_ROOT/ann/src/main/java/com/twitter/ann/faiss/swig" \
        "$REPO_ROOT/ann/faiss_setup/jvm/centos7/swigfaiss.swig"
}

function build_swig_bridge() {
    mkdir -p "$TMP_ROOT/swig-build"

    function invoke_compiler() {
        ARCH=$1

        clang++ -std=c++11 \
            -fPIC \
            -Xclang -fopenmp \
            -Wno-sign-compare \
            -g \
            -O3 \
            -Wextra \
            -mavx2 \
            -mpopcnt \
            -L "$TMP_ROOT/faiss-1.7.2/faiss" \
            -I "$TMP_ROOT/faiss-1.7.2" \
            -I "$TMP_ROOT/faiss-1.7.2/faiss" \
            -I /Library/Java/JavaVirtualMachines/TwitterJDK11/Contents/Home/include \
            -I /Library/Java/JavaVirtualMachines/TwitterJDK11/Contents/Home/include/darwin/ \
            -lfaiss_avx2 \
            -lomp \
            -L /usr/local/lib \
            swigfaiss.cpp \
            -target "$ARCH" \
            -framework Accelerate \
            -fvisibility-inlines-hidden-static-local-var \
            -shared -o "swig-build/swigfaiss.$ARCH.dylib"
    }

    cd "$TMP_ROOT"
    invoke_compiler "x86_64-apple-macos12"

    if [ "$(arch)" == "arm64" ]; then
        invoke_compiler "arm64-apple-macos12"

        lipo -create -output swigfaiss.dylib \
            swig-build/swigfaiss.x86_64-apple-macos12.dylib \
            swig-build/swigfaiss.arm64-apple-macos12.dylib
    else
        cp -R swig-build/swigfaiss.x86_64-apple-macos12.dylib swigfaiss.dylib
    fi
}

function collect_artifacts() {
    mkdir -p "$TMP_ROOT/result"
    cp -R "$TMP_ROOT/swigfaiss.dylib" "$TMP_ROOT/result/"
}

build_faiss
instantiate_swig_template
build_swig_bridge
collect_artifacts
