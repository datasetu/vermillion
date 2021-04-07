#!/bin/bash

set -ex

cd provider && rm -rf ./*
cd ../../api-server
rm org.jacoco.agent-0.8.6-runtime.jar
cd webroot/consumer && rm -rf ./*
cd ../../../tests
rm test-resource.public
