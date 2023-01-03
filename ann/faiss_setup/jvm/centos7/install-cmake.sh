#!/bin/bash

set -e

wget -O - https://artifactory.twitter.biz/artifactory/generic-3rdparty-local/cmake/cmake-3.24.0-linux-x86_64.tar.gz | tar xzf -
cp -R cmake-3.24.0-linux-x86_64/* /usr/local
