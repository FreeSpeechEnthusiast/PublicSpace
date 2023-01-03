#!/bin/bash

# This helper script provides re-usable code to build / deploy
# ADP services
#
# In general the script will either:
# - Launch a workflow that builds and deploys a service remotely
# or
# - build a service bundle locally, using a `bazel bundle` command
# - upload the new bundle to packer
# - stage an aurora deploy or aurora workflow to deploy the service


# Usage Examples
#
# Deploy to staging:
#
#    ./adp/foo/service/deploy.sh staging
#    <<<< Then open and play the staged workflow >>>>
#
# Deploy to prod:
#
#    ./adp/foo/service/deploy.sh prod
#    <<<< Then open and play the staged workflow >>>>
#
# Deploy to staging as an aurora deploy. E2E tests use an 'aurora' deploy,
# as they need to need to wait for a deploy to finish before proceeding.
#
#    UPDATE_TYPE=aurora CLUSTER=atla ./adp/foo/service/deploy.sh staging
#    UPDATE_TYPE=aurora CLUSTER=smf1 ./adp/foo/service/deploy.sh staging

packer_update() {
    BUILD_TARGET=$1
    shift

    PACKER_PACKAGE_NAME=$1
    shift

    PACKER_ZIP=$1
    shift

    echo "Compiling $BUILD_TARGET into package $PACKER_PACKAGE_NAME to file $PACKER_ZIP"
    # Store bash options; Enable xtrace bash option
    flags=$(set +o); set -x

    ./bazel bundle "$BUILD_TARGET" --bundle-jvm-archive=zip

    # Restore bash options
    set +x; eval "$flags"

    for package_role in "$@"; do
        echo "Uploading package $WORKFLOW_PACKAGE_NAME to packer at role=$package_role";

        # Store bash options; Enable xtrace bash option
        flags=$(set +o); set -x

        package upload -c smf1,atla,pdxa -r "$package_role" "$PACKER_PACKAGE_NAME" "$PACKER_ZIP"

        # Restore bash options
        set +x; eval "$flags"
    done
}

workflow_update() {
    WORKFLOW_FILE=$1
    shift

    # Fail if CLUSTER is set, this is misleading for a workflow deploy
    if [ "$CLUSTER" != "" ]; then
        echo "Don't specify CLUSTER for a workflow-based deploy. The Aurora workflow will deploy to all DCs."
        exit 1
    fi

    # Stage the current version of the workflow:
    echo "Running aurora workflow stage"

    # Store bash options; Enable xtrace bash option
    flags=$(set +o); set -x

    aurora workflow stage --open-browser "$WORKFLOW_FILE"

    # Restore bash options
    set +x; eval "$flags"

    echo
    echo "NOW open the staged workflow and release the items there."
    echo
}


aurora_update() {
    AURORA_FILE=$1
    shift

    # The aurora command waits by default, but you can make it not wait by
    # setting AURORA_WAIT to something other than 'true'
    # e.g.
    # AURORA_WAIT=no CLUSTER=smf1 ./adp/statebird/v2/deploy.sh staging aurora
    AURORA_WAIT=${AURORA_WAIT:=true}

    if [ $AURORA_WAIT == "true" ]; then
        WAIT_ARG="--wait"
    else
        WAIT_ARG=""
    fi

    # Store bash options; Enable xtrace bash option
    flags=$(set +o); set -x

    # NOTE: Typically, the Packer 'package name' and the aurora 'job name' match
    #
    # The AURORA_NAME variable, if set, allows a deploy using an aurora job name that doesn't match the packer package name.
    # The 'qa' instances of eagleeye-http-server and dal do this.
    AURORA_NAME=${AURORA_NAME:=$PACKER_PACKAGE_NAME}

    aurora update start "$WAIT_ARG" "$CLUSTER/$AURORA_ROLE/$AURORA_ENV/$AURORA_NAME" "$AURORA_FILE" --bind WF_JOB_NAME=$AURORA_NAME

    # Restore bash options
    set +x; eval "$flags"
}

do_update() {
    AURORA_ROLE=$1
    shift;
    AURORA_FILE=$1
    shift;
    WORKFLOW_FILE=$1

    UPDATE_TYPE=${UPDATE_TYPE:=workflow}

    if [ ${UPDATE_TYPE} == "workflow" ]; then
        workflow_update "$WORKFLOW_FILE"
    elif [ ${UPDATE_TYPE} == "aurora" ]; then
        # Don't default to a cluster, make the user pick
        CLUSTER=${CLUSTER:=UNKNOWN}

        if [ $CLUSTER == "UNKNOWN" ]; then
            echo "CLUSTER environment variable is unset, should be smf1, atla"
            exit -1
        fi
        aurora_update "$AURORA_FILE"
    else
        echo "Unknown update type $UPDATE_TYPE, should be 'workflow' or 'aurora'"
        exit -1
    fi
}

