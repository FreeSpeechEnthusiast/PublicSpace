#!/bin/bash
#
# This file contains three helper functions:
#    start_ssh_tunnel, ssh_tunnel_running, stop_ssh_tunnel
#
# These helper functions aid scripts that require an SSH tunnel, especially
# those that run 'pants' commands using that tunnel.
#
# NOTE: To DISABLE these SSH tunnels from being created (say, if you are
#       connecting locally / without a network/VPN connection), set
#       the environment variable SSH_TUNNEL_DISABLED to "true" like so:
#       $> SSH_TUNNEL_DISABLED=true ./path/to/tunnel_script.sh
#


#
# Starts an SSH tunnel, with local/dynamic port forwarding set up
# to allow Finagle services / service resolution to work.
#
start_ssh_tunnel() {
    if [ "$SSH_TUNNEL_DISABLED" == "true" ]; then
	echo "Using SSH tunnel is disabled"
	return 0;
    fi

    # Default values for data center, port, tunnel host:
    SSH_TUNNEL_PROXY_DATACENTER=${SSH_TUNNEL_PROXY_DATACENTER:-atla}
    SSH_TUNNEL_HOST=${NEST_HOST:-hadoopnest3.$SSH_TUNNEL_PROXY_DATACENTER.twitter.com}
    SSH_TUNNEL_PROXY_PORT=${SSH_TUNNEL_PROXY_PORT:-29876}

    # SSH options explained:
    #   -S socket file location
    SSH_TUNNEL_CMD="ssh -S ~/.ssh/$SSH_TUNNEL_HOST"

    # SSH options explained:
    #   -M  # Defines master mode for the client
    #   -N  # Don't execute any remote command
    #   -f  # Put SSH into the background
    #   -L  # Set a local port forward
    #   -D  # Set a dynamic port forward
    export SSH_TUNNEL_PANTS_ARGS="--jvm-options='-DsocksProxyHost=localhost' \
        --jvm-options='-DsocksProxyPort=$SSH_TUNNEL_PROXY_PORT' \
        --jvm-options='-Dcom.twitter.server.resolverZkHosts=localhost:2181'"

    # Bazel seems to use slightly different options for "repl" than it does for "run"
    export SSH_TUNNEL_BAZEL_RUN_ARGS="--jvm_flags=-DsocksProxyHost=localhost --jvm_flags=-DsocksProxyPort=$SSH_TUNNEL_PROXY_PORT --jvm_flags=-Dcom.twitter.server.resolverZkHosts=localhost:2181"

    echo "Creating SSH tunnel to $SSH_TUNNEL_HOST; to use a different host set NEST_HOST=your_host"
    $SSH_TUNNEL_CMD -MNf \
        -L "*:2181:sdzookeeper-read.${SSH_TUNNEL_PROXY_DATACENTER}.twitter.com:2181" \
        -D "*:$SSH_TUNNEL_PROXY_PORT" \
        "$SSH_TUNNEL_HOST" || exit -1
}


#
# Test whether the ssh tunnel is running
#
# When the tunnel is running, prints something similar to:
#
#  Master running (pid=3517)
#
ssh_tunnel_running() {
    $SSH_TUNNEL_CMD -O check "$SSH_TUNNEL_HOST"
}


#
# Stops the running SSH tunnel
#
stop_ssh_tunnel() {
    if [ "$SSH_TUNNEL_DISABLED" == "true" ]; then
	echo "Using SSH tunnel is disabled"
	return 0;
    fi
    $SSH_TUNNEL_CMD -O exit "$SSH_TUNNEL_HOST" > /dev/null 2>&1
}
