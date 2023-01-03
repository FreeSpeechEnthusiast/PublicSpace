#!/bin/bash

set -e

cd /
wget -O - https://artifactory.twitter.biz/artifactory/generic-3rdparty-local/swig/source/rel-4.0.2.tar.gz | tar -xzf -
cd swig-rel-4.0.2
./autogen.sh
./configure
make -j
make install
