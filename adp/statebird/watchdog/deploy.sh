#!/bin/bash

# This deploy script builds / deploys statebird-watchdog service.
# For usage, see comments in:
#    adp/common/workflow_deploy_helpers.sh

# Configures bash to exit if any of the below commands fail:
set -o errexit

source adp/common/workflow_deploy_helpers.sh

# The first arg controls which environment we deploy to (prod / staging / local)
ENV=$1

PROD_ROLE=statebird
STAGING_ROLE=statebird-staging

BUILD_TARGET=adp/statebird/watchdog/:bundle

PACKAGE_NAME=statebird-watchdog
PACKAGE_FILE=dist/statebird-watchdog-server-package-dist.zip

AURORA_FILE=adp/statebird/watchdog/config/deploy.aurora
STAGING_WORKFLOW_FILE=adp/statebird/watchdog/config/deploy-staging.workflow
PROD_WORKFLOW_FILE=adp/statebird/watchdog/config/deploy.workflow

UPDATE_TYPE=${UPDATE_TYPE:=workflow-with-build}

run_deploy_steps
