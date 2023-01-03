#!/bin/bash

# This deploy script builds / deploys screech-owl-service service.
# For usage, see comments in:
#    adp/common/workflow_deploy_helpers.sh

# Configures bash to exit if any of the below commands fail:
set -o errexit

source adp/common/workflow_deploy_helpers.sh

# The first arg controls which environment we deploy to (prod / staging / local)
ENV=$1

PROD_ROLE=screechowl
STAGING_ROLE=screechowl-staging

BUILD_TARGET=adp/screechowl/service:bundle

PACKAGE_NAME=screech-owl-service
PACKAGE_FILE=dist/screech-owl-service.zip

AURORA_FILE=adp/screechowl/service/config/deploy.aurora
STAGING_WORKFLOW_FILE=adp/screechowl/service/config/deploy-staging.workflow
PROD_WORKFLOW_FILE=adp/screechowl/service/config/deploy.workflow

UPDATE_TYPE=${UPDATE_TYPE:=workflow-with-build}

run_deploy_steps
