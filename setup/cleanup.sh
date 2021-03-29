#!/bin/bash

cd provider
rm -r *
cd ../../api-server
rm codacy-coverage-reporter-assembly.jar
rm org.jacoco.agent-0.8.6-runtime.jar
cd webroot/consumer
rm -r *
cd ../../../tests
rm test-resource.public
