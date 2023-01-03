#!/bin/bash

set -e

role="dal"
env="prod"
package_name="dal-repair-service"

# Not running for tst physical locations
IFS=','
for location in dw2,smf1 dwrev,smf1 proc,atla proc2,atla procpi,atla procrev,atla cold,atla ; do
  set ${location}
  dataset_cluster=$1
  dataset_dc=$2
  aurora_cluster=${dataset_dc}
  job_path="${aurora_cluster}/${role}/${env}/${package_name}-${dataset_cluster}-${dataset_dc}"

  echo -e "\n## Stopping repair service: ${dataset_cluster}, ${dataset_dc} ##"

  aurora job killall ${job_path}
done
