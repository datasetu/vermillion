#!/bin/ash

rabbitmqctl stop
rabbitmqctl reset
tmux kill-server
fuser -k 80/tcp

cd authenticator 
kodev build > /dev/null 2>/dev/null
tmux new-session -d -s authenticator 'cd /authenticator && kodev run'
rabbitmq-server -detached > /dev/null 2>&1

