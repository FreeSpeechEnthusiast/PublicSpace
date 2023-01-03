#!/bin/bash

# This deploy script builds / deploys statebird-v2 service.
# For usage, see comments in:
#    adp/common/workflow_deploy_helpers.sh

# Configures bash to exit if any of the below commands fail:
set -o errexit

source adp/common/workflow_deploy_helpers.sh

# The first arg controls which environment we deploy to (prod / staging / local)
ENV=$1

PROD_ROLE=statebird
STAGING_ROLE=statebird-staging

BUILD_TARGET=adp/statebird/v2:bundle

PACKAGE_NAME=statebird-v2
PACKAGE_FILE=dist/statebird-v2-server-package-dist.zip

AURORA_FILE=adp/statebird/v2/config/deploy.aurora
STAGING_WORKFLOW_FILE=adp/statebird/v2/config/deploy-staging.workflow
PROD_WORKFLOW_FILE=adp/statebird/v2/config/deploy.workflow

UPDATE_TYPE=${UPDATE_TYPE:=workflow-with-build}

run_deploy_steps
