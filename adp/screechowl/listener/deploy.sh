#!/bin/bash

# This deploy script builds / deploys screech-owl-listener service.
# For usage, see comments in:
#    adp/common/workflow_deploy_helpers.sh

# Configures bash to exit if any of the below commands fail:
set -o errexit

source adp/common/workflow_deploy_helpers.sh

# The first arg controls which environment we deploy to (prod / staging / local)
ENV=$1

PROD_ROLE=screechowl
STAGING_ROLE=screechowl-staging

BUILD_TARGET=adp:screech-owl-listener

PACKAGE_NAME=screech-owl-listener
PACKAGE_FILE=dist/screech-owl-listener.zip

AURORA_FILE=adp/screechowl/listener/config/deploy.aurora
STAGING_WORKFLOW_FILE=adp/screechowl/listener/config/deploy-staging.workflow
PROD_WORKFLOW_FILE=adp/screechowl/listener/config/deploy.workflow

UPDATE_TYPE=${UPDATE_TYPE:=workflow-with-build}

run_deploy_steps
