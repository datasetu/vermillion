#!/bin/ash
rm -r /tmp/tmux-* > /dev/null 2>&1

mvn compile assembly:single

tmux new-session -d -s vertx 'java -jar iudx-api-server-0.0.1-SNAPSHOT-jar-with-dependencies.jar'
