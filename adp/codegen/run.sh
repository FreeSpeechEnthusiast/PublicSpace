#!/bin/bash
set -ex

# Run all ADP's Slick codegen and scalafmt the generated code
./bazel run --cwd eagleeye/eagleeye-db/src/main/scala/com/twitter/eagleeye/db/codegen:bin
./bazel run --cwd eagleeye/eagleeye-thrift-server/src/main/scala/com/twitter/eagleeye/thrift_server/codegen:bin
./bazel run --cwd src/scala/com/twitter/dal/server/codegen:bin
./bazel run --cwd src/scala/com/twitter/screechowl/db/codegen:bin


arc lint --apply-patches eagleeye/eagleeye-db/src/main/scala/com/twitter/eagleeye/db/codegen/tables/*.scala
arc lint --apply-patches eagleeye/eagleeye-thrift-server/src/main/scala/com/twitter/eagleeye/thrift_server/codegen/tables/*.scala
arc lint --apply-patches src/scala/com/twitter/dal/server/codegen/tables/*.scala
arc lint --apply-patches src/scala/com/twitter/screechowl/db/codegen/tables/*.scala
