#!/usr/bin/env bash

# to connect to zookepper from local machine
./birdherd/src/scripts/birdherd-tunnels.sh
# to connect to staging services from local machine
source ./twitter-server-internal/scripts/socks.sh

S2S_CERTS_DIR="$HOME/.s2s/local/devel/policykeeper/$USER"

if [ ! -d "$S2S_CERTS_DIR" ]; then
  developer-cert-util -j "policykeeper" --local
fi

REPO_ROOT_DIR="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT_DIR" || exit

while [ "$1" ]; do
  case "$1" in
  -debug)
    # if the debug flag is present setup jvm debugging on port 5007
    DEBUG_OPT="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5007"
    echo "Using debug option: $DEBUG_OPT"
    ;;
  esac
  shift
done

DTAB_ADD=${DTAB_ADD:=''}

DTAB_NO_WHITESPACE="$(echo -e "${DTAB_ADD}" | tr -d '[:space:]')"

echo "Starting up local policykeeper server..."
PANTS_CONCURRENT=True ./pants run \
  --jvm-run-jvm-options="$SOCKS_JVM_ARGS
        -Dcom.twitter.wilyns.client.perPathStats=false -Dlog.service.output=/dev/stdout -Dcom.twitter.finatra.authentication.filters.PasetoPassportExtractorLocalMode=true $DEBUG_OPT" \
  --run-jvm-cwd=. \
  --jvm-run-jvm-program-args="
       -admin.port=:31383
       -thrift.port=:31909
       -thrift.clientId=policykeeper
       -decider.base=$REPO_ROOT_DIR/auth/policykeeper/server/src/main/resources/config/decider.yml
       -service.identifier=$USER:policykeeper:devel:local
       -opportunistic.tls.level=required
       -policy_storage_config_bus_path=$REPO_ROOT_DIR/auth/policykeeper/server/src/main/resources/config
       -dtab.add=${DTAB_NO_WHITESPACE}
       " \
  auth/policykeeper:bin
