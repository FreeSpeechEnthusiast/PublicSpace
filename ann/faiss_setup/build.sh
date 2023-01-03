#!/bin/bash

set -e

REPO_ROOT=$(git rev-parse --show-toplevel)
# shellcheck source=/dev/null
source "$REPO_ROOT/ann/faiss_setup/common.sh"

rm -rf "$TMP_ROOT/result/"

"$REPO_ROOT/ann/faiss_setup/jvm/macos/build.sh"
"$REPO_ROOT/ann/faiss_setup/jvm/centos7/build.sh"

cp -R "$TMP_ROOT/result/" "$REPO_ROOT/ann/src/main/java/com/twitter/ann/faiss/swig/resources"

cd "$REPO_ROOT"

# Test that we're able to build and run when all dependencies are present in filesystem
# We can't run directly, because https://jira.twitter.biz/browse/DPB-16443
bazel build ann/src/main/java/com/twitter/ann/faiss/selftest:swig-selftest-binary-local &&
    bazel-bin/ann/src/main/java/com/twitter/ann/faiss/selftest/swig-selftest-binary-local.sh

bazel build ann/src/main/java/com/twitter/ann/faiss/swig/resources

echo "Jar for artifactory upload is at $REPO_ROOT/bazel-out/darwin-fastbuild/bin/ann/src/main/java/com/twitter/ann/faiss/swig/resources/resources-scala.jar"
echo "Follow https://confluence.twitter.biz/display/ENG/Artifactory#Artifactory-IfAnArtifactisnotinMCR to learn how to upload"
echo "Once uploaded, update corresponding version in 3rdparty/jvm/com/twitter/ann/faiss/swig"
echo "Here is current 3rdparty declaration"
cat 3rdparty/jvm/com/twitter/ann/faiss/swig/BUILD
echo "Press any key to run self test with resources from artifactory"
read -r

bazel build ann/src/main/java/com/twitter/ann/faiss/selftest:swig-selftest-binary-artifactory &&
    bazel-bin/ann/src/main/java/com/twitter/ann/faiss/selftest/swig-selftest-binary-artifactory.sh
