#!/usr/bin/env bash

# to connect to zookepper from local machine
./birdherd/src/scripts/birdherd-tunnels.sh
# to connect to staging services from local machine
source ./twitter-server-internal/scripts/socks.sh

S2S_CERTS_DIR="$HOME/.s2s/local/devel/customerauthtooling/$USER"

if [ ! -d "$S2S_CERTS_DIR" ]; then
  developer-cert-util -j "customerauthtooling" --local
fi

REPO_ROOT_DIR="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT_DIR" || exit

while [ "$1" ]; do
  case "$1" in
  -debug)
    # if the debug flag is present setup jvm debugging on port 5009
    DEBUG_OPT="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5009"
    echo "Using debug option: $DEBUG_OPT"
    ;;
  esac
  shift
done

echo "Starting up local customerauthtooling server..."
PANTS_CONCURRENT=True ./pants run \
  --jvm-run-jvm-options="$SOCKS_JVM_ARGS
        -Dcom.twitter.wilyns.client.perPathStats=false -Dlog.service.output=/dev/stdout $DEBUG_OPT" \
  --run-jvm-cwd=. \
  --jvm-run-jvm-program-args="
       -admin.port=:31382
       -thrift.name=customerauthtooling
       -thrift.port=:31908
       -thrift.clientId=customerauthtooling
       -decider.base=$REPO_ROOT_DIR/auth/customerauthtooling/server/src/main/resources/config/decider.yml
       -service.identifier=$USER:customerauthtooling:devel:local
       -opportunistic.tls.level=required
       -scope.to.dps.file.path=$REPO_ROOT_DIR/auth/customerauthtooling/server/src/main/resources/scope_to_dps.csv
       -dps.list.file.path=$REPO_ROOT_DIR/auth/customerauthtooling/server/src/main/resources/dps.csv
       -dp.recommender.file.path=$REPO_ROOT_DIR/auth/customerauthtooling/server/src/main/resources/dp_recommender_output.csv
       -kite.client.env=staging
       -dtab.add=\"
        /s/passbird/passbird=>/srv#/staging/local/passbird/passbird;
        /s/limiter/limiter=>/srv#/staging/local/limiter/limiter;
        /s/snowflake/snowflake=>/srv#/staging/local/snowflake/snowflake;
        /s/gizmoduck/gizmoduck=>/srv#/staging/local/gizmoduck/gizmoduck;
        \"
       " \
  auth/customerauthtooling:bin
