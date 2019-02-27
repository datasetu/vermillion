#!/bin/bash

cd iudx-api-server 

if [ -e iudx-api-server-0.0.1-SNAPSHOT-fat.jar ]
then
rm iudx-api-server-0.0.1-SNAPSHOT-fat.jar
fi

cp target/iudx-api-server-0.0.1-SNAPSHOT-fat.jar .

tmux new-session -d -s vertx 'java -jar iudx-api-server-0.0.1-SNAPSHOT-fat.jar -d64 -Xms512m -Xmx4g'
