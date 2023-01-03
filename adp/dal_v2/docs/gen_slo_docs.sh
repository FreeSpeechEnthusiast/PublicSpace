#!/bin/bash

DEST_DIR=dal/docs/dal_slo_includes/
TEMPLATE_FILE=adp/dal_v2/docs/slo_template.txt

gen_method_file() {
    METHOD_NAME=$1
    LOWER_NAME=$(echo $METHOD_NAME | tr '[:upper:]' '[:lower:]')

    cat $TEMPLATE_FILE | perl -p -e "s/__METHOD_NAME__/$METHOD_NAME/g" > "$DEST_DIR/dal_slo_${LOWER_NAME}.rst"
}

gen_method_file createSegment
gen_method_file createSegmentByUrl

gen_method_file deleteSegmentsByUrl
gen_method_file deleteSegmentsByRootPath

gen_method_file prepareOutputSegment
gen_method_file prepareReplicaSegment

gen_method_file replicateSegmentByUrl

gen_method_file updateSegmentStateByUrl
gen_method_file updateSegmentStates
gen_method_file updateSegmentStatesByRunId


gen_method_file findInputSegments
gen_method_file findInputSegmentsByUrl
gen_method_file findSegmentDifferences
gen_method_file findServiceableSegments
gen_method_file getSegmentEvents
