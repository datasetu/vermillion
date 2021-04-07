#!/bin/bash

set -ex

cd ../tests && sudo behave
PID=$(docker exec vertx ps -aux | grep java | awk '{print $2}')
echo "killing $PID"
docker exec vertx kill -15 $PID
sleep 15
cd ../api-server
mvn jacoco:report
sleep 10
echo "Done"



