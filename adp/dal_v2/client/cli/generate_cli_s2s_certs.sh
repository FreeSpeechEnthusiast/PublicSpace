#!/bin/bash

set -x
set -o errexit

# Create the developer cert
# and update TSS to copy the created cert files to mesos hadoopnest hosts
developer-cert-util --job dalv2-cli -d audubon:hadoopnest -d audubon:mesos.prod.slave --client-only

# This command distributes tss materials named:

#   s2s/atla/devel/dalv2-cli/$USER/client.key
#   s2s/atla/devel/dalv2-cli/$USER/client.crt
#   s2s/atla/devel/dalv2-cli/$USER/client.chain

#   s2s/smf1/devel/dalv2-cli/$USER/client.key
#   s2s/smf1/devel/dalv2-cli/$USER/client.crt
#   s2s/smf1/devel/dalv2-cli/$USER/client.chain

#   s2s/pdxa/devel/dalv2-cli/$USER/client.key
#   s2s/pdxa/devel/dalv2-cli/$USER/client.crt
#   s2s/pdxa/devel/dalv2-cli/$USER/client.chain
