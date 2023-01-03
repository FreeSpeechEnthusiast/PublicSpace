#!/bin/sh
set -e

# See: http://pantsbuild.github.io/invoking.html#order-of-arguments
./pants -q run auth/api-verification:bin
