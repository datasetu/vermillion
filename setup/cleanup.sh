#!/bin/bash

set -ex

rm -rf provider/*

# TODO: Read version var from conf file for all usages
rm ../api-server/org.jacoco.agent-0.8.6-runtime.jar

rm -rf ../api-server/webroot/consumer/*
