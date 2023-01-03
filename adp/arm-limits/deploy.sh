#!/bin/sh -ex

ARM_ZOOKEEPER_CLUSTERS="zookeeper.smf1.twitter.com zookeeper.atla.twitter.com zookeeper.pdxa.twitter.com"
USER=twadoop
NEST=hadoopnest2.smf1.twitter.com

# cd to the script's directory
SCRIPTPATH=$(dirname "$0")
cd $SCRIPTPATH

if [ ! -s config/arm-limits.yml ]; then
  echo "Error: can't find config file"
fi

# copy config to nest host
scp config/arm-limits.yml $USER@$NEST:/home/$USER/

# update zookeeper
for cluster in $ARM_ZOOKEEPER_CLUSTERS; do
    # Previous version of this script used `fileutil` command, which isn't on nest machines any more:
    # ssh $USER@$NEST fileutil --zkhost $cluster cp -f /home/$USER/arm-limits.yml zk:///twitter/twadoop/arm/config

    echo "Fetching file from ZK BEFORE UPDATE"
    ssh $USER@$NEST "zk-shell --run-once 'get /twitter/twadoop/arm/config/' $cluster"

    # Remove the existing file, zk-shell won't overwrite it:
    ssh $USER@$NEST "zk-shell --run-once 'rm /twitter/twadoop/arm/config/' $cluster"
    # Copy updated file:
    ssh $USER@$NEST "zk-shell --run-once 'cp file:///home/twadoop/arm-limits.yml  /twitter/twadoop/arm/config/' $cluster"

    echo "Fetching file from ZK AFTER UPDATE"
    ssh $USER@$NEST "zk-shell --run-once 'get /twitter/twadoop/arm/config/' $cluster"

    echo
    echo
    
done
