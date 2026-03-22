#!/bin/bash
# Upgrade an existing LXC container to a new OCI image
# Usage: ./upgrade-test-lxc.sh


VMID=303
NODE="chiki"
IMAGE="docker.io/sd25/tracko-app"


pve-oci deploy \
    --vmid "$VMID" \
    --node "$NODE" \
    --image "$IMAGE"
