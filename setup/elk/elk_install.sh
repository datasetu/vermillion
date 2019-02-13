#!/bin/ash

tmux new-session -d -s es 'su ideam -c "/home/ideam/elasticsearch-6.2.4/bin/elasticsearch"'

tmux new-session -d -s kibana '/home/ideam/kibana-6.2.4-linux-x86_64/bin/kibana'
