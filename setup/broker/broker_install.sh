#!/bin/ash
cd authenticator 
kodev build > /dev/null 2>/dev/null
tmux new-session -d -s authenticator 'cd /authenticator && kodev run'
rabbitmq-server -detached > /dev/null 2>&1

while ! nc -z localhost 5672
    do
	sleep 0.1
    done
