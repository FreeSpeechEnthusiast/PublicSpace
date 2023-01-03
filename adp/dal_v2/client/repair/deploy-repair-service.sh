#!/bin/bash
set -ex

DEPLOY_BRANCH=${DEPLOY_BRANCH:-master}

echo "Running aurora workflow build using branch: $DEPLOY_BRANCH"
aurora workflow build --build-branch $DEPLOY_BRANCH --open-browser adp/dal_v2/client/repair/config/deploy-repair-service.workflow
echo
echo "You may watch the in-progress workflow at https://workflows.twitter.biz/workflow/dal/dal-repair-service/"
echo
