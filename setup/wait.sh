#!/bin/bash

#Wait for the auth server to come up
until $(curl -k -XPOST --output /dev/null --silent https://localhost); do
    printf '.'
    sleep 1
done

echo "ready"
