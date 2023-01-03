#!/bin/bash

set -e

function printUsageAndExit {
    echo "Usage: $(basename $0) [file-path=file containing datasets info] [dal-env=staging or prod] [role=dataset role]
      [optional: -b|--bundle] [optional: --extra-args=...]"
    exit 1
}

function validate {
  if [ -z $1 ]; then
    echo $2
    printUsageAndExit
  fi
}

if [ $# -lt 2 ]; then
    printUsageAndExit
fi

bundle=""
filePath=""
datasetRole=""
dalEnv=""
extraArgs=""
while [[ $# -gt 0 ]]
do
key="$1"

case ${key} in
    -b|--bundle)
    bundle="-bundle="
    ;;
    --file-path=*)
    filePath="${key#*=}"
    ;;
    --dal-env=*)
    dalEnv="${key#*=}"
    ;;
    --role=*)
    datasetRole="${key#*=}"
    ;;
    --extra-args=*)
    extraArgs="${key#*=}"
    ;;
    *)
    echo "Skipping Unknown Option: $key"
    ;;
esac
shift
done

validate "$filePath" "File path missing"
validate "$dalEnv" "DAL environment missing"
validate "$datasetRole" "Dataset Role missing"

repairToolExtraArgs=""
if [ -n "${extraArgs}" ]; then
  repairToolExtraArgs="-repair-tool-args='${extraArgs}'"
fi

# Start repair on Aurora
./bazel run --cwd src/scala/com/twitter/dal/client/repair/adhoc:bin -- \
  -file-path=${filePath} -role=${datasetRole} -dal-environment=${dalEnv} ${repairToolExtraArgs} ${bundle}
