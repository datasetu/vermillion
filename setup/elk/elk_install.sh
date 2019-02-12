#!/bin/ash

cd iudx-dbconnector

mvn clean compile assembly:single

tmux new-session -d -s db 'java -jar iudx-dbconnector-0.0.1-SNAPSHOT-jar-with-dependencies.jar'

tmux new-session -d -s es 'su ideam -c "/home/ideam/elasticsearch-6.2.4/bin/elasticsearch"'

tmux new-session -d -s kibana '/home/ideam/kibana-6.2.4-linux-x86_64/bin/kibana'