run_deploy_steps() {
    # add ssh agent when running job in Jenkins
    if [[ $(uname) = Linux ]]; then
        eval "$(ssh-agent)"; ssh-add
    fi

    if [ "${UPDATE_TYPE}" == "workflow-with-build" ]; then
        CURRENT_BRANCH_NAME="$(git symbolic-ref --short -q HEAD 2>/dev/null)"
        DEPLOY_BRANCH=${DEPLOY_BRANCH:=UNSET}

        if [ $DEPLOY_BRANCH != "UNSET" ]; then
            echo "Starting workflow deploy from non-master branch of $DEPLOY_BRANCH"
        elif [ "$CURRENT_BRANCH_NAME" != "master" ]; then
            cat <<'EOF'
You are on not on master, but have not specified which branch to deploy.

To build remotely from a non-master branch, you need to:
- push the branch you wish to deploy
- set the environment variable DEPLOY_BRANCH when calling this script:
  $ DEPLOY_BRANCH=jboyd/my-cool-feature  ./adp/dal_v2/server/deploy.sh staging
  or
  $ DEPLOY_BRANCH=jboyd/my-cool-feature  ./adp/dal_v2/server/deploy.sh devel

To build locally, from your laptop, you may specify UPDATE_TYPE=aurora:
  $ UPDATE_TYPE=aurora CLUSTER=atla adp/dal_v2/server/deploy.sh staging
  or
  $ UPDATE_TYPE=aurora CLUSTER=atla adp/dal_v2/server/deploy.sh devel
EOF

            exit -1
        else
            DEPLOY_BRANCH="master"
        fi

        #
        # do the workflow update
        #
        if [ "$ENV" == "devel" ]; then
            AURORA_ENV="$ENV"

            if [ "$DEVEL_WORKFLOW_FILE" == "" ]; then
                echo "ERROR: $UPDATE_TYPE is unsupported for $ENV. Exiting..."
                exit -1
            else
                echo "You can browse the running workflows at: https://workflows.twitter.biz/workflow/$STAGING_ROLE/$PACKAGE_NAME"

                echo
                echo

                aurora workflow build --build-branch $DEPLOY_BRANCH --open-browser "$DEVEL_WORKFLOW_FILE"
            fi
        elif [ "$ENV" == "staging" ]; then
            AURORA_ENV="$ENV"

            echo "You can browse the running workflows at: https://workflows.twitter.biz/workflow/$STAGING_ROLE/$PACKAGE_NAME"
            echo
            echo

            aurora workflow build --build-branch $DEPLOY_BRANCH --open-browser "$STAGING_WORKFLOW_FILE"
            # Additionally deploy read_only service if it is dal
            if [ "$PACKAGE_NAME" == "dal" ]; then
              echo
              echo "Now deploying staging dal_read_only"
              AURORA_ENV="staging"
              echo "You can browse the running workflows at: https://workflows.twitter.biz/workflow/$STAGING_ROLE/$READONLY_PACKAGE_NAME"
              echo
              echo
              aurora workflow build --build-branch $DEPLOY_BRANCH --open-browser "$READONLY_STAGING_WORKFLOW_FILE"
            fi

        elif [ "$ENV" == "qa" ]; then
            AURORA_ENV="staging2"

            echo "You can browse the running workflows at: https://workflows.twitter.biz/workflow/$QA_ROLE/$QA_PACKAGE_NAME"
            echo
            echo

            aurora workflow build --build-branch $DEPLOY_BRANCH --open-browser "$QA_WORKFLOW_FILE"
        elif [ "$ENV" == "prod" ]; then
            AURORA_ENV="$ENV"
            echo "You can browse the running workflows at: https://workflows.twitter.biz/workflow/$PROD_ROLE/$PACKAGE_NAME"
            echo
            echo

            aurora workflow build --build-branch $DEPLOY_BRANCH --open-browser "$PROD_WORKFLOW_FILE"
            if [ "$PACKAGE_NAME" == "dal" ]; then
              echo
              echo "Now deploying production dal_read_only"
              echo "You can browse the running workflows at: https://workflows.twitter.biz/workflow/$PROD_ROLE/$READONLY_PACKAGE_NAME"
              echo
              echo
              aurora workflow build --build-branch $DEPLOY_BRANCH --open-browser "$READONLY_WORKFLOW_FILE"
            fi
        elif [ "$ENV" == "read_only_staging" ]; then
           AURORA_ENV="staging"
           echo "You can browse the running workflows at: https://workflows.twitter.biz/workflow/$STAGING_ROLE/$READONLY_PACKAGE_NAME"
           echo
           echo

           aurora workflow build --build-branch $DEPLOY_BRANCH --open-browser "$READONLY_STAGING_WORKFLOW_FILE"
        elif [ "$ENV" == "read_only" ]; then
           AURORA_ENV="prod"
           echo "You can browse the running workflows at: https://workflows.twitter.biz/workflow/$PROD_ROLE/$READONLY_PACKAGE_NAME"
           echo
           echo

           aurora workflow build --build-branch $DEPLOY_BRANCH --open-browser "$READONLY_WORKFLOW_FILE"
        else
            echo "Usage: $0 <devel|staging|prod|qa|read_only|read_only_staging> [ aurora ]"
            exit 1
        fi
    else
        if [ "$ENV" == "devel" ]; then
            AURORA_ENV="$ENV"

            ROLE=$USER
            UPDATE_TYPE="aurora"
            CLUSTER=${CLUSTER:-atla}

            packer_update \
                "$BUILD_TARGET" \
                "$PACKAGE_NAME-$USER" \
                "$PACKAGE_FILE" \
                "$STAGING_ROLE"

            do_update "$STAGING_ROLE" "$AURORA_FILE"
        elif [ "$ENV" == "staging" ]; then
            AURORA_ENV="$ENV"

            packer_update \
                "$BUILD_TARGET" \
                "$PACKAGE_NAME" \
                "$PACKAGE_FILE" \
                "$STAGING_ROLE"

            do_update "$STAGING_ROLE" "$AURORA_FILE" "$STAGING_WORKFLOW_FILE"

            if [ "$PACKAGE_NAME" == "dal" ]; then
               echo "Deploying dal_read_only to staging"
               packer_update \
                  "$BUILD_TARGET" \
                  "$READONLY_PACKAGE_NAME" \
                  "$PACKAGE_FILE" \
                  "$STAGING_ROLE" \
                  "$STAGING_ROLE"
               AURORA_NAME="$READONLY_PACKAGE_NAME"
               do_update "$STAGING_ROLE" "$AURORA_FILE" "$READONLY_STAGING_WORKFLOW_FILE"
            fi
        elif [ "$ENV" == "qa" ]; then
            AURORA_ENV="staging2"

            # NOTE: QA_PACKAGE_NAME is used here instead of PACKAGE_NAME
            # This allows QA and STAGING instances to have different packages even though they use the same service account.
            packer_update \
                "$BUILD_TARGET" \
                "$QA_PACKAGE_NAME" \
                "$PACKAGE_FILE" \
                "$QA_ROLE"

            # Use the 'regular' package name as the aurora job name, instead of the 'qa package name'
            AURORA_NAME="$PACKAGE_NAME"
            do_update "$QA_ROLE" "$AURORA_FILE" "$QA_WORKFLOW_FILE"
        elif [ "$ENV" == "prod" ]; then
            AURORA_ENV="$ENV"

            packer_update \
                "$BUILD_TARGET" \
                "$PACKAGE_NAME" \
                "$PACKAGE_FILE" \
                "$STAGING_ROLE" \
                "$PROD_ROLE"

            do_update "$PROD_ROLE" "$AURORA_FILE" "$PROD_WORKFLOW_FILE"

            if [ "$PACKAGE_NAME" == "dal" ]; then
              AURORA_ENV="prod"
              packer_update \
                "$READONLY_BUILD_TARGET" \
                "$READONLY_PACKAGE_NAME" \
                "$READONLY_PACKAGE_NAME" \
                "$READONLY_PACKAGE_FILE" \
                "$PROD_ROLE"
              AURORA_NAME="$READONLY_PACKAGE_NAME"
              do_update "$PROD_ROLE" "$AURORA_FILE" "$READONLY_WORKFLOW_FILE"
            fi
        elif [ "$ENV" == "read_only_staging" ]; then
            AURORA_ENV="staging"

            packer_update \
                "$BUILD_TARGET" \
                "$READONLY_PACKAGE_NAME" \
                "$PACKAGE_FILE" \
                "$STAGING_ROLE" \
                "$STAGING_ROLE"

            AURORA_NAME="$READONLY_PACKAGE_NAME"
            do_update "$STAGING_ROLE" "$AURORA_FILE" "$READONLY_STAGING_WORKFLOW_FILE"
        elif [ "$ENV" == "read_only" ]; then
            AURORA_ENV="prod"

            packer_update \
                "$READONLY_BUILD_TARGET" \
                "$READONLY_PACKAGE_NAME" \
                "$READONLY_PACKAGE_NAME" \
                "$READONLY_PACKAGE_FILE" \
                "$PROD_ROLE"
            AURORA_NAME="$READONLY_PACKAGE_NAME"
            do_update "$PROD_ROLE" "$AURORA_FILE" "$READONLY_WORKFLOW_FILE"
        else
            echo "Usage: $0 <devel|staging|prod|qa|read_only|read_only_staging> [ aurora ]"
            exit 1
        fi

    fi

    # kill ssh agent when running job in Jenkins
    if [[ $(uname) = Linux ]]; then
        ssh-agent -k
    fi
}
