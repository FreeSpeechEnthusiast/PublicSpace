#!/bin/bash

set -o errexit

#
# This script regenerates the DAL-CLI "help" documentation, which is
# included at the end of the general DAL-CLI documentation.
#
# Run this script with no arguments:
#
#    ./adp/dal_v2/docs/gen_cli_help_docs.sh
#

append_header() {
    cat <<EOF > "$1"

.. This is a generated file, DO NOT EDIT.  Update it with source/adp/dal_v2/docs/gen_cli_help_docs.sh


.. code-block:: none

EOF
}


JAVA_DAL_CLI_COMMAND=$(mktemp)

echo "JAVA_DAL_CLI_COMMAND file is $JAVA_DAL_CLI_COMMAND"

./bazel run --script_path="$JAVA_DAL_CLI_COMMAND" src/scala/com/twitter/dal/client/cli:bundle

BRIEF_HELP_OUTPUT_FILE=dal/docs/dal_help/dal_cli_short_help.rst
LONG_HELP_OUTPUT_FILE=dal/docs/dal_help/dal_cli_help.rst
LOGICAL_DATASET_HELP_OUTPUT_FILE=dal/docs/dal_help/dal_cli_logical_dataset_help.rst
LOGICAL_DATASET_LIST_HELP_OUTPUT_FILE=dal/docs/dal_help/dal_cli_logical_dataset_list_help.rst

SEGMENT_HELP_OUTPUT_FILE=dal/docs/dal_help/dal_cli_segment_help.rst
SEGMENT_LIST_HELP_OUTPUT_FILE=dal/docs/dal_help/dal_cli_segment_list_help.rst

echo "long help"
append_header $LONG_HELP_OUTPUT_FILE
$JAVA_DAL_CLI_COMMAND --full-help | perl -p -e 's/^/    /' >> $LONG_HELP_OUTPUT_FILE

echo "brief help"
append_header $BRIEF_HELP_OUTPUT_FILE
$JAVA_DAL_CLI_COMMAND --help | perl -p -e 's/^/    /' >> $BRIEF_HELP_OUTPUT_FILE

echo "logical-dataset help"
append_header $LOGICAL_DATASET_HELP_OUTPUT_FILE
$JAVA_DAL_CLI_COMMAND logical-dataset --help | perl -p -e 's/^/    /' >> $LOGICAL_DATASET_HELP_OUTPUT_FILE

echo "logical-dataset list help"
append_header $LOGICAL_DATASET_LIST_HELP_OUTPUT_FILE
$JAVA_DAL_CLI_COMMAND logical-dataset list --help | perl -p -e 's/^/    /' >> $LOGICAL_DATASET_LIST_HELP_OUTPUT_FILE

echo "segment help"
append_header $SEGMENT_HELP_OUTPUT_FILE
$JAVA_DAL_CLI_COMMAND segment --help | perl -p -e 's/^/    /' >> $SEGMENT_HELP_OUTPUT_FILE

echo "segment list help"
append_header $SEGMENT_LIST_HELP_OUTPUT_FILE
$JAVA_DAL_CLI_COMMAND segment list --help | perl -p -e 's/^/    /' >> $SEGMENT_LIST_HELP_OUTPUT_FILE

