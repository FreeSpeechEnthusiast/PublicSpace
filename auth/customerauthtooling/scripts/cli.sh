#!/bin/bash
args="$*"

# to connect to zookepper from local machine
./birdherd/src/scripts/birdherd-tunnels.sh
# to connect to staging services from local machine
source ./twitter-server-internal/scripts/socks.sh

S2S_CERTS_DIR="$HOME/.s2s/local/devel/customerauthtooling-cli/$USER"

if [ ! -d "$S2S_CERTS_DIR" ]; then
  developer-cert-util -j "customerauthtooling-cli" --local
fi

PANTS_CONCURRENT=True ./pants run \
  --jvm-run-jvm-options="$SOCKS_JVM_ARGS
       -Dlog_level=WARN
       -Dlog.lens.index=customerauthtoolingcli
       -Dlog.access.output=customerauthtoolingcli.access.log
       -Dlog.service.output=customerauthtoolingcli.log
       -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
       -Xmx5g
       -XX:MaxMetaspaceSize=2g" \
  --run-jvm-cwd=. \
  --jvm-run-jvm-program-args="$args" \
 auth/customerauthtooling/server/src/main/scala/com/twitter/auth/customerauthtooling/cli:customerauthcli
