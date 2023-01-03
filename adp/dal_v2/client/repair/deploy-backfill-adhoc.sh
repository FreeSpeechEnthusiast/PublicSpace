#!/bin/bash

set -e

# This file contains info about datasets to backfill. The file-path should be included in
# the bundle uploaded to packer (specified in the build target, BUILD:backfill-bundle).
config_file_path="config/backfill.txt"
packer_package="dal-backfill-adhoc"
package_jar="dist/dal-backfill-dist.zip"

function printUsageAndExit {
    echo "
    # This is useful for performing backfill on datasets missing in DAL. For existing datasets use: dal-repair-tool-adhoc.sh
    # Uses segment-definition to perform backfill, also creating logical and physical datasets if they don't exist
    # Note: This does not perform any deletes.

    Usage: $(basename $0) [optional: -b|--bundle] [optional: -h|--help]
        -b|--bundle => specify this option to bundle your local changes and uploading them to packer
    "
    exit 1
}

function bundleAndUploadToPacker {
  echo -e "\n## Packaging repair tool to upload to Packer ##"
  ./bazel bundle adp/dal_v2/client/repair:backfill-bundle --bundle-jvm-archive=zip

  echo -e "\n## Uploading repair tool binary to smf1 ##"
  packer add_version --cluster=smf1 dal ${packer_package} ${package_jar}

  echo -e "\n## Uploading repair tool binary to atla ##"
  packer add_version --cluster=atla dal ${packer_package} ${package_jar}
}

# Parse input arguments
while [[ $# > 0 ]]; do
  key="$1"
  case ${key} in
    -b|--bundle)
    bundleAndUploadToPacker
    ;;
    -h|--help)
    printUsageAndExit
    ;;
    *)
    echo "Skipping Unknown Option: $key"
    ;;
  esac
  shift
done

# Start repair on Aurora
IFS=','
for location in dw2,smf1 dwrev,smf1 proc,atla proc2,atla procpi,atla procrev,atla cold,atla ; do
  set ${location}
  cluster=$1
  dc=$2
  aurora_cluster=${dc} # Hadoop dc (smf1/atla) is called cluster in Aurora
  job_path="${aurora_cluster}/dal/prod/${packer_package}-${cluster}-${dc}"

  echo -e "\n## Starting backfill for ${cluster}-${dc} ##"

  aurora_config_path="adp/dal_v2/client/repair/config/deploy-backfill-adhoc.aurora"
  CLUSTER=${cluster} DC=${dc} aurora job create ${job_path} ${aurora_config_path}
done
