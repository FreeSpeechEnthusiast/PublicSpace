#!/bin/bash

# Find all files 'owned' by adp-team, by looking directly at the PROJECT
# file.
# 
# This incorrectly finds some files where adp-team is only a watcher, and 
# some files where a PROJECT in a sub-directory assigns ownership to a non-adp
# team.
#
# Overall, this seems workable, and I don't know a smarter way to inspect the 
# PROJECT files, that doesn't generate those false-positives.
#
# We manually exclude some of the extra files below.
#
my_array=( $( find . -name PROJECT -print0 | xargs -0 grep -l adp-team | grep -v .idea/ ) )

# The below keywords are checked for.  This is only a subset of those in the 
# inclusive language guidelines; more can be added later.
keywords=(blacklist whitelist sanity)


# Collect the overall list of files with issues into a temporary file:
TEMP_FILE=check_inclusive_language-files_with_inclusive_issues_unsorted.tmp.log
# Reset the temporary file, if it exists.
echo > "$TEMP_FILE"

for keyword in "${keywords[@]}"
do
    echo "Checking files for $keyword ..."
    for element in "${my_array[@]}"
    do
        ag -il "$keyword"  `dirname "$element" ` >> "$TEMP_FILE"
    done
done

echo "Done"

# We manually exclude some files below.  Comments below explain why:
#
# The first file excluded is this script itself:
#
#    adp/common/check_inclusive_language.sh
#
#
# The below files relate to the 'persisted schema checker' which is owned by CDL.
# Perhaps we could move them to a directory owned by CDL?
#
#    tests/scala/com/twitter/dal/client/gen/dataset/
#    src/scala/com/twitter/dal/client/gen/dataset/
#    src/resources/com/twitter/dal/client/gen/dataset/blacklist.yml
#    tests/resources/com/twitter/dal/client/gen/dataset/blacklist.yml
#    tests/thrift/com/twitter/dal/client/dataset_gen_test.thrift
#
# 
# This file is from the open-source Hive, and contains a property named 
# `hive.metastore.partition.name.whitelist.pattern`
#
#    dal/dal-hive-meta-store/src/main/thrift/hive_service/hive_metastore.thrift
#
#
# I'm excluding all tasking-related items:
#
#    tasking/
#
# 
# I'm excluding the CDL-Team owned auto-tuning implementation:
#
#    src/scala/com/twitter/scalding_internal/auto_tuning/
#
#
# I'm excluding the CDL-Team owned Dr. Scalding implementation:
#
#   src/scala/com/twitter/scalding_internal/drscalding/
#
#
# I'm also excluding this directory, which contains two large JSON files of test data:
#
#    grep -v tests/resources/com/twitter/twadoop/crane/from/salesforce/
#


cat "$TEMP_FILE" | sort | uniq | \
    grep -v adp/common/check_inclusive_language.sh | \
    grep -v tests/scala/com/twitter/dal/client/gen/dataset/ | \
    grep -v src/scala/com/twitter/dal/client/gen/dataset/ | \
    grep -v src/resources/com/twitter/dal/client/gen/dataset/blacklist.yml | \
    grep -v tests/resources/com/twitter/dal/client/gen/dataset/blacklist.yml | \
    grep -v tests/thrift/com/twitter/dal/client/dataset_gen_test.thrift | \
    grep -v dal/dal-hive-meta-store/src/main/thrift/hive_service/hive_metastore.thrift | \
    grep -v ^tasking/ | \
    grep -v src/scala/com/twitter/scalding_internal/auto_tuning/ | \
    grep -v src/scala/com/twitter/scalding_internal/drscalding/ | \
    grep -v tests/resources/com/twitter/twadoop/crane/from/salesforce/

