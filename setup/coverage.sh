#!/bin/bash


cd ../tests && sudo behave
echo statement3
PID=`docker exec vertx ps -aux | grep java | awk '{print $2}'`
echo "killing $PID"
docker exec vertx kill -15 $PID
sleep 15
echo statement4
cd ../api-server
echo statement5
mvn jacoco:report
sleep 10
echo statement6

export CODACY_PROJECT_TOKEN=$secrets.CODACY_KEY
curl -LS -o codacy-coverage-reporter-assembly.jar "$(curl -LSs https://api.github.com/repos/codacy/codacy-coverage-reporter/releases/latest | jq -r '.assets | map({name, browser_download_url} | select(.name | endswith(".jar"))) | .[0].browser_download_url')"
sleep 10
java -jar codacy-coverage-reporter-assembly.jar report -r target/site/jacoco/jacoco.xml
#bash <(curl -Ls https://coverage.codacy.com/get.sh) report -r target/site/jacoco/jacoco.xml



