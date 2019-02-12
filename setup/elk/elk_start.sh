#!/bin/ash

rm -r /tmp/tmux-*

if ! nc -z localhost 9200
then
tmux new-session -d -s elasticsearch 'su ideam -c "/home/ideam/elasticsearch-6.2.4/bin/elasticsearch"'
fi

if ! nc -z localhost 5601
then
tmux new-session -d -s kibana '/home/ideam/kibana-6.2.4-linux-x86_64/bin/kibana'
fi
