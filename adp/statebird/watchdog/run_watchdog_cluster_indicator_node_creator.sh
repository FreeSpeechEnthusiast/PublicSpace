#!/bin/bash

#
# Run a program that creates the 'cluster name' node in ATLA
# This program should be needed *ONLY* when a new cluster comes online
# or when their is a problem/change with the current cluster node.
#

CLUSTER=${CLUSTER:-smf1}

FORWARDED_PORT=2181
PROXY_HOST=127.0.0.1
# The below is the cluster-neutral, read-write host
ZOOKEEPER_HOST=zookeeper.local.twitter.com


if [ "$CLUSTER" == "smf1" ]; then
    PROXY_PORT=50001
    SSH_HOST=hadoopnest2.smf1.twitter.com
elif [ "$CLUSTER" == "atla" ]; then
    PROXY_PORT=50002
    SSH_HOST=hadoopnest3.atla.twitter.com
else
    echo "unknown cluster $CLUSTER"
    exit 1
fi


# Test if the forwarded port is open already; open it if it is not
lsof -i  :$FORWARDED_PORT || \
    ssh -N -f -L *:$FORWARDED_PORT:$ZOOKEEPER_HOST:2181 -D *:$PROXY_PORT $SSH_HOST

# Finagle ZK port-forward settings cribbed from:
#   http://docstager-devel-rcerveranavarro.service.smf1.twitter.biz/finagle-cookbook/naming/accesssvcfromlaptop.html
#
# Actually run the GOAL
./pants run  \
    src/scala/com/twitter/statebird/watchdog/client/example:create-$CLUSTER-main \
    --run-jvm-jvm-options='
      -Dcom.twitter.server.resolverZkHosts=$PROXY_HOST:$FORWARDED_PORT
      -DsocksProxyHost=$PROXY_HOST
      -DsocksProxyPort=$PROXY_PORT
    ' \
    --run-jvm-args=-zk.hosts=$PROXY_HOST:$FORWARDED_PORT
