#!/bin/bash

#Wait for the auth server to come up
until $(curl -k -XPOST --output /dev/null --silent https://localhost:8443/certificate-info --cert ../datasetu-ca/provider/provider.pem --key ../datasetu-ca/provider/provider.key.pem); do
    printf '.'
    sleep 1
done

echo "ready"
