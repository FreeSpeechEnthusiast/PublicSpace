#!/bin/bash

# This deploy script builds / deploys dal service.
# For usage, see comments in:
#    adp/common/workflow_deploy_helpers.sh

# Configures bash to exit if any of the below commands fail:
set -o errexit

source adp/common/workflow_deploy_helpers.sh

# The first arg controls which environment we deploy to (prod / staging / local)
ENV=$1

PROD_ROLE=dal
STAGING_ROLE=dal-staging

BUILD_TARGET=adp/dal_v2/server:dal

PACKAGE_NAME=dal
PACKAGE_FILE=dist/dal.zip
READONLY_PACKAGE_NAME=dal_read_only
READONLY_PACKAGE_FILE=dist/dal_read_only.zip

AURORA_FILE=adp/dal_v2/server/config/deploy.aurora
DEVEL_WORKFLOW_FILE=adp/dal_v2/server/config/deploy_devel.workflow
QA_WORKFLOW_FILE=adp/dal_v2/server/config/deploy_qa.workflow
STAGING_WORKFLOW_FILE=adp/dal_v2/server/config/deploy_staging.workflow
PROD_WORKFLOW_FILE=adp/dal_v2/server/config/deploy.workflow
READONLY_WORKFLOW_FILE=adp/dal_v2/server/config/deploy_readonly.workflow
READONLY_STAGING_WORKFLOW_FILE=adp/dal_v2/server/config/deploy_readonly_staging.workflow

UPDATE_TYPE=${UPDATE_TYPE:=workflow-with-build}

run_deploy_steps
