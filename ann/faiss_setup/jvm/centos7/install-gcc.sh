#!/bin/bash

set -e

wget -O - https://artifactory.twitter.biz/artifactory/generic-3rdparty-local/gcc/source/gcc-10.1.0.tar.gz | tar xzf - -C /
cd /gcc-10.1.0
./contrib/download_prerequisites

mkdir objdir
cd objdir
../configure --disable-multilib
make -j8
make install
