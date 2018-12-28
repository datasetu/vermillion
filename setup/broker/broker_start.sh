#!/bin/ash

rm -r /tmp/tmux-* > /dev/null 2>&1
kodev build > /dev/null 2>/dev/null
tmux new-session -d -s authenticator 'cd /authenticator && kodev run'
rabbitmq-server -detached > /dev/null 2>&1
