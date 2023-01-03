#!/bin/bash

set -ex
CLUSTER=atla
ROLE=screechowl-staging
BUNDLE_FILE=screech-owl-send-test-events.tar.gz
TARGET_NAME=screech-owl-send-test-events

./bazel bundle tests/scala/com/twitter/screechowl/listener/send_test_events:bin --bundle-jvm-archive=tgz

packer add_version --cluster=$CLUSTER $ROLE $TARGET_NAME dist/$BUNDLE_FILE

CMD="tar xzf $BUNDLE_FILE && /usr/lib/jvm/java-11-twitter/bin/java -cp libs/*.jar:$TARGET_NAME.jar com.twitter.screechowl.listener.send_test_events.SendTestScreechOwlEvents"

aurora task quickrun --ram 3g --open-browser --name $TARGET_NAME --role $ROLE --package $ROLE/$TARGET_NAME/latest --cluster $CLUSTER --command "$CMD"
