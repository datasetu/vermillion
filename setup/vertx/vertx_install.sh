#!/bin/bash

cd iudx-api-server 

mvn clean compile assembly:single

cp target/iudx-api-server-0.0.1-SNAPSHOT-jar-with-dependencies.jar . 

tmux new-session -d -s vertx 'java -jar iudx-api-server-0.0.1-SNAPSHOT-jar-with-dependencies.jar'
