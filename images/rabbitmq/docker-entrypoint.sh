#!/bin/ash

if [ nc -z localhost 80 ] ;
then
fuser -k 80/tcp
fi
cd /authenticator
kodev build
tmux new-session -d -s authenticator 'cd /authenticator && kodev run'
rabbitmq-server

